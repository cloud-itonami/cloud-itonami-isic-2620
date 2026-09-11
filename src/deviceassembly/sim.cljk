(ns deviceassembly.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean device-unit
  through intake -> compliance-rules requirements verification ->
  end-of-line-defect screening -> burn-in-cell robotics mission ->
  device-unit-shipment proposal (always escalates) -> human approval ->
  commit, then through Declaration-of-Conformity proposal (always
  escalates) -> human approval -> commit, then shows six HARD holds (a
  jurisdiction with no spec-basis, a shipment proposed before any
  robotics mission ran, an out-of-spec EMC emission deviation, a
  robotics mission already on file but out-of-tolerance on independent
  recheck, an unresolved end-of-line defect screened directly via
  `:end-of-line-quality/screen` [never via an actuation op against an
  unscreened device-unit -- see this actor's own governor ns docstring
  / the lesson `automotive`'s (cloud-itonami-isic-2910), `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s, `conservation`'s,
  `salon`'s, `entertainment`'s, `casework`'s, `hospital`'s, `facility`'s,
  `school`'s, `association`'s, `leasing`'s, `behavioral`'s,
  `secondary`'s, `card`'s, `water`'s, `telecom`'s, `turbine`'s and
  `steelworks`'s ADR-0001s already recorded], and a double device-
  unit-shipment/declaration-issuance of an already-processed
  device-unit) that never reach a human at all, and prints the audit
  ledger + the draft shipment and Declaration-of-Conformity records."
  (:require [langgraph.graph :as g]
            [deviceassembly.export :as export]
            [deviceassembly.store :as store]
            [deviceassembly.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :compliance-engineer :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== device-unit/intake device-unit-1 (JPN, clean; EMC within spec, no EOL defect) ==")
    (println (exec! actor "t1" {:op :device-unit/intake :subject "device-unit-1"
                                :patch {:id "device-unit-1" :device-unit-name "Sakura Notebook NB-14"}} operator))

    (println "== compliance-rules/verify device-unit-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :compliance-rules/verify :subject "device-unit-1"} operator))
    (println (approve! actor "t2"))

    (println "== end-of-line-quality/screen device-unit-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :end-of-line-quality/screen :subject "device-unit-1"} operator))
    (println (approve! actor "t3"))

    (println "== robotics/simulate-burn-in-cell device-unit-1 (burn-in/EMC-pre-scan mission; escalates -- human approves) ==")
    (println (exec! actor "t3b" {:op :robotics/simulate-burn-in-cell :subject "device-unit-1"} operator))
    (println (approve! actor "t3b"))

    (println "== actuation/ship-device-unit device-unit-1 (always escalates -- actuation/ship-device-unit) ==")
    (let [r (exec! actor "t4" {:op :actuation/ship-device-unit :subject "device-unit-1"} operator)]
      (println r)
      (println "-- human compliance engineer approves --")
      (println (approve! actor "t4")))

    (println "== actuation/issue-declaration-of-conformity device-unit-1 (always escalates -- actuation/issue-declaration-of-conformity) ==")
    (let [r (exec! actor "t5" {:op :actuation/issue-declaration-of-conformity :subject "device-unit-1"} operator)]
      (println r)
      (println "-- human compliance engineer approves --")
      (println (approve! actor "t5")))

    (println "== compliance-rules/verify device-unit-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t6" {:op :compliance-rules/verify :subject "device-unit-2" :no-spec? true} operator))

    (println "== compliance-rules/verify device-unit-3 (escalates -- human approves; sets up the out-of-spec test) ==")
    (println (exec! actor "t7" {:op :compliance-rules/verify :subject "device-unit-3"} operator))
    (println (approve! actor "t7"))

    (println "== actuation/ship-device-unit device-unit-3 before robotics simulation -> HARD hold (robotics-simulation-missing) ==")
    (println (exec! actor "t7b" {:op :actuation/ship-device-unit :subject "device-unit-3"} operator))

    (println "== robotics/simulate-burn-in-cell device-unit-3 (clean thermal-margin deviation; escalates -- human approves) ==")
    (println (exec! actor "t7c" {:op :robotics/simulate-burn-in-cell :subject "device-unit-3"} operator))
    (println (approve! actor "t7c"))

    (println "== actuation/ship-device-unit device-unit-3 (0.35 outside [-0.10,0.10] EMC tolerance -> HARD hold) ==")
    (println (exec! actor "t8" {:op :actuation/ship-device-unit :subject "device-unit-3"} operator))

    (println "== actuation/ship-device-unit device-unit-5 (robotics-sim on file, but thermal-margin deviation 0.30 outside [-0.05,0.05] tolerance on independent recheck -> HARD hold) ==")
    (println (exec! actor "t8b" {:op :compliance-rules/verify :subject "device-unit-5"} operator))
    (println (approve! actor "t8b"))
    (println (exec! actor "t8c" {:op :actuation/ship-device-unit :subject "device-unit-5"} operator))

    (println "== end-of-line-quality/screen device-unit-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t9" {:op :end-of-line-quality/screen :subject "device-unit-4"} operator))

    (println "== actuation/ship-device-unit device-unit-1 AGAIN (double-shipment -> HARD hold) ==")
    (println (exec! actor "t10" {:op :actuation/ship-device-unit :subject "device-unit-1"} operator))

    (println "== actuation/issue-declaration-of-conformity device-unit-1 AGAIN (double-issuance -> HARD hold) ==")
    (println (exec! actor "t11" {:op :actuation/issue-declaration-of-conformity :subject "device-unit-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft device-unit-shipment records ==")
    (doseq [r (store/shipment-history db)] (println r))

    (println "== draft Declaration-of-Conformity records ==")
    (doseq [r (store/declaration-history db)] (println r))

    (println "== social hand-off: audit package counts ==")
    (println (:counts (export/audit-package db)))
    (println "== social hand-off: CSV bundle keys ==")
    (println (keys (export/package->csv-bundle db)))))
