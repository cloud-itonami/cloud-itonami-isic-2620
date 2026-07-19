(ns deviceassembly.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout template: `90-docs/business/cloud-itonami-flagship-
  generator-template.edn`). This repo previously had a HAND-AUTHORED
  `docs/samples/operator-console.html` (a generic boilerplate landing
  page pasted from a single `clojure -M:dev:run` transcript, never
  regenerated). This namespace REPLACES that with a genuine build-time
  generator that drives the REAL actor stack (`deviceassembly.operation`
  -> `deviceassembly.governor` -> `deviceassembly.store`) through a
  scenario adapted from this repo's own `deviceassembly.sim` demo driver
  (`clojure -M:dev:run`, confirmed by running it directly -- its ids
  (device-unit-1..6), ops and violation rule names all match
  `deviceassembly.store/demo-data`'s real seed data and
  `deviceassembly.governor`'s real check functions; unlike
  `cloud-itonami-isic-851`'s known-bad `schoolops.sim`, this repo's own
  sim driver was safe to mine directly rather than author from scratch),
  trimmed to a representative subset (one full auto-commit, three
  always-escalate/phase-escalate -> approve lifecycles building
  device-unit-1 up to a real shipment + Declaration of Conformity, and
  four distinct HARD-hold reasons -- five distinct governor rule names
  across those four holds -- that never reach a human) and rendered
  deterministically -- no invented numbers, no timestamps in the page
  content, byte-identical across reruns against the same seed (verify
  by diffing two consecutive runs before shipping). The
  `:connector-mating-force-out-of-tolerance` hold below is driven by a
  REAL `physics-2d`-stepped simulation result (device-unit-6's own
  recorded 5.0kg `:connector-plug-mass-kg` genuinely produces a
  37.5N peak insertion force against the 30.0N ceiling,
  `deviceassembly.robotics`/ADR-2607991500) -- not a hand-typed number.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [deviceassembly.store :as store]
            [deviceassembly.operation :as op]
            [langgraph.graph :as g]))

;; ----------------------------- harness (unchanged across every repo
;; in this cluster -- do not rewrite, only copy) -----------------------

(def ^:private operator
  {:actor-id "op-1" :actor-role :compliance-engineer :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach:

  Clean path:
    - device-unit-1 :device-unit/intake normalizes the device-unit
      record (governor-clean, high confidence, phase-3 `:auto` -- the
      ONLY op in `deviceassembly.phase`'s phase-3 auto set -- auto-
      commits, no human).

  Escalate -> approve lifecycles (building device-unit-1 up to a real
  shipment + Declaration of Conformity):
    - device-unit-1 :compliance-rules/verify drafts the JPN evidence
      checklist (governor-clean, but not in any phase's `:auto` set --
      escalates on `:phase-approval`, approved).
    - device-unit-1 :end-of-line-quality/screen finds no unresolved
      defect (governor-clean -- escalates, approved).
    - device-unit-1 :robotics/simulate-burn-in-cell runs the REAL
      burn-in-cell + connector-mating `physics-2d` mission (passes --
      escalates, approved).
    - device-unit-1 :actuation/ship-device-unit is ALWAYS high-stakes
      per `deviceassembly.governor/high-stakes`, regardless of
      confidence -- escalates, approved by a human compliance engineer,
      commits a real device-unit-shipment draft.
    - device-unit-1 :actuation/issue-declaration-of-conformity is
      likewise ALWAYS high-stakes -- escalates, approved, commits a
      real Declaration-of-Conformity draft.

  HARD-hold paths (never reach a human, five distinct real governor
  rules from `deviceassembly.governor` across four ledger facts):
    - device-unit-2 :compliance-rules/verify with `:no-spec?` proposes
      against jurisdiction \"ATL\", which has no entry in
      `deviceassembly.facts/catalog` -> `:no-spec-basis`.
    - device-unit-4 :end-of-line-quality/screen -- device-unit-4's own
      seed record carries `:eol-defect-unresolved? true` ->
      `:end-of-line-defect-unresolved`.
    - device-unit-6 :actuation/ship-device-unit, attempted with NO
      prior `:compliance-rules/verify` on file AND device-unit-6's own
      recorded `:connector-plug-mass-kg` (5.0kg, deliberately far
      heavier than any genuine small-form-factor connector-mating test
      rig) -> TWO real rules in the SAME hold: `:evidence-incomplete`
      (no verification on file) and `:connector-mating-force-out-of-
      tolerance` (the REAL `physics-2d` simulation independently
      recomputes a 37.5N peak insertion force against the 30.0N
      ceiling, even though `:robotics-sim-verified?` was seeded `true`
      -- 'already on file' never overrides the independent recheck).
    - device-unit-1 :actuation/ship-device-unit AGAIN, after it already
      shipped above -> `:already-shipped`, off the dedicated
      `:device-unit-shipped?` fact (never a `:status` value).

  Returns the resulting store -- every field `render` below reads is
  real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]

    (exec! actor "t1-intake"
           {:op :device-unit/intake :subject "device-unit-1"
            :patch {:id "device-unit-1" :device-unit-name "Sakura Notebook NB-14"}})

    (exec! actor "t2-verify" {:op :compliance-rules/verify :subject "device-unit-1"})
    (approve! actor "t2-verify")

    (exec! actor "t3-screen" {:op :end-of-line-quality/screen :subject "device-unit-1"})
    (approve! actor "t3-screen")

    (exec! actor "t4-robotics" {:op :robotics/simulate-burn-in-cell :subject "device-unit-1"})
    (approve! actor "t4-robotics")

    (exec! actor "t5-ship" {:op :actuation/ship-device-unit :subject "device-unit-1"})
    (approve! actor "t5-ship")

    (exec! actor "t6-declare" {:op :actuation/issue-declaration-of-conformity :subject "device-unit-1"})
    (approve! actor "t6-declare")

    (exec! actor "t7-no-spec-basis"
           {:op :compliance-rules/verify :subject "device-unit-2" :no-spec? true})

    (exec! actor "t8-eol-unresolved"
           {:op :end-of-line-quality/screen :subject "device-unit-4"})

    (exec! actor "t9-connector-mating-out-of-tolerance"
           {:op :actuation/ship-device-unit :subject "device-unit-6"})

    (exec! actor "t10-already-shipped"
           {:op :actuation/ship-device-unit :subject "device-unit-1"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for
  "Every op in this domain keys its ledger fact's `:subject` on the
  device-unit-id itself (unlike siblings whose actuation ops key on a
  separate maintenance-id/shipment-id) -- see `deviceassembly.operation`
  `commit-fact`/`deviceassembly.governor` `hold-fact`, both of which set
  `:subject (:subject request)`, and every op in this scenario passes
  the device-unit-id as `:subject`. So a per-device-unit ledger lookup
  genuinely reflects that unit's own most recent op outcome -- for
  device-unit-1 that is the REJECTED double-shipment attempt (t10),
  which is why the ground-truth `Shipped`/`Declared` columns below are
  read directly from the device-unit record (`:device-unit-shipped?`/
  `:declaration-issued?`), not inferred from this last-fact status."
  [ledger subject-id]
  (last (filter #(= (:subject %) subject-id) ledger)))

(defn- status-cell [ledger subject-id]
  (let [f (last-fact-for ledger subject-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rules (map name (:basis f))]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (str/join ", " rules)) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- device-unit-row [ledger {:keys [id device-unit-name jurisdiction
                                        emc-emission-deviation-actual emc-emission-deviation-min emc-emission-deviation-max
                                        connector-plug-mass-kg sim-peak-insertion-force-n
                                        eol-defect-unresolved? robotics-sim-verified?
                                        device-unit-shipped? declaration-issued?]}]
  (format (str "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s &isin; [%s,%s]</td>"
               "<td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>")
          (esc id) (esc device-unit-name) (esc jurisdiction)
          (esc emc-emission-deviation-actual) (esc emc-emission-deviation-min) (esc emc-emission-deviation-max)
          (esc connector-plug-mass-kg) (esc sim-peak-insertion-force-n)
          (if eol-defect-unresolved? "<span class=\"err\">unresolved</span>" "<span class=\"ok\">resolved</span>")
          (if robotics-sim-verified? "<span class=\"ok\">yes</span>" "<span class=\"muted\">no</span>")
          (str (if device-unit-shipped? "<span class=\"ok\">shipped</span>" "<span class=\"muted\">not shipped</span>")
               " / "
               (if declaration-issued? "<span class=\"ok\">declared</span>" "<span class=\"muted\">not declared</span>"))
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", ")) (some-> disposition name) ""))))

(defn- draft-row [kind {:strs [record_id device_unit_id jurisdiction]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc record_id) (esc kind) (esc device_unit_id) (esc jurisdiction)))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README `Ops`
  ;; table, `deviceassembly.governor`/`deviceassembly.phase`) --
  ;; documentation of fixed behavior, not runtime telemetry, so it is
  ;; legitimately hand-described rather than derived from a live run.
  ["        <tr><td><code>:device-unit/intake</code></td><td><span class=\"ok\">auto-commit when clean, phase-3 (the ONLY phase-3 auto op)</span></td></tr>"
   "        <tr><td><code>:compliance-rules/verify</code></td><td><span class=\"warn\">human approval (phase-gated, never auto-eligible) &middot; spec-basis independently re-checked -- no jurisdiction requirements ever fabricated</span></td></tr>"
   "        <tr><td><code>:end-of-line-quality/screen</code></td><td><span class=\"critical\">unresolved defect is a HARD, un-overridable hold</span></td></tr>"
   "        <tr><td><code>:robotics/simulate-burn-in-cell</code></td><td><span class=\"warn\">human approval &middot; runs the REAL burn-in + connector-mating <code>physics-2d</code> mission</span></td></tr>"
   "        <tr><td><code>:actuation/ship-device-unit</code></td><td><span class=\"warn\">ALWAYS human approval (safety-critical, regardless of confidence) &middot; evidence/robotics-sim/EMC independently re-checked &middot; double-shipment blocked</span></td></tr>"
   "        <tr><td><code>:actuation/issue-declaration-of-conformity</code></td><td><span class=\"warn\">ALWAYS human approval (safety-critical) &middot; end-of-line defect independently re-checked &middot; double-issuance blocked</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        device-units (store/all-device-units db)
        device-unit-rows (str/join "\n" (map (partial device-unit-row ledger) device-units))
        ledger-rows (str/join "\n" (map ledger-row ledger))
        shipment-rows (str/join "\n" (map (partial draft-row "device-unit-shipment-draft") (store/shipment-history db)))
        declaration-rows (str/join "\n" (map (partial draft-row "declaration-of-conformity-draft") (store/declaration-history db)))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-2620 &middot; computer &amp; peripheral equipment manufacturing</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 1080px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Computer &amp; peripheral equipment manufacturing (ISIC 2620) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · shipment/Declaration-of-Conformity actuation always human</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Device units</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>deviceassembly.store</code> via <code>deviceassembly.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly. EMC-emission bounds, connector-mating peak insertion force (real <code>physics-2d</code>-simulated telemetry, ADR-2607991500), and Shipped/Declared are ground truth the governor independently re-derives — never trusted from a proposal's own report.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Device unit</th><th>Name</th><th>Jurisdiction</th><th>EMC emission dev.</th><th>Connector mass (kg)</th><th>Peak insertion force (N)</th><th>EOL</th><th>Robotics sim on file</th><th>Shipped / Declared</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     device-unit-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Assembly Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by any phase or human approval. Shipment and Declaration-of-Conformity actuation are always a human compliance engineer's call, at every phase.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft shipment / Declaration-of-Conformity records</h2>\n"
     "    <p class=\"muted\">Unsigned drafts (<code>deviceassembly.registry</code>) — the plant's own signature/submission is a separate, later act, never performed by this actor.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Kind</th><th>Device unit</th><th>Jurisdiction</th></tr></thead>\n"
     "      <tbody>\n"
     shipment-rows "\n"
     declaration-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/shipment-history db)) "shipment drafts,"
             (count (store/declaration-history db)) "declaration drafts )")))
