(ns deviceassembly.export-test
  "Audit-package export contract -- social/regulatory hand-off shape."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [langgraph.graph :as g]
            [deviceassembly.export :as export]
            [deviceassembly.operation :as op]
            [deviceassembly.store :as store]))

(def operator {:actor-id "op-1" :actor-role :compliance-engineer :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn- seed-with-one-shipment []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "v" {:op :compliance-rules/verify :subject "device-unit-1"})
    (approve! actor "v")
    (exec! actor "r" {:op :robotics/simulate-burn-in-cell :subject "device-unit-1"})
    (approve! actor "r")
    (exec! actor "d" {:op :actuation/ship-device-unit :subject "device-unit-1"})
    (approve! actor "d")
    db))

(deftest audit-package-shape
  (let [db (seed-with-one-shipment)
        pkg (export/audit-package db)]
    (is (= "2620" (:isic pkg)))
    (is (= "cloud-itonami-isic-2620" (:business-id pkg)))
    (is (= :edn-maps (:format pkg)))
    (is (pos? (get-in pkg [:counts :ledger])))
    (is (= 1 (get-in pkg [:counts :shipments])))
    (is (some #(= "device-unit-1" (:id %)) (:device-units pkg)))
    (is (true? (:device-unit-shipped?
                (first (filter #(= "device-unit-1" (:id %)) (:device-units pkg))))))))

(deftest csv-bundle-has-headers-and-rows
  (let [db (seed-with-one-shipment)
        bundle (export/package->csv-bundle db)]
    (is (every? bundle ["device-units.csv" "ledger.csv" "shipments.csv" "declarations-of-conformity.csv"]))
    (is (str/starts-with? (get bundle "device-units.csv") "id,device-unit-name,"))
    (is (re-find #"device-unit-1" (get bundle "device-units.csv")))
    (is (re-find #"JPN-SHP-000000" (get bundle "shipments.csv")))
    (is (re-find #":actuation/ship-device-unit" (get bundle "ledger.csv")))))

(deftest empty-store-export-is-usable
  (let [db (store/seed-db)
        pkg (export/audit-package db)
        bundle (export/package->csv-bundle db)]
    (is (= 0 (get-in pkg [:counts :shipments])))
    (is (= 6 (get-in pkg [:counts :device-units])))
    (is (str/includes? (get bundle "ledger.csv") "seq,t,op"))))
