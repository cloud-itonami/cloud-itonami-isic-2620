(ns deviceassembly.robotics
  "Robot-executed burn-in/EMC-pre-scan verification -- the concrete,
  actor-level realization of ADR-2607011000's robotics premise (every
  cloud-itonami vertical is designed on the premise that a robot
  performs the physical-domain work; an independent governor gates any
  action before it ever reaches hardware) and ADR-2607142800's
  fleet-wide robotics-process-simulation pattern (established by
  `cloud-itonami-isic-2910`'s `automotive.robotics`), applied to THIS
  actor's own `deviceassembly.facts` requirement that a shipment
  proposal cite an EMC-test-report actually on file -- not merely a
  self-reported checklist string.

  A robot mission (`kotoba.robotics/mission`) walks the device-unit
  through three :sense/:actuate steps -- automated functional-test rig,
  thermal burn-in stress test, robotic EMC pre-scan -- built with
  `kotoba.robotics/action` + `kotoba.robotics/telemetry-proof`, and
  reports an overall :passed? verdict. `simulation-out-of-tolerance?`
  independently re-derives that verdict from the device-unit's OWN
  recorded thermal-margin fields, never from the mission's self-
  reported result -- the SAME 'ground truth, not self-report'
  discipline `deviceassembly.registry/device-unit-emc-emission-out-of-
  range?` uses for EMC emissions (and `automotive.registry/vehicle-
  emissions-out-of-range?` established for that fleet sibling).
  `deviceassembly.governor`'s `robotics-simulation-violations` calls
  this ns's independent recheck, never the stored :passed? value,
  before any `:actuation/ship-device-unit` proposal may commit.

  Pure data + pure functions -- no real robot I/O, no network.
  `kotoba.robotics` is itself \"policy, not control\"; this namespace
  simulates what a real burn-in cell would report, deterministically,
  from the device-unit's own recorded fields, so tests and the demo run
  offline exactly like every other sibling namespace in this actor."
  (:require [kotoba.robotics :as robotics]))

(def mission-actions
  "The three-step burn-in-cell verification mission every device-unit
  walks through before `:actuation/ship-device-unit` is proposable. All
  :sense/:actuate at :none/:low safety -- verification/QA sensing and a
  bounded thermal-stress cycle on a stationary device-unit, not the
  distribution actuation that is `:actuation/ship-device-unit` itself
  (always :safety-critical -- see `deviceassembly.governor`)."
  [{:step :automated-functional-test    :kind :sense   :safety :none}
   {:step :thermal-burn-in-stress-test  :kind :actuate :safety :low}
   {:step :robotic-emc-pre-scan         :kind :sense   :safety :none}])

(defn thermal-margin-out-of-range?
  "Ground-truth check: does `device-unit`'s own recorded
  :thermal-margin-deviation-actual fall outside its own recorded
  [:thermal-margin-deviation-min :thermal-margin-deviation-max] bounds
  (deviation of the measured operating thermal margin from the
  qualification spec -- positive means LESS margin than required)?
  Needs no mission run or proposal inspection -- its inputs are
  permanent fields already on the device-unit, the same shape
  `deviceassembly.registry/device-unit-emc-emission-out-of-range?` uses
  for EMC emissions."
  [{:keys [thermal-margin-deviation-actual thermal-margin-deviation-min thermal-margin-deviation-max]}]
  (and (number? thermal-margin-deviation-actual) (number? thermal-margin-deviation-min) (number? thermal-margin-deviation-max)
       (or (< thermal-margin-deviation-actual thermal-margin-deviation-min)
           (> thermal-margin-deviation-actual thermal-margin-deviation-max))))

(defn simulate-burn-in-cell
  "Run the robot burn-in-cell verification mission for `device-unit-id`
  (`device-unit` is the full device-unit record, incl. thermal-margin-
  deviation-* fields). Returns {:mission .. :actions [{:action ..
  :proof ..} ..] :passed? bool}. Deterministic: :passed? is derived
  from the device-unit's OWN recorded thermal-margin fields via
  `thermal-margin-out-of-range?`, never invented or randomized --
  `kotoba.robotics` mandates no network/IO, and a repeatable simulation
  is what makes the governor's independent recheck
  (`simulation-out-of-tolerance?`) meaningful."
  [device-unit-id device-unit]
  (let [out-of-range? (thermal-margin-out-of-range? device-unit)
        reading (if out-of-range? :out-of-tolerance :nominal)
        mission (robotics/mission (str "mission-" device-unit-id "-burn-in-verify")
                                   :robot/burn-in-cell-1
                                   :burn-in-emc-verification
                                   :boundaries {:station "end-of-line-burn-in-cell"}
                                   :max-steps (count mission-actions))
        actions (mapv (fn [{:keys [step kind safety]}]
                        (let [a (robotics/action (str (:mission/id mission) "-" (name step))
                                                  (:mission/id mission) kind safety
                                                  :params {:step step :device-unit-id device-unit-id})]
                          {:action a
                           :proof (robotics/telemetry-proof (:mission/id mission) step reading
                                                             :provenance :simulated)}))
                      mission-actions)]
    {:mission mission
     :actions actions
     :passed? (not out-of-range?)}))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does
  `device-unit`'s OWN current thermal-margin fields fall out of range
  right now? Ignores whatever :passed? verdict a prior mission run
  stored -- identical in spirit to `deviceassembly.registry/device-
  unit-emc-emission-out-of-range?`'s refusal to trust a proposal's
  self-report."
  [device-unit]
  (thermal-margin-out-of-range? device-unit))
