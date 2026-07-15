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
  through four :sense/:actuate steps -- automated functional-test rig,
  thermal burn-in stress test, robotic EMC pre-scan, and (ADR-2607991500,
  below) a CONNECTOR MATING/INSERTION-FORCE TEST -- built with
  `kotoba.robotics/action` + `kotoba.robotics/telemetry-proof`, and
  reports an overall :passed? verdict. `simulation-out-of-tolerance?`
  independently re-derives the thermal-margin half of that verdict from
  the device-unit's OWN recorded thermal-margin fields, never from the
  mission's self-reported result -- the SAME 'ground truth, not
  self-report' discipline `deviceassembly.registry/device-unit-emc-
  emission-out-of-range?` uses for EMC emissions (and `automotive.
  registry/vehicle-emissions-out-of-range?` established for that fleet
  sibling). `connector-mating-force-out-of-tolerance?` (ADR-2607991500)
  independently re-derives the connector-mating half of that verdict
  from the device-unit's OWN recorded REAL `physics-2d`-simulated
  telemetry, the same discipline. `deviceassembly.governor`'s
  `robotics-simulation-violations` calls BOTH independent rechecks,
  never the stored :passed? value, before any `:actuation/ship-device-
  unit` proposal may commit.

  ADR-2607991500 wires a SECOND, independent real-physics check into
  this ns (this actor's FIRST real time-stepped simulation -- until
  now `thermal-margin-out-of-range?` above was purely symbolic, a
  static comparison of self-reported fields with no physics timestep
  at all): a genuine time-stepped `physics-2d` rigid-body simulation of
  a CONNECTOR MATING/INSERTION-FORCE TEST -- a real computer/
  peripheral-equipment QA procedure (per the IEC 60512 series of
  connector test standards, e.g. IEC 60512-13-1 'insertion and
  withdrawal forces': a test rig drives a connector plug -- USB-C/
  HDMI/M.2-class, the kind of data/power connector a laptop/desktop/
  server/monitor final-assembly build actually carries -- into its
  mating receptacle at a controlled rate and records the peak force at
  full seating, comparing it against the connector's own specified
  maximum mated-insertion-force limit). This is ADDITIONAL to (never a
  replacement for) the existing burn-in/EMC `thermal-margin-out-of-
  range?` check above -- the two verify UNRELATED real-world QA domains
  (thermal/EMC qualification of the whole device-unit vs. connector
  mechanical mating on the unit's own ports), so this ADR keeps
  `thermal-margin-out-of-range?`/`simulation-out-of-tolerance?` exactly
  as they were and adds `connector-mating-force-out-of-tolerance?`
  beside them, both folded into the SAME `simulate-burn-in-cell`
  mission/verdict -- mirroring how `autoparts.robotics`/`commsdevice.
  robotics` each added their own real `physics-2d` check alongside
  (not instead of) this fleet's established checks.

  Unlike `autoparts.robotics`'s weld-joint/fastener PULL test (which
  had to be honestly REFRAMED as an approach against a virtual
  limit-boundary, because `physics-2d` only natively resolves bodies
  that are approaching/colliding, never separating under tension), a
  connector mating/insertion event genuinely IS an approach/contact
  event -- the plug really does travel toward and collide with its
  receptacle -- so this ns needs no reframing trick, the same simpler,
  direct shape `commsdevice.robotics`'s OCA-lamination display-bonding
  press test uses. Like `autoparts.robotics`/`commsdevice.robotics`,
  this vertical has no design-library sibling repo, so the physics
  module lives DIRECTLY in this ns and takes a real pinned
  git-coordinate dependency on `kotoba-lang/physics-2d` alone (see
  `deps.edn`), no BREP/CAM/webgpu-scene bridge.

  A real `physics-2d` `Body2D` -- `:plug` -- approaches a static (mass
  0, immovable) `:receptacle` `Body2D` at a controlled rate;
  `world-step` actually integrates/collides/resolves the contact over
  real ticks, and `:sim-peak-insertion-force-n` is read directly off
  the ACTUAL simulated velocity trajectory (`connector-mating-
  telemetry-for` below) -- not invented.

  Disclosed engineering priors for the connector-mating simulation
  (this ns's own, not measured facts -- same discipline as
  `autoparts.robotics`'s pull-test constants / `commsdevice.robotics`'s
  press-bond constants):

  - `insertion-speed-mps` is a deliberately chosen, disclosed ANALOG
    insertion rate (fast enough for `physics-2d`'s single-tick
    boxcar-collision model to produce a physically meaningful impulse),
    NOT a literal reproduction of a real IEC 60512-13-1-style
    insertion-force test rig's actual (much slower, quasi-static,
    force-gauge-driven crosshead) speed -- the SAME honest disclosure
    `commsdevice.robotics`'s `press-closing-velocity-mps` makes for its
    OCA-lamination press-bond test.
  - `travel-to-seat-m` is a representative low-single-digit-millimeter
    order of magnitude for a small-form-factor data/power connector's
    (USB-C/HDMI/M.2-class) full mating-engagement stroke from first
    contact to full seating -- a real, disclosed prior, not a
    measurement of any specific device-unit's connector.
  - `initial-approach-slack-m` is a small, real, disclosed pre-contact
    standoff the plug travels BEFORE the receptacle itself begins to
    bear load -- present only so the simulated trajectory captures a
    real pre-seating approach phase, not just the single stopping tick
    (mirrors `commsdevice.robotics`'s `gap-m` / `autoparts.robotics`'s
    `initial-grip-slack-m`).
  - `max-insertion-force-n` is a newly-defined, clearly-disclosed
    real-world ceiling (the SAME allowance ADR-2607152000 established
    for `autoparts.robotics`'s `min-proof-load-n`, applied here as a
    MAX instead of a MIN) -- a plausible maximum acceptable peak
    mated-insertion force for a small-form-factor computer/peripheral
    data/power connector class, NOT a literal transcription of one
    specific named standard's exact figure. Too much insertion force at
    seating risks connector-housing/board-level solder-joint damage and
    signals misalignment/binding -- the real failure mode IEC
    60512-13-1-style insertion-force testing screens for.

  By the SAME exact kinematic identity `commsdevice.robotics` documents
  (a = v^2/d for a boxcar full stop over transit distance d at speed
  v), `:sim-peak-decel-mps2` here is PROVABLY INDEPENDENT of the
  plug's own mass when colliding with a mass-0 (immovable) receptacle
  -- mass cancels algebraically in `physics-2d`'s `resolve-contact` --
  so it is the device-unit's own recorded `:connector-plug-mass-kg`
  (the effective participating mass of the moving test-probe assembly:
  the robotic insertion-test actuator head + the connector plug
  itself, NOT just the bare plug's own mass -- the same 'effective
  participating mass' framing `autoparts.robotics`'s `:joint-mass-kg`
  uses) that actually moves `:sim-peak-insertion-force-n` (via
  F = m*a), never the insertion speed or seating travel (both fixed
  constants, shared by every device-unit).

  `connector-mating-force-out-of-tolerance?` independently re-derives
  the device-unit's OWN recorded `:sim-peak-insertion-force-n` against
  `max-insertion-force-n`, never from the mission's self-reported
  result -- the SAME 'ground truth, not self-report' discipline
  `thermal-margin-out-of-range?` above and `deviceassembly.registry/
  device-unit-emc-emission-out-of-range?` already establish.
  `deviceassembly.governor`'s `robotics-simulation-violations` calls
  this independent recheck too, never the stored :passed? value,
  before any `:actuation/ship-device-unit` proposal may commit.

  Pure data + pure functions -- no real robot I/O, no network.
  `physics-2d/world-step` is itself a pure, fixed-timestep integrator
  (no wall-clock/IO), so this stays exactly as offline/deterministic as
  every other sibling namespace in this actor -- tests and the demo run
  without a network.

  Honest scope (mirrors `autoparts.robotics`/`commsdevice.robotics`):
  this DOES model a real time-stepped `physics-2d` rigid-body
  trajectory for the connector-mating event. It does NOT model:
  connector contact-spring/normal-force compliance (`physics-2d` has no
  force-deflection/spring model at all -- the connector's own real
  multi-contact-pin normal-force behavior cannot itself vary the
  simulated reading, only this device-unit's own recorded
  `:connector-plug-mass-kg` can), 3D geometry (2D projection only, the
  same disclosed limit every sibling states), a real load-cell/DAQ
  connection, or a real robot controller -- still simulation, not
  control, the same 'policy, not control' boundary `kotoba.robotics`'s
  docstring already establishes."
  (:require [kotoba.robotics :as robotics]
            [physics-2d :as p2d]))

;; ---------------------------------------------------------------------------
;; Platform shims (mirrors physics-2d's own private sqrt*/abs*/signum* style
;; and `autoparts.robotics`'s identical shims, keeping this ns portable
;; .cljc -- a raw Math/ceil + Math/abs would be JVM-only and break a
;; ClojureScript consumer).
;; ---------------------------------------------------------------------------

(defn- abs* [x] (if (neg? x) (- x) x))

(defn- ceil* [x]
  #?(:clj  (Math/ceil (double x))
     :cljs (js/Math.ceil x)))

(def mission-actions
  "The four-step burn-in-cell verification mission every device-unit
  walks through before `:actuation/ship-device-unit` is proposable. All
  :sense/:actuate at :none/:low safety -- verification/QA sensing and a
  bounded thermal-stress cycle / connector-mating test on a stationary
  device-unit, not the distribution actuation that is `:actuation/
  ship-device-unit` itself (always :safety-critical -- see
  `deviceassembly.governor`). `:connector-mating-insertion-force-test`
  (ADR-2607991500) is the ADDITIONAL real `physics-2d`-simulated step;
  the first three steps are unchanged from before that ADR."
  [{:step :automated-functional-test              :kind :sense   :safety :none}
   {:step :thermal-burn-in-stress-test             :kind :actuate :safety :low}
   {:step :robotic-emc-pre-scan                    :kind :sense   :safety :none}
   {:step :connector-mating-insertion-force-test   :kind :actuate :safety :low}])

(defn thermal-margin-out-of-range?
  "Ground-truth check: does `device-unit`'s own recorded
  :thermal-margin-deviation-actual fall outside its own recorded
  [:thermal-margin-deviation-min :thermal-margin-deviation-max] bounds
  (deviation of the measured operating thermal margin from the
  qualification spec -- positive means LESS margin than required)?
  Needs no mission run or proposal inspection -- its inputs are
  permanent fields already on the device-unit, the same shape
  `deviceassembly.registry/device-unit-emc-emission-out-of-range?` uses
  for EMC emissions. Purely symbolic -- a static field comparison, no
  physics timestep (see `connector-mating-force-out-of-tolerance?`
  below for this ns's REAL time-stepped `physics-2d` check,
  ADR-2607991500)."
  [{:keys [thermal-margin-deviation-actual thermal-margin-deviation-min thermal-margin-deviation-max]}]
  (and (number? thermal-margin-deviation-actual) (number? thermal-margin-deviation-min) (number? thermal-margin-deviation-max)
       (or (< thermal-margin-deviation-actual thermal-margin-deviation-min)
           (> thermal-margin-deviation-actual thermal-margin-deviation-max))))

;; ---------------------- real connector-mating physics constants ------------

(def ^:const insertion-speed-mps
  "Controlled connector-plug insertion rate (m/s) -- see ns docstring:
  a deliberately chosen, disclosed ANALOG rate (fast enough for
  `physics-2d`'s single-tick boxcar-collision model to produce a
  physically meaningful impulse), not a literal transcription of a
  real IEC 60512-13-1-style insertion-force test rig's actual
  (much slower, quasi-static, force-gauge-driven crosshead) speed."
  0.15)

(def ^:const travel-to-seat-m
  "The connector's own real mating-engagement travel (m) from first
  contact to full seating -- see ns docstring: a representative
  low-single-digit-millimeter prior for a small-form-factor data/power
  connector's (USB-C/HDMI/M.2-class) full insertion stroke."
  0.003)

(def ^:const initial-approach-slack-m
  "Pre-contact standoff distance (m) the plug travels before the
  receptacle itself begins to bear load -- present only so the
  trajectory captures a real pre-seating approach phase, mirroring
  `commsdevice.robotics`'s `gap-m` / `autoparts.robotics`'s
  `initial-grip-slack-m`."
  0.01)

(def ^:const plug-half-w-m
  "Connector-plug AABB half-width along the insertion axis (m) -- a
  small, fixed connector-scale footprint, not a per-device-unit CAD
  input (this ns has no CAD/BREP pipeline, unlike automotive's
  envelope-solid bridge)."
  0.002)

(def ^:const plug-half-h-m 0.004)

(def ^:const receptacle-half-w-m
  "Receptacle AABB half-width (m) -- static anchor; `physics-2d`
  treats a mass-0 body as having zero inverse mass (immovable), which
  is also physically apt here -- a real connector receptacle is
  mounted to the device-unit's own chassis/board, not free to recoil."
  0.002)

(def ^:const receptacle-half-h-m 0.006)

(def ^:const settle-ticks
  "Extra ticks appended after the plug is expected to reach the
  receptacle, so the trajectory also captures post-seating settling.
  `physics-2d`'s positional correction removes 80% of any remaining
  overlap per tick (`resolve-contact`'s `0.8` factor), so residual
  overlap after `settle-ticks` further ticks is `0.2^settle-ticks` of
  whatever it was at first contact -- 15 ticks converges to ~3e-11
  (same rationale/constant as `autoparts.robotics`'s /
  `commsdevice.robotics`'s `settle-ticks`, a genuine physics-2d engine
  property, not re-derived here)."
  15)

(def ^:const max-insertion-force-n
  "Real, disclosed maximum acceptable peak connector mating/insertion
  force (N) -- see ns docstring for the reasoned-engineering-estimate
  disclosure. A plausible ceiling for a small-form-factor computer/
  peripheral data/power connector class (USB-C/HDMI/M.2-class), NOT a
  literal transcription of one specific named standard's exact figure
  (ADR-2607991500 explicitly allows this, the same allowance
  ADR-2607152000 gave `autoparts.robotics/min-proof-load-n`)."
  30.0)

;; ------------------------------ real simulation ------------------------------

(defn run-connector-mating-test
  "Time-steps a REAL `physics-2d` world for the connector mating/
  insertion-force test and returns:

    {:trajectory [{:tick :position :velocity} ...]   ; plug body only
     :sim-peak-decel-mps2 n :sim-peak-insertion-force-n n
     :ticks n :dt n :insertion-speed-mps n :travel-to-seat-m n}

  `plug-mass-kg` is the device-unit's own recorded effective
  participating mass of the moving test-probe assembly (robotic
  insertion-test actuator head + connector plug -- see ns docstring).
  opts (all optional, for tuning/testing): `:insertion-speed-mps`,
  `:travel-to-seat-m`, `:initial-approach-slack-m`, `:dt` overrides
  (each defaults to this ns's own constant of the same name).

  `:sim-peak-decel-mps2` is the PEAK magnitude of tick-to-tick velocity
  change (along the insertion axis) divided by `dt` -- derived from
  the actual simulated velocity trajectory, not invented; by the
  kinematic identity this ns's docstring documents, it is provably
  independent of `plug-mass-kg` (the receptacle is immovable).
  `:sim-peak-insertion-force-n` is `:sim-peak-decel-mps2 * plug-mass-
  kg` (Newtons) -- see ns docstring for why mass legitimately scales
  this reading."
  [plug-mass-kg & [{v-opt :insertion-speed-mps travel-opt :travel-to-seat-m
                     slack-opt :initial-approach-slack-m dt-opt :dt}]]
  (let [v      (double (or v-opt insertion-speed-mps))
        travel (double (or travel-opt travel-to-seat-m))
        slack  (double (or slack-opt initial-approach-slack-m))
        dt     (double (or dt-opt (/ travel v)))
        receptacle-x 0.0
        plug-x0 (- receptacle-x receptacle-half-w-m plug-half-w-m slack)
        approach-m (+ slack travel)
        ticks (long (+ settle-ticks (long (ceil* (/ approach-m (* v dt))))))
        receptacle (p2d/make-body {:position [receptacle-x 0.0]
                                    :velocity [0.0 0.0]
                                    :mass 0.0
                                    :restitution 0.0
                                    :friction 0.0
                                    :collider (p2d/make-aabb-collider receptacle-half-w-m receptacle-half-h-m)
                                    :user-data :receptacle})
        plug (p2d/make-body {:position [plug-x0 0.0]
                              :velocity [v 0.0]
                              :mass (double plug-mass-kg)
                              :restitution 0.0
                              :friction 0.0
                              :collider (p2d/make-aabb-collider plug-half-w-m plug-half-h-m)
                              :user-data :plug})
        w0 (p2d/world-new [0.0 0.0])
        [w1 _receptacle-id] (p2d/world-add w0 receptacle)
        [w2 plug-id] (p2d/world-add w1 plug)
        worlds (reductions (fn [w _] (p2d/world-step w dt)) w2 (range ticks))
        trajectory (mapv (fn [tick world]
                            (let [b (nth (:bodies world) plug-id)]
                              {:tick tick :position (:position b) :velocity (:velocity b)}))
                          (range (count worlds)) worlds)
        vxs (mapv (comp first :velocity) trajectory)
        peak-decel-mps2 (->> (map (fn [va vb] (abs* (/ (- vb va) dt))) vxs (rest vxs))
                              (reduce max 0.0))]
    {:trajectory trajectory
     :sim-peak-decel-mps2 peak-decel-mps2
     :sim-peak-insertion-force-n (* peak-decel-mps2 (double plug-mass-kg))
     :ticks (count trajectory)
     :dt dt
     :insertion-speed-mps v
     :travel-to-seat-m travel}))

(defn connector-mating-telemetry-for
  "Runs the REAL `run-connector-mating-test` `physics-2d` simulation
  for `device-unit`'s own recorded `:connector-plug-mass-kg` and
  returns the actual simulated trajectory telemetry:
  {:sim-peak-insertion-force-n n :sim-peak-decel-mps2 n :ticks n :dt n
  :insertion-speed-mps n :travel-to-seat-m n}. Pure, deterministic --
  the same `:connector-plug-mass-kg` always reproduces the same
  telemetry."
  [device-unit]
  (select-keys (run-connector-mating-test (:connector-plug-mass-kg device-unit))
               [:sim-peak-insertion-force-n :sim-peak-decel-mps2 :ticks :dt
                :insertion-speed-mps :travel-to-seat-m]))

(defn connector-mating-force-out-of-tolerance?
  "Ground-truth check: does `device-unit`'s own recorded
  `:sim-peak-insertion-force-n` (the REAL `run-connector-mating-test`
  trajectory telemetry already on file for this device-unit -- see
  `connector-mating-telemetry-for`) exceed `max-insertion-force-n`?
  Needs no mission run -- its inputs are permanent fields already on
  the device-unit once a mission has recorded them, the same shape
  `thermal-margin-out-of-range?` above uses."
  [{:keys [sim-peak-insertion-force-n]}]
  (and (number? sim-peak-insertion-force-n)
       (> sim-peak-insertion-force-n max-insertion-force-n)))

(defn simulate-burn-in-cell
  "Run the robot burn-in-cell verification mission for `device-unit-id`
  (`device-unit` is the full device-unit record, incl. thermal-margin-
  deviation-* fields AND `:connector-plug-mass-kg`). Returns {:mission
  .. :actions [{:action .. :proof ..} ..] :passed? bool
  :sim-peak-insertion-force-n n :sim-peak-decel-mps2 n}.

  Actually runs the REAL engine for the connector-mating half:
  `connector-mating-telemetry-for` -- the actual `physics-2d`-stepped
  plug/receptacle collision trajectory. Deterministic: :passed? is
  derived from the device-unit's OWN recorded thermal-margin fields via
  `thermal-margin-out-of-range?` AND its own recorded
  `:connector-plug-mass-kg` via the REAL simulated trajectory
  (`connector-mating-force-out-of-tolerance?`) -- either check failing
  fails the whole mission, never invented or randomized. `kotoba.
  robotics` mandates no network/IO, and a repeatable simulation is what
  makes the governor's independent recheck meaningful."
  [device-unit-id device-unit]
  (let [thermal-out-of-range? (thermal-margin-out-of-range? device-unit)
        connector-telemetry (connector-mating-telemetry-for device-unit)
        connector-out-of-range? (connector-mating-force-out-of-tolerance? connector-telemetry)
        out-of-range? (or thermal-out-of-range? connector-out-of-range?)
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
     :passed? (not out-of-range?)
     :sim-peak-insertion-force-n (:sim-peak-insertion-force-n connector-telemetry)
     :sim-peak-decel-mps2 (:sim-peak-decel-mps2 connector-telemetry)}))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does
  `device-unit`'s OWN current thermal-margin fields fall out of range
  right now? Ignores whatever :passed? verdict a prior mission run
  stored -- identical in spirit to `deviceassembly.registry/device-
  unit-emc-emission-out-of-range?`'s refusal to trust a proposal's
  self-report. Covers ONLY the burn-in/EMC thermal-margin half of the
  mission verdict -- see `connector-mating-force-out-of-tolerance?`
  above for the ADR-2607991500 connector-mating half; `deviceassembly.
  governor`'s `robotics-simulation-violations` calls BOTH."
  [device-unit]
  (thermal-margin-out-of-range? device-unit))
