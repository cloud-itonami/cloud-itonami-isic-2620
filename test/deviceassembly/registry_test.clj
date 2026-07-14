(ns deviceassembly.registry-test
  (:require [clojure.test :refer [deftest is]]
            [deviceassembly.registry :as r]))

;; ----------------------------- device-unit-emc-emission-out-of-range? -----------------------------

(deftest not-out-of-range-when-within-bounds
  (is (not (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual 0.05 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10})))
  (is (not (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual -0.10 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10})))
  (is (not (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual 0.10 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10}))))

(deftest out-of-range-when-below-minimum-or-above-maximum
  (is (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual -0.35 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10}))
  (is (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual 0.35 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10})))

(deftest out-of-range-is-false-on-missing-fields
  (is (not (r/device-unit-emc-emission-out-of-range? {})))
  (is (not (r/device-unit-emc-emission-out-of-range? {:emc-emission-deviation-actual 0.35}))))

;; ----------------------------- register-device-unit-shipment -----------------------------

(deftest shipment-is-a-draft-not-a-real-shipment
  (let [result (r/register-device-unit-shipment "device-unit-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest shipment-assigns-shipment-number
  (let [result (r/register-device-unit-shipment "device-unit-1" "JPN" 7)]
    (is (= (get result "shipment_number") "JPN-SHP-000007"))
    (is (= (get-in result ["record" "device_unit_id"]) "device-unit-1"))
    (is (= (get-in result ["record" "kind"]) "device-unit-shipment-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest shipment-validation-rules
  (is (thrown? Exception (r/register-device-unit-shipment "" "JPN" 0)))
  (is (thrown? Exception (r/register-device-unit-shipment "device-unit-1" "" 0)))
  (is (thrown? Exception (r/register-device-unit-shipment "device-unit-1" "JPN" -1))))

;; ----------------------------- register-declaration-of-conformity -----------------------------

(deftest declaration-is-a-draft-not-real-certification
  (let [result (r/register-declaration-of-conformity "device-unit-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest declaration-assigns-evidence-number
  (let [result (r/register-declaration-of-conformity "device-unit-1" "JPN" 3)]
    (is (= (get result "evidence_number") "JPN-DOC-000003"))
    (is (= (get-in result ["record" "device_unit_id"]) "device-unit-1"))
    (is (= (get-in result ["record" "kind"]) "declaration-of-conformity-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest declaration-validation-rules
  (is (thrown? Exception (r/register-declaration-of-conformity "" "JPN" 0)))
  (is (thrown? Exception (r/register-declaration-of-conformity "device-unit-1" "" 0)))
  (is (thrown? Exception (r/register-declaration-of-conformity "device-unit-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-device-unit-shipment "device-unit-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-device-unit-shipment "device-unit-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-SHP-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-SHP-000001" (get-in hist2 [1 "record_id"])))))
