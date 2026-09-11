(ns deviceassembly.phase
  "Phase 0->3 staged rollout -- the device-assembly-plant analog of
  `cloud-itonami-isic-2910`'s `automotive.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- device-unit intake allowed, every
                                 write needs human approval.
    Phase 2  assisted-verify  -- adds compliance-rules requirements
                                 verification + end-of-line quality
                                 screening + robot burn-in-cell
                                 simulation writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:device-unit/intake` (no capital risk
                                 yet) may auto-commit. `:actuation/
                                 ship-device-unit`/`:actuation/issue-
                                 declaration-of-conformity` NEVER
                                 auto-commit, at any phase.

  `:actuation/ship-device-unit`/`:actuation/issue-declaration-of-
  conformity` are deliberately ABSENT from every phase's `:auto` set,
  including phase 3 -- a permanent structural fact, not a rollout
  milestone still to come. Shipping a real device-unit and issuing a
  real Declaration of Conformity are the two real-world legal acts
  this actor performs; both are always a human compliance engineer's
  call. `deviceassembly.governor`'s `:actuation/ship-device-unit`/
  `:actuation/issue-declaration-of-conformity` high-stakes gate
  enforces the same invariant independently -- two layers, not one,
  agree on this. `:end-of-line-quality/screen`/`:robotics/simulate-
  burn-in-cell` are likewise never auto-eligible, at any phase -- the
  same posture every sibling's screening/verification op has.
  Phase 3's `:auto` set here has only ONE member (`:device-unit/
  intake`) -- this domain has no separate no-capital-risk 'file'
  lifecycle distinct from the device-unit record itself.")

(def read-ops  #{})
(def write-ops #{:device-unit/intake :compliance-rules/verify :end-of-line-quality/screen
                 :robotics/simulate-burn-in-cell
                 :actuation/ship-device-unit :actuation/issue-declaration-of-conformity})

;; NOTE the invariant: `:actuation/ship-device-unit`/`:actuation/
;; issue-declaration-of-conformity` are members of `write-ops`
;; (governor-gated like any write) but are NEVER members of any
;; phase's `:auto` set below. Do not add them there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                          :auto #{}}
   1 {:label "assisted-intake"  :writes #{:device-unit/intake}                                       :auto #{}}
   2 {:label "assisted-verify"  :writes #{:device-unit/intake :compliance-rules/verify :end-of-line-quality/screen
                                          :robotics/simulate-burn-in-cell}                            :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:device-unit/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/ship-device-unit`/`:actuation/issue-declaration-of-
    conformity` are never auto-eligible at any phase, so they always
    escalate once the governor clears them (or hold if the governor
    doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map an Assembly Governor verdict to a base disposition before the
  phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
