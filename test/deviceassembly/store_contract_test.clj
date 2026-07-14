(ns deviceassembly.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a configuration
  change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the sibling
  actor."
  (:require [clojure.test :refer [deftest is testing]]
            [deviceassembly.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Notebook NB-14" (:device-unit-name (store/device-unit s "device-unit-1"))))
      (is (= "JPN" (:jurisdiction (store/device-unit s "device-unit-1"))))
      (is (= 0.05 (:emc-emission-deviation-actual (store/device-unit s "device-unit-1"))))
      (is (= -0.10 (:emc-emission-deviation-min (store/device-unit s "device-unit-1"))))
      (is (= 0.10 (:emc-emission-deviation-max (store/device-unit s "device-unit-1"))))
      (is (false? (:eol-defect-unresolved? (store/device-unit s "device-unit-1"))))
      (is (= 0.35 (:emc-emission-deviation-actual (store/device-unit s "device-unit-3"))))
      (is (true? (:eol-defect-unresolved? (store/device-unit s "device-unit-4"))))
      (is (false? (:robotics-sim-verified? (store/device-unit s "device-unit-1"))) "no robotics mission has run yet")
      (is (true? (:robotics-sim-verified? (store/device-unit s "device-unit-5"))) "seeded as already-on-file")
      (is (= 0.30 (:thermal-margin-deviation-actual (store/device-unit s "device-unit-5"))))
      (is (false? (:device-unit-shipped? (store/device-unit s "device-unit-1"))))
      (is (false? (:declaration-issued? (store/device-unit s "device-unit-1"))))
      (is (= ["device-unit-1" "device-unit-2" "device-unit-3" "device-unit-4" "device-unit-5"]
             (mapv :id (store/all-device-units s))))
      (is (nil? (store/eol-screen-of s "device-unit-1")))
      (is (nil? (store/requirements-verification-of s "device-unit-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/shipment-history s)))
      (is (= [] (store/declaration-history s)))
      (is (zero? (store/next-shipment-sequence s "JPN")))
      (is (zero? (store/next-declaration-sequence s "JPN")))
      (is (false? (store/device-unit-already-shipped? s "device-unit-1")))
      (is (false? (store/device-unit-already-declared? s "device-unit-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :device-unit/upsert
                                 :value {:id "device-unit-1" :device-unit-name "Sakura Notebook NB-14"}})
        (is (= "Sakura Notebook NB-14" (:device-unit-name (store/device-unit s "device-unit-1"))))
        (is (= 0.05 (:emc-emission-deviation-actual (store/device-unit s "device-unit-1"))) "unrelated field preserved"))
      (testing "robotics-sim result commits via :device-unit/upsert and reads back"
        (store/commit-record! s {:effect :device-unit/upsert
                                 :value {:id "device-unit-1" :robotics-sim-verified? true
                                        :robotics-sim-record {:mission-id "m-1" :passed? true}}})
        (is (true? (:robotics-sim-verified? (store/device-unit s "device-unit-1"))))
        (is (= {:mission-id "m-1" :passed? true} (:robotics-sim-record (store/device-unit s "device-unit-1"))))
        (is (= 0.05 (:emc-emission-deviation-actual (store/device-unit s "device-unit-1"))) "unrelated field still preserved"))
      (testing "verification / EOL-screen payloads commit and read back"
        (store/commit-record! s {:effect :verification/set :path ["device-unit-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/requirements-verification-of s "device-unit-1")))
        (store/commit-record! s {:effect :eol-screen/set :path ["device-unit-1"]
                                 :payload {:device-unit-id "device-unit-1" :verdict :resolved}})
        (is (= {:device-unit-id "device-unit-1" :verdict :resolved} (store/eol-screen-of s "device-unit-1"))))
      (testing "device-unit shipment drafts a record and advances the sequence"
        (store/commit-record! s {:effect :device-unit/mark-shipped :path ["device-unit-1"]})
        (is (= "JPN-SHP-000000" (get (first (store/shipment-history s)) "record_id")))
        (is (= "device-unit-shipment-draft" (get (first (store/shipment-history s)) "kind")))
        (is (true? (:device-unit-shipped? (store/device-unit s "device-unit-1"))))
        (is (= 1 (count (store/shipment-history s))))
        (is (= 1 (store/next-shipment-sequence s "JPN")))
        (is (true? (store/device-unit-already-shipped? s "device-unit-1")))
        (is (false? (store/device-unit-already-shipped? s "device-unit-2"))))
      (testing "Declaration of Conformity drafts a record and advances the sequence"
        (store/commit-record! s {:effect :device-unit/mark-declared :path ["device-unit-1"]})
        (is (= "JPN-DOC-000000" (get (first (store/declaration-history s)) "record_id")))
        (is (= "declaration-of-conformity-draft" (get (first (store/declaration-history s)) "kind")))
        (is (true? (:declaration-issued? (store/device-unit s "device-unit-1"))))
        (is (= 1 (count (store/declaration-history s))))
        (is (= 1 (store/next-declaration-sequence s "JPN")))
        (is (true? (store/device-unit-already-declared? s "device-unit-1")))
        (is (false? (store/device-unit-already-declared? s "device-unit-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/device-unit s "nope")))
    (is (= [] (store/all-device-units s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/shipment-history s)))
    (is (= [] (store/declaration-history s)))
    (is (zero? (store/next-shipment-sequence s "JPN")))
    (is (zero? (store/next-declaration-sequence s "JPN")))
    (store/with-device-units s {"x" {:id "x" :device-unit-name "n" :emc-emission-deviation-actual 0.05
                                   :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                                   :eol-defect-unresolved? false
                                   :device-unit-shipped? false :declaration-issued? false
                                   :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:device-unit-name (store/device-unit s "x"))))))
