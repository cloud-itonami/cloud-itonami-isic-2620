(ns deviceassembly.governor
  "Assembly Governor -- the independent compliance layer that earns the
  Device Assembly Advisor the right to commit. The LLM has no notion of
  EMC/product-safety compliance law, whether a device-unit's own
  measured EMC emission deviation actually stays within its own
  recorded regulatory bounds, whether an end-of-line-detected defect
  against the device-unit has actually stayed unresolved, or when an
  act stops being a draft and becomes a real-world device-unit shipment
  or Declaration-of-Conformity issuance, so this MUST be a separate
  system able to *reject* a proposal and fall back to HOLD -- the
  device-assembly-plant analog of `cloud-itonami-isic-2910`'s
  Automotive Governor.

  Five checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated compliance spec-basis, incomplete evidence, a robot burn-
  in/EMC-pre-scan mission that never ran or that independently
  re-checks out-of-tolerance, an out-of-spec device-unit, or an
  unresolved end-of-line defect). Two further guards (double-shipment,
  double-declaration) are enforced but not numbered below since they
  need no upstream comparison at all. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `deviceassembly.phase`: for `:stake
  :actuation/ship-device-unit`/`:actuation/issue-declaration-of-
  conformity` (a real safety-critical act) NO phase ever allows
  auto-commit either. Two independent layers agree that actuation is
  always a human call.

    1. Spec-basis                  -- did the requirements proposal cite
                                       an OFFICIAL source (`deviceassembly.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/ship-device-unit`/
                                       `:actuation/issue-declaration-of-
                                       conformity`, has the device-unit
                                       actually been verified with a
                                       full EMC-test-report/safety-test-
                                       report/end-of-line-quality-
                                       chain-of-custody-record evidence
                                       checklist on file?
    3. Robot simulation missing or
       independently out-of-
       tolerance                    -- for `:actuation/ship-device-
                                       unit`, has the robot burn-in-cell
                                       verification mission
                                       (`deviceassembly.robotics`)
                                       actually run and been recorded
                                       on the device-unit
                                       (`:robotics-sim-verified?`)? AND
                                       INDEPENDENTLY recompute whether
                                       the device-unit's own recorded
                                       thermal-margin reading falls out
                                       of its own recorded tolerance
                                       bounds (`deviceassembly.robotics/
                                       simulation-out-of-tolerance?`),
                                       ignoring whatever :passed?
                                       verdict the mission run itself
                                       stored -- the same 'ground
                                       truth, not self-report'
                                       discipline check 4 below uses
                                       for EMC emissions. ADR-2607991500
                                       ADDS a second independent
                                       recheck alongside this one (never
                                       replacing it): whether the
                                       device-unit's own recorded REAL
                                       `physics-2d`-simulated connector
                                       mating/insertion-force telemetry
                                       (`:sim-peak-insertion-force-n`)
                                       exceeds its real disclosed
                                       ceiling (`deviceassembly.
                                       robotics/connector-mating-force-
                                       out-of-tolerance?`) -- the SAME
                                       'ground truth, not self-report'
                                       discipline, an unrelated QA
                                       domain (connector mechanical
                                       mating, not thermal/EMC) folded
                                       into the SAME robotics-simulation
                                       HARD check.
    4. Device-unit EMC emission out
       of range                      -- for `:actuation/ship-device-
                                       unit`, INDEPENDENTLY recompute
                                       whether the device-unit's own
                                       measured EMC emission deviation
                                       falls outside its own recorded
                                       regulatory-limit bounds
                                       (`deviceassembly.registry/
                                       device-unit-emc-emission-out-of-
                                       range?`) -- needs no proposal
                                       inspection or stored-verdict
                                       lookup at all. Another instance
                                       of this fleet's two-sided range
                                       check family (`testlab.governor/
                                       within-tolerance-violations`/
                                       `conservation.governor/body-
                                       condition-out-of-range-
                                       violations`/`water.governor/
                                       contaminant-level-out-of-range-
                                       violations`/`steelworks.
                                       governor`/`turbine.governor`/
                                       `automotive.governor` established
                                       the priors).
    5. End-of-line defect unresolved -- reported by THIS proposal itself
                                       (an `:end-of-line-quality/
                                       screen` that just found an
                                       unresolved defect), or
                                       already on file for the
                                       device-unit (`:end-of-line-
                                       quality/screen`/`:actuation/
                                       issue-declaration-of-
                                       conformity`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/`automotive.
                                       governor/end-of-line-defect-
                                       unresolved-violations`...(prior
                                       siblings)... established --
                                       exercised in tests/demo via
                                       `:end-of-line-quality/screen`
                                       DIRECTLY, not via an actuation
                                       op against an unscreened
                                       device-unit -- see this ns's own
                                       test suite.

  Two more guards, double-shipment/double-declaration prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-shipped-violations`/
  `already-declared-violations` refuse to ship a device-unit / issue a
  Declaration of Conformity for the SAME device-unit twice, off
  dedicated `:device-unit-shipped?`/`:declaration-issued?` facts (never
  a `:status` value) -- the SAME 'check a dedicated boolean, not
  status' discipline every prior sibling governor's guards establish,
  informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [deviceassembly.facts :as facts]
            [deviceassembly.registry :as registry]
            [deviceassembly.robotics :as robotics]
            [deviceassembly.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Shipping a real device-unit and issuing a real Declaration of
  Conformity are the two real-world actuation events this actor
  performs -- a two-member set, matching every prior dual-actuation
  sibling's shape."
  #{:actuation/ship-device-unit :actuation/issue-declaration-of-conformity})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance-rules/verify` (or actuation) proposal with no
  spec-basis citation is a HARD violation -- never invent a
  jurisdiction's compliance requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance-rules/verify :actuation/ship-device-unit :actuation/issue-declaration-of-conformity} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は適合要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/ship-device-unit`/`:actuation/issue-declaration-of-
  conformity`, the jurisdiction's required EMC-test-report/safety-
  test-report/end-of-line-quality-chain-of-custody-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/ship-device-unit :actuation/issue-declaration-of-conformity} op)
    (let [a (store/device-unit st subject)
          verification (store/requirements-verification-of st subject)]
      (when-not (and verification
                     (facts/required-evidence-satisfied?
                      (:jurisdiction a) (:checklist verification)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(EMC試験報告書/安全性試験報告書/完成検査連鎖記録等)が充足していない状態での提案"}]))))

(defn- robotics-simulation-violations
  "For `:actuation/ship-device-unit`: HARD hold if the robot burn-in-
  cell verification mission (`deviceassembly.robotics`) never ran and
  was recorded on the device-unit (`:robotics-sim-verified?`), OR if it
  did but an INDEPENDENT recompute of the device-unit's own thermal-
  margin fields (`deviceassembly.robotics/simulation-out-of-
  tolerance?`) says out-of-tolerance right now, OR (ADR-2607991500,
  ADDED alongside the thermal-margin recheck, never replacing it) an
  INDEPENDENT recompute of the device-unit's own recorded REAL
  `physics-2d`-simulated connector mating/insertion-force telemetry
  (`deviceassembly.robotics/connector-mating-force-out-of-tolerance?`)
  says out-of-tolerance right now -- never trusts the mission's own
  stored :passed? verdict alone for either check, the same discipline
  `device-unit-emc-emission-out-of-range-violations` below uses for
  EMC emissions."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-device-unit)
    (let [a (store/device-unit st subject)]
      (cond
        (not (:robotics-sim-verified? a))
        [{:rule :robotics-simulation-missing
          :detail (str subject " のバーンインセル検証ミッションが未実行・未合格")}]

        (robotics/simulation-out-of-tolerance? a)
        [{:rule :robotics-simulation-out-of-tolerance
          :detail (str subject " の熱マージン実測値("
                       (:thermal-margin-deviation-actual a) ")が独立再検証で許容範囲["
                       (:thermal-margin-deviation-min a) "," (:thermal-margin-deviation-max a) "]を逸脱")}]

        (robotics/connector-mating-force-out-of-tolerance? a)
        [{:rule :connector-mating-force-out-of-tolerance
          :detail (str subject " の実測コネクタ嵌合力(" (:sim-peak-insertion-force-n a)
                       "N)が独立再検証で許容上限(" robotics/max-insertion-force-n "N)を超過")}]))))

(defn- device-unit-emc-emission-out-of-range-violations
  "For `:actuation/ship-device-unit`, INDEPENDENTLY recompute whether
  the device-unit's own EMC emission deviation falls outside its own
  recorded regulatory-limit bounds via `deviceassembly.registry/
  device-unit-emc-emission-out-of-range?` -- needs no proposal
  inspection or stored-verdict lookup at all, since its inputs are
  permanent ground-truth fields already on the device-unit."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-device-unit)
    (let [a (store/device-unit st subject)]
      (when (registry/device-unit-emc-emission-out-of-range? a)
        [{:rule :device-unit-emc-emission-out-of-range
          :detail (str subject " の実測EMC放射偏差(" (:emc-emission-deviation-actual a)
                      ")が規制限度[" (:emc-emission-deviation-min a) "," (:emc-emission-deviation-max a) "]を逸脱")}]))))

(defn- end-of-line-defect-unresolved-violations
  "An unresolved end-of-line-detected defect -- reported by THIS
  proposal (e.g. an `:end-of-line-quality/screen` that itself just
  found one), or already on file in the store for the device-unit
  (`:end-of-line-quality/screen`/`:actuation/issue-declaration-of-
  conformity`) -- is a HARD, un-overridable hold. Evaluated
  UNCONDITIONALLY (not scoped to a specific op) so the screening op
  itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        device-unit-id (when (contains? #{:end-of-line-quality/screen :actuation/issue-declaration-of-conformity} op) subject)
        hit-on-file? (and device-unit-id (= :unresolved (:verdict (store/eol-screen-of st device-unit-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :end-of-line-defect-unresolved
        :detail "未解決の完成検査欠陥がある状態での適合宣言発行提案は進められない"}])))

(defn- already-shipped-violations
  "For `:actuation/ship-device-unit`, refuses to ship the SAME
  device-unit twice, off a dedicated `:device-unit-shipped?` fact
  (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-device-unit)
    (when (store/device-unit-already-shipped? st subject)
      [{:rule :already-shipped
        :detail (str subject " は既に出荷済み")}])))

(defn- already-declared-violations
  "For `:actuation/issue-declaration-of-conformity`, refuses to issue a
  Declaration of Conformity for the SAME device-unit twice, off a
  dedicated `:declaration-issued?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-declaration-of-conformity)
    (when (store/device-unit-already-declared? st subject)
      [{:rule :already-declared
        :detail (str subject " は既に適合宣言発行済み")}])))

(defn check
  "Censors a Device Assembly Advisor proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c :escalate?
  bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (robotics-simulation-violations request st)
                           (device-unit-emc-emission-out-of-range-violations request st)
                           (end-of-line-defect-unresolved-violations request proposal st)
                           (already-shipped-violations request st)
                           (already-declared-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
