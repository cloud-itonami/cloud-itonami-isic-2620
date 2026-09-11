(ns deviceassembly.robotics-test
  "Direct tests of `deviceassembly.robotics`'s REAL, ADR-2607991500
  time-stepped `physics-2d` connector mating/insertion-force
  simulation -- proving `:sim-peak-insertion-force-n` is actually
  DERIVED from the simulated trajectory (changes sensibly with
  `plug-mass-kg`, is deterministic/repeatable, and the peak
  deceleration is mass-invariant against the immovable receptacle, the
  same shape this fleet's other `*-robotics-test` suites use to prove
  a physics check isn't invented or randomized) -- alongside the
  UNCHANGED pre-existing symbolic `thermal-margin-out-of-range?`
  check, proving this ADR is purely additive."
  (:require [clojure.test :refer [deftest is testing]]
            [deviceassembly.robotics :as robotics]))

(defn- approx= [a b eps] (< (Math/abs (double (- a b))) eps))

(deftest connector-mating-test-runs-a-real-trajectory
  (testing "run-connector-mating-test returns a non-trivial, tick-by-tick trajectory -- not a single invented number"
    (let [{:keys [trajectory ticks dt insertion-speed-mps travel-to-seat-m]} (robotics/run-connector-mating-test 1.3)]
      (is (> ticks 1) "more than one simulated tick")
      (is (= ticks (count trajectory)))
      (is (pos? dt))
      (is (= robotics/insertion-speed-mps insertion-speed-mps))
      (is (= robotics/travel-to-seat-m travel-to-seat-m))
      (testing "the plug starts moving at the full insertion speed"
        (is (= insertion-speed-mps (first (:velocity (first trajectory))))))
      (testing "the plug's velocity actually drops to (near) zero once it seats -- a real collision was resolved, not skipped"
        (is (< (Math/abs (double (first (:velocity (last trajectory))))) 1.0e-6))))))

(deftest connector-mating-force-scales-with-plug-mass
  (testing "F = m*a: a heavier plug-mass-kg input yields a proportionally larger peak insertion force, off the SAME simulated deceleration -- proves the reading is derived, not a fixed/invented constant"
    (let [light (robotics/run-connector-mating-test 1.0)
          heavy (robotics/run-connector-mating-test 2.0)]
      (is (< (:sim-peak-insertion-force-n light) (:sim-peak-insertion-force-n heavy)))
      (is (approx= (* 2.0 (:sim-peak-insertion-force-n light)) (:sim-peak-insertion-force-n heavy) 1.0e-6)
          "force doubles (within floating-point tolerance) with mass -- same peak deceleration, per ns docstring's mass-invariance disclosure")
      (testing "peak deceleration itself is mass-invariant (the receptacle is immovable, mass cancels algebraically)"
        (is (approx= (:sim-peak-decel-mps2 light) (:sim-peak-decel-mps2 heavy) 1.0e-9))))))

(deftest connector-mating-force-scales-with-insertion-speed
  (testing "a faster controlled insertion-speed-mps yields a larger peak force off the SAME plug mass -- a second independent axis the reading actually tracks"
    (let [slow (robotics/run-connector-mating-test 1.5 {:insertion-speed-mps 0.05})
          fast (robotics/run-connector-mating-test 1.5 {:insertion-speed-mps 0.30})]
      (is (< (:sim-peak-insertion-force-n slow) (:sim-peak-insertion-force-n fast))))))

(deftest connector-mating-simulation-is-deterministic
  (testing "the same plug-mass-kg always reproduces the same telemetry -- no wall-clock/IO/randomness"
    (let [a (robotics/run-connector-mating-test 1.35)
          b (robotics/run-connector-mating-test 1.35)]
      (is (= (dissoc a :trajectory) (dissoc b :trajectory)))
      (is (= a b)))))

(deftest connector-mating-telemetry-for-reads-the-device-units-own-mass
  (testing "connector-mating-telemetry-for runs the real simulation off :connector-plug-mass-kg, not a hand-typed double"
    (let [light-unit {:connector-plug-mass-kg 1.2}
          heavy-unit {:connector-plug-mass-kg 5.0}
          light-telemetry (robotics/connector-mating-telemetry-for light-unit)
          heavy-telemetry (robotics/connector-mating-telemetry-for heavy-unit)]
      (is (= (:sim-peak-insertion-force-n light-telemetry)
             (:sim-peak-insertion-force-n (robotics/run-connector-mating-test 1.2))))
      (is (< (:sim-peak-insertion-force-n light-telemetry) (:sim-peak-insertion-force-n heavy-telemetry))))))

(deftest connector-mating-force-out-of-tolerance-thresholds-on-the-real-ceiling
  (testing "a device-unit whose real simulated peak insertion force is at/under the ceiling is in-tolerance; over it is out-of-tolerance"
    (is (false? (robotics/connector-mating-force-out-of-tolerance? {:sim-peak-insertion-force-n (- robotics/max-insertion-force-n 1.0)})))
    (is (true? (robotics/connector-mating-force-out-of-tolerance? {:sim-peak-insertion-force-n (+ robotics/max-insertion-force-n 1.0)})))
    (is (false? (robotics/connector-mating-force-out-of-tolerance? {:sim-peak-insertion-force-n nil}))
        "missing telemetry is never silently treated as a violation")))

(deftest thermal-margin-check-is-unchanged-and-independent-of-the-new-physics-check
  (testing "ADR-2607991500 is purely additive: the pre-existing symbolic thermal-margin check still fires on its own, unrelated to connector-mating-force"
    (let [device-unit {:thermal-margin-deviation-actual 0.30 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                        :connector-plug-mass-kg 1.2}]
      (is (true? (robotics/thermal-margin-out-of-range? device-unit)))
      (is (false? (robotics/connector-mating-force-out-of-tolerance?
                   (merge device-unit (robotics/connector-mating-telemetry-for device-unit))))
          "an out-of-tolerance thermal-margin reading does not, by itself, make the connector-mating force out-of-tolerance"))))

(deftest simulate-burn-in-cell-folds-both-checks-into-one-verdict
  (testing "simulate-burn-in-cell fails :passed? when EITHER the pre-existing thermal-margin check OR the new connector-mating-force check fails -- neither silently overrides the other"
    (let [clean {:thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                 :connector-plug-mass-kg 1.2}
          bad-thermal (assoc clean :thermal-margin-deviation-actual 0.30)
          bad-connector (assoc clean :connector-plug-mass-kg 5.0)]
      (is (true? (:passed? (robotics/simulate-burn-in-cell "u-clean" clean))))
      (is (false? (:passed? (robotics/simulate-burn-in-cell "u-bad-thermal" bad-thermal))))
      (is (false? (:passed? (robotics/simulate-burn-in-cell "u-bad-connector" bad-connector))))
      (testing "the mission now walks four steps, not three -- the connector-mating step is additive"
        (is (= 4 (count (:actions (robotics/simulate-burn-in-cell "u-clean" clean)))))
        (is (= 4 (count robotics/mission-actions)))))))

(deftest simulation-out-of-tolerance-still-checks-thermal-margin-only
  (testing "the pre-existing governor recheck function keeps its original, narrower meaning (thermal-margin only) -- the governor calls the NEW connector-mating-force-out-of-tolerance? separately, see deviceassembly.governor"
    (is (true? (robotics/simulation-out-of-tolerance? {:thermal-margin-deviation-actual 0.30 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05})))
    (is (false? (robotics/simulation-out-of-tolerance? {:thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                                                          :sim-peak-insertion-force-n (+ robotics/max-insertion-force-n 100.0)}))
        "even a wildly out-of-tolerance connector-mating reading does not flip simulation-out-of-tolerance?, by design")))
