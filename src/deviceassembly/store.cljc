(ns deviceassembly.store
  "SSoT for the device-assembly actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/deviceassembly/store_contract_test.clj), which is the whole
  point: the actor, the Assembly Governor and the audit ledger never
  know which SSoT they run on.

  Like `automotive.store`'s dual dispatch/conformity-certificate
  history and every other dual-actuation sibling before it, this actor
  has TWO actuation events (shipping a device-unit, issuing a
  Declaration of Conformity) acting on the SAME entity (a device-unit),
  each with its OWN history collection, sequence counter and dedicated
  double-actuation-guard boolean (`:device-unit-shipped?`/
  `:declaration-issued?`, never a `:status` value) -- the same
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320).

  The ledger stays append-only on every backend: 'which device-unit was
  screened for an unresolved end-of-line defect, which device-unit was
  shipped, which Declaration of Conformity was issued, on what
  jurisdictional basis, approved by whom' is always a query over an
  immutable log -- the audit trail a community trusting a device-
  assembly plant needs, and the evidence a manufacturer needs if a
  shipment or conformity decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [deviceassembly.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (device-unit [s id])
  (all-device-units [s])
  (eol-screen-of [s device-unit-id] "committed end-of-line-defect screening verdict for a device-unit, or nil")
  (requirements-verification-of [s device-unit-id] "committed compliance-rules requirements verification, or nil")
  (ledger [s])
  (shipment-history [s] "the append-only device-unit-shipment history (deviceassembly.registry drafts)")
  (declaration-history [s] "the append-only Declaration-of-Conformity history (deviceassembly.registry drafts)")
  (next-shipment-sequence [s jurisdiction] "next shipment-number sequence for a jurisdiction")
  (next-declaration-sequence [s jurisdiction] "next evidence-number sequence for a jurisdiction")
  (device-unit-already-shipped? [s device-unit-id] "has this device-unit already been shipped?")
  (device-unit-already-declared? [s device-unit-id] "has this device-unit's Declaration of Conformity already been issued?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-device-units [s device-units] "replace/seed the device-unit directory (map id->device-unit)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained device-unit set covering both actuation
  lifecycles (shipping a device-unit, issuing a Declaration of
  Conformity) so the actor + tests run offline."
  []
  {:device-units
   {"device-unit-1" {:id "device-unit-1" :device-unit-name "Sakura Notebook NB-14"
                      :emc-emission-deviation-actual 0.05 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                      :thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                      :eol-defect-unresolved? false
                      :robotics-sim-verified? false :robotics-sim-record nil
                      :device-unit-shipped? false :declaration-issued? false
                      :jurisdiction "JPN" :status :intake}
    "device-unit-2" {:id "device-unit-2" :device-unit-name "Atlantis Server Tower ST-2U"
                      :emc-emission-deviation-actual 0.05 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                      :thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                      :eol-defect-unresolved? false
                      :robotics-sim-verified? false :robotics-sim-record nil
                      :device-unit-shipped? false :declaration-issued? false
                      :jurisdiction "ATL" :status :intake}
    "device-unit-3" {:id "device-unit-3" :device-unit-name "鈴木デスクトップ DT-07"
                      :emc-emission-deviation-actual 0.35 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                      :thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                      :eol-defect-unresolved? false
                      :robotics-sim-verified? false :robotics-sim-record nil
                      :device-unit-shipped? false :declaration-issued? false
                      :jurisdiction "JPN" :status :intake}
    "device-unit-4" {:id "device-unit-4" :device-unit-name "田中モニタユニット MU-24"
                      :emc-emission-deviation-actual 0.05 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                      :thermal-margin-deviation-actual 0.02 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                      :eol-defect-unresolved? true
                      :robotics-sim-verified? false :robotics-sim-record nil
                      :device-unit-shipped? false :declaration-issued? false
                      :jurisdiction "JPN" :status :intake}
    "device-unit-5" {:id "device-unit-5" :device-unit-name "佐藤オールインワンPC AiO-09"
                      :emc-emission-deviation-actual 0.05 :emc-emission-deviation-min -0.10 :emc-emission-deviation-max 0.10
                      :thermal-margin-deviation-actual 0.30 :thermal-margin-deviation-min -0.05 :thermal-margin-deviation-max 0.05
                      :eol-defect-unresolved? false
                      :robotics-sim-verified? true :robotics-sim-record nil
                      :device-unit-shipped? false :declaration-issued? false
                      :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- ship-device-unit!
  "Backend-agnostic `:device-unit/mark-shipped` -- looks up the
  device-unit via the protocol and drafts the device-unit-shipment
  record, and returns {:result .. :device-unit-patch ..} for the
  caller to persist."
  [s device-unit-id]
  (let [a (device-unit s device-unit-id)
        seq-n (next-shipment-sequence s (:jurisdiction a))
        result (registry/register-device-unit-shipment device-unit-id (:jurisdiction a) seq-n)]
    {:result result
     :device-unit-patch {:device-unit-shipped? true
                          :shipment-number (get result "shipment_number")}}))

(defn- issue-declaration-of-conformity!
  "Backend-agnostic `:device-unit/mark-declared` -- looks up the
  device-unit via the protocol and drafts the Declaration-of-Conformity
  record, and returns {:result .. :device-unit-patch ..} for the
  caller to persist."
  [s device-unit-id]
  (let [a (device-unit s device-unit-id)
        seq-n (next-declaration-sequence s (:jurisdiction a))
        result (registry/register-declaration-of-conformity device-unit-id (:jurisdiction a) seq-n)]
    {:result result
     :device-unit-patch {:declaration-issued? true
                          :evidence-number (get result "evidence_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (device-unit [_ id] (get-in @a [:device-units id]))
  (all-device-units [_] (sort-by :id (vals (:device-units @a))))
  (eol-screen-of [_ id] (get-in @a [:eol-screens id]))
  (requirements-verification-of [_ device-unit-id] (get-in @a [:verifications device-unit-id]))
  (ledger [_] (:ledger @a))
  (shipment-history [_] (:shipments @a))
  (declaration-history [_] (:declarations @a))
  (next-shipment-sequence [_ jurisdiction] (get-in @a [:shipment-sequences jurisdiction] 0))
  (next-declaration-sequence [_ jurisdiction] (get-in @a [:declaration-sequences jurisdiction] 0))
  (device-unit-already-shipped? [_ device-unit-id] (boolean (get-in @a [:device-units device-unit-id :device-unit-shipped?])))
  (device-unit-already-declared? [_ device-unit-id] (boolean (get-in @a [:device-units device-unit-id :declaration-issued?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :device-unit/upsert
      (swap! a update-in [:device-units (:id value)] merge value)

      :verification/set
      (swap! a assoc-in [:verifications (first path)] payload)

      :eol-screen/set
      (swap! a assoc-in [:eol-screens (first path)] payload)

      :device-unit/mark-shipped
      (let [device-unit-id (first path)
            {:keys [result device-unit-patch]} (ship-device-unit! s device-unit-id)
            jurisdiction (:jurisdiction (device-unit s device-unit-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:shipment-sequences jurisdiction] (fnil inc 0))
                       (update-in [:device-units device-unit-id] merge device-unit-patch)
                       (update :shipments registry/append result))))
        result)

      :device-unit/mark-declared
      (let [device-unit-id (first path)
            {:keys [result device-unit-patch]} (issue-declaration-of-conformity! s device-unit-id)
            jurisdiction (:jurisdiction (device-unit s device-unit-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:declaration-sequences jurisdiction] (fnil inc 0))
                       (update-in [:device-units device-unit-id] merge device-unit-patch)
                       (update :declarations registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-device-units [s device-units] (when (seq device-units) (swap! a assoc :device-units device-units)) s))

(defn seed-db
  "A MemStore seeded with the demo device-unit set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :verifications {} :eol-screens {} :ledger [] :shipment-sequences {}
                           :shipments [] :declaration-sequences {} :declarations []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (verification/eol-screen payloads, ledger facts,
  shipment/declaration records) are stored as EDN strings so `langchain.
  db` doesn't expand them into sub-entities -- the same convention
  every sibling actor's store uses."
  {:device-unit/id                    {:db/unique :db.unique/identity}
   :verification/device-unit-id       {:db/unique :db.unique/identity}
   :eol-screen/device-unit-id         {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :shipment/seq                      {:db/unique :db.unique/identity}
   :declaration/seq                   {:db/unique :db.unique/identity}
   :shipment-sequence/jurisdiction    {:db/unique :db.unique/identity}
   :declaration-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- device-unit->tx [{:keys [id device-unit-name emc-emission-deviation-actual emc-emission-deviation-min emc-emission-deviation-max
                                 thermal-margin-deviation-actual thermal-margin-deviation-min thermal-margin-deviation-max
                                 eol-defect-unresolved? robotics-sim-verified? robotics-sim-record
                                 device-unit-shipped? declaration-issued?
                                 jurisdiction status shipment-number evidence-number]}]
  (cond-> {:device-unit/id id}
    device-unit-name                            (assoc :device-unit/device-unit-name device-unit-name)
    emc-emission-deviation-actual                (assoc :device-unit/emc-emission-deviation-actual emc-emission-deviation-actual)
    emc-emission-deviation-min                   (assoc :device-unit/emc-emission-deviation-min emc-emission-deviation-min)
    emc-emission-deviation-max                   (assoc :device-unit/emc-emission-deviation-max emc-emission-deviation-max)
    thermal-margin-deviation-actual              (assoc :device-unit/thermal-margin-deviation-actual thermal-margin-deviation-actual)
    thermal-margin-deviation-min                 (assoc :device-unit/thermal-margin-deviation-min thermal-margin-deviation-min)
    thermal-margin-deviation-max                 (assoc :device-unit/thermal-margin-deviation-max thermal-margin-deviation-max)
    (some? eol-defect-unresolved?)               (assoc :device-unit/eol-defect-unresolved? eol-defect-unresolved?)
    (some? robotics-sim-verified?)                (assoc :device-unit/robotics-sim-verified? robotics-sim-verified?)
    (some? robotics-sim-record)                   (assoc :device-unit/robotics-sim-record (enc robotics-sim-record))
    (some? device-unit-shipped?)                 (assoc :device-unit/device-unit-shipped? device-unit-shipped?)
    (some? declaration-issued?)                  (assoc :device-unit/declaration-issued? declaration-issued?)
    jurisdiction                                 (assoc :device-unit/jurisdiction jurisdiction)
    status                                       (assoc :device-unit/status status)
    shipment-number                              (assoc :device-unit/shipment-number shipment-number)
    evidence-number                              (assoc :device-unit/evidence-number evidence-number)))

(def ^:private device-unit-pull
  [:device-unit/id :device-unit/device-unit-name :device-unit/emc-emission-deviation-actual
   :device-unit/emc-emission-deviation-min :device-unit/emc-emission-deviation-max
   :device-unit/thermal-margin-deviation-actual :device-unit/thermal-margin-deviation-min :device-unit/thermal-margin-deviation-max
   :device-unit/eol-defect-unresolved? :device-unit/robotics-sim-verified? :device-unit/robotics-sim-record
   :device-unit/device-unit-shipped? :device-unit/declaration-issued?
   :device-unit/jurisdiction :device-unit/status :device-unit/shipment-number :device-unit/evidence-number])

(defn- pull->device-unit [m]
  (when (:device-unit/id m)
    {:id (:device-unit/id m) :device-unit-name (:device-unit/device-unit-name m)
     :emc-emission-deviation-actual (:device-unit/emc-emission-deviation-actual m)
     :emc-emission-deviation-min (:device-unit/emc-emission-deviation-min m)
     :emc-emission-deviation-max (:device-unit/emc-emission-deviation-max m)
     :thermal-margin-deviation-actual (:device-unit/thermal-margin-deviation-actual m)
     :thermal-margin-deviation-min (:device-unit/thermal-margin-deviation-min m)
     :thermal-margin-deviation-max (:device-unit/thermal-margin-deviation-max m)
     :eol-defect-unresolved? (boolean (:device-unit/eol-defect-unresolved? m))
     :robotics-sim-verified? (boolean (:device-unit/robotics-sim-verified? m))
     :robotics-sim-record (dec* (:device-unit/robotics-sim-record m))
     :device-unit-shipped? (boolean (:device-unit/device-unit-shipped? m))
     :declaration-issued? (boolean (:device-unit/declaration-issued? m))
     :jurisdiction (:device-unit/jurisdiction m) :status (:device-unit/status m)
     :shipment-number (:device-unit/shipment-number m) :evidence-number (:device-unit/evidence-number m)}))

(defrecord DatomicStore [conn]
  Store
  (device-unit [_ id]
    (pull->device-unit (d/pull (d/db conn) device-unit-pull [:device-unit/id id])))
  (all-device-units [_]
    (->> (d/q '[:find [?id ...] :where [?e :device-unit/id ?id]] (d/db conn))
         (map #(pull->device-unit (d/pull (d/db conn) device-unit-pull [:device-unit/id %])))
         (sort-by :id)))
  (eol-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?k :eol-screen/device-unit-id ?aid] [?k :eol-screen/payload ?p]]
              (d/db conn) id)))
  (requirements-verification-of [_ device-unit-id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?a :verification/device-unit-id ?aid] [?a :verification/payload ?p]]
              (d/db conn) device-unit-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (shipment-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :shipment/seq ?s] [?e :shipment/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (declaration-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :declaration/seq ?s] [?e :declaration/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-shipment-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :shipment-sequence/jurisdiction ?j] [?e :shipment-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-declaration-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :declaration-sequence/jurisdiction ?j] [?e :declaration-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (device-unit-already-shipped? [s device-unit-id]
    (boolean (:device-unit-shipped? (device-unit s device-unit-id))))
  (device-unit-already-declared? [s device-unit-id]
    (boolean (:declaration-issued? (device-unit s device-unit-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :device-unit/upsert
      (d/transact! conn [(device-unit->tx value)])

      :verification/set
      (d/transact! conn [{:verification/device-unit-id (first path) :verification/payload (enc payload)}])

      :eol-screen/set
      (d/transact! conn [{:eol-screen/device-unit-id (first path) :eol-screen/payload (enc payload)}])

      :device-unit/mark-shipped
      (let [device-unit-id (first path)
            {:keys [result device-unit-patch]} (ship-device-unit! s device-unit-id)
            jurisdiction (:jurisdiction (device-unit s device-unit-id))
            next-n (inc (next-shipment-sequence s jurisdiction))]
        (d/transact! conn
                     [(device-unit->tx (assoc device-unit-patch :id device-unit-id))
                      {:shipment-sequence/jurisdiction jurisdiction :shipment-sequence/next next-n}
                      {:shipment/seq (count (shipment-history s)) :shipment/record (enc (get result "record"))}])
        result)

      :device-unit/mark-declared
      (let [device-unit-id (first path)
            {:keys [result device-unit-patch]} (issue-declaration-of-conformity! s device-unit-id)
            jurisdiction (:jurisdiction (device-unit s device-unit-id))
            next-n (inc (next-declaration-sequence s jurisdiction))]
        (d/transact! conn
                     [(device-unit->tx (assoc device-unit-patch :id device-unit-id))
                      {:declaration-sequence/jurisdiction jurisdiction :declaration-sequence/next next-n}
                      {:declaration/seq (count (declaration-history s)) :declaration/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-device-units [s device-units]
    (when (seq device-units) (d/transact! conn (mapv device-unit->tx (vals device-units)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:device-units ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [device-units]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-device-units s device-units))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo device-unit set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
