(ns deviceassembly.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/ship-device-unit`/`:actuation/issue-
  declaration-of-conformity` must NEVER be a member of any phase's
  `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [deviceassembly.phase :as phase]))

(deftest ship-device-unit-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real device-unit shipment"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/ship-device-unit))
          (str "phase " n " must not auto-commit :actuation/ship-device-unit")))))

(deftest issue-declaration-of-conformity-never-auto-at-any-phase
  (testing "structural invariant: no phase auto-commits a real Declaration of Conformity"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/issue-declaration-of-conformity))
          (str "phase " n " must not auto-commit :actuation/issue-declaration-of-conformity")))))

(deftest end-of-line-quality-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :end-of-line-quality/screen))
          (str "phase " n " must not auto-commit :end-of-line-quality/screen")))))

(deftest robotics-simulate-burn-in-cell-never-auto-at-any-phase
  (testing "the robot burn-in-cell verification mission carries no direct capital risk, but is still never auto-eligible, matching every sibling verification op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :robotics/simulate-burn-in-cell))
          (str "phase " n " must not auto-commit :robotics/simulate-burn-in-cell")))))

(deftest robotics-simulate-burn-in-cell-enabled-from-phase-2
  (is (contains? (:writes (get phase/phases 2)) :robotics/simulate-burn-in-cell))
  (is (contains? (:writes (get phase/phases 3)) :robotics/simulate-burn-in-cell))
  (is (not (contains? (:writes (get phase/phases 1)) :robotics/simulate-burn-in-cell))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":device-unit/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:device-unit/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :device-unit/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/ship-device-unit} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/issue-declaration-of-conformity} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :device-unit/intake} :commit)))))
