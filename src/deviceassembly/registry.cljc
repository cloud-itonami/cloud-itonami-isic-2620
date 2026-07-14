(ns deviceassembly.registry
  "Pure-function device-unit-shipment + Declaration-of-Conformity record
  construction -- an append-only device-assembly-plant book-of-record
  draft.

  Like every sibling actor's registry, there is no single international
  check-digit standard for a shipment or Declaration-of-Conformity
  reference number -- every manufacturer/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `deviceassembly.facts` uses.

  `device-unit-emc-emission-out-of-range?` is a further instance of this
  fleet's two-sided range check family (`testlab.registry/within-
  tolerance?` established the first, `conservation.registry/body-
  condition-out-of-range?` the second, `water.registry/contaminant-
  level-out-of-range?` the third, `steelworks.registry/heat-chemistry-
  out-of-range?`/`turbine.registry/unit-tolerance-out-of-range?` further
  siblings, `automotive.registry/vehicle-emissions-out-of-range?` the
  fourth -- cloud-itonami-isic-2910), applying the SAME lo/hi bounds-
  comparison shape to a device-unit's own measured EMC emission-level
  deviation against the device-unit's own recorded regulatory-limit
  bounds.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real MES / assembly-line control system. It builds the
  RECORD a manufacturer would keep, not the act of shipping the
  device-unit or issuing the Declaration of Conformity itself (that is
  `deviceassembly.operation`'s `:actuation/ship-device-unit`/
  `:actuation/issue-declaration-of-conformity`, always human-gated --
  see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  manufacturer's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn device-unit-emc-emission-out-of-range?
  "Does `device-unit`'s own `:emc-emission-deviation-actual` fall
  outside its own `[:emc-emission-deviation-min :emc-emission-
  deviation-max]` recorded regulatory-limit bounds (dB relative to the
  jurisdiction's emission limit line -- positive means the reading
  exceeds the limit)? A pure ground-truth check against the device-
  unit's own permanent fields -- no upstream comparison needed. One of
  this fleet's two-sided range check family (see ns docstring)."
  [{:keys [emc-emission-deviation-actual emc-emission-deviation-min emc-emission-deviation-max]}]
  (and (number? emc-emission-deviation-actual) (number? emc-emission-deviation-min) (number? emc-emission-deviation-max)
       (or (< emc-emission-deviation-actual emc-emission-deviation-min)
           (> emc-emission-deviation-actual emc-emission-deviation-max))))

(defn register-device-unit-shipment
  "Validate + construct the DEVICE-UNIT-SHIPMENT registration DRAFT --
  the manufacturer's own act of releasing a real finished device-unit
  for shipment. Pure function -- does not touch any real MES/assembly-
  line control system; it builds the RECORD a manufacturer would keep.
  `deviceassembly.governor` independently re-verifies the device-unit's
  own EMC-emission sufficiency against its own spec bounds, and a
  double-shipment for the same device-unit, before this is ever allowed
  to commit."
  [device-unit-id jurisdiction sequence]
  (when-not (and device-unit-id (not= device-unit-id ""))
    (throw (ex-info "device-unit-shipment: device_unit_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "device-unit-shipment: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "device-unit-shipment: sequence must be >= 0" {})))
  (let [shipment-number (str (str/upper-case jurisdiction) "-SHP-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "device-unit-shipment-draft"
                "device_unit_id" device-unit-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "DeviceUnitShipment" shipment-number shipment-number)}))

(defn register-declaration-of-conformity
  "Validate + construct the DECLARATION-OF-CONFORMITY registration
  DRAFT -- the manufacturer's own act of issuing a real CE-DoC/FCC-SDoC-
  style self-declaration of conformity for a device-unit. Pure function
  -- does not touch any real MES/assembly-line control system; it
  builds the RECORD a manufacturer would keep. `deviceassembly.
  governor` independently re-verifies the device-unit's own end-of-line
  defect resolution status, and a double-issuance for the same device-
  unit, before this is ever allowed to commit."
  [device-unit-id jurisdiction sequence]
  (when-not (and device-unit-id (not= device-unit-id ""))
    (throw (ex-info "declaration-of-conformity: device_unit_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "declaration-of-conformity: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "declaration-of-conformity: sequence must be >= 0" {})))
  (let [evidence-number (str (str/upper-case jurisdiction) "-DOC-" (zero-pad sequence 6))
        record {"record_id" evidence-number
                "kind" "declaration-of-conformity-draft"
                "device_unit_id" device-unit-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "evidence_number" evidence-number
     "certificate" (unsigned-certificate "DeclarationOfConformity" evidence-number evidence-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
