(ns deviceassembly.deviceassemblyadvisor
  "Device Assembly Advisor client -- the *contained intelligence node*
  for the device-assembly actor.

  It normalizes device-unit intake, drafts a per-jurisdiction EMC/
  product-safety compliance evidence checklist, screens device-units
  for an unresolved end-of-line-detected defect, drafts the burn-in-
  cell verification mission, drafts the device-unit-shipment action,
  and drafts the Declaration-of-Conformity-issuance action. CRITICAL:
  it is a smart-but-untrusted advisor. It returns a *proposal* (with a
  rationale + the fields it cited), never a committed record or a real
  robot shipment/Declaration-of-Conformity issuance. Every output is
  censored downstream by `deviceassembly.governor` before anything
  touches the SSoT, and `:actuation/ship-device-unit`/`:actuation/
  issue-declaration-of-conformity` proposals NEVER auto-commit at any
  phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/ship-device-unit | :actuation/issue-declaration-of-conformity | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [deviceassembly.facts :as facts]
            [deviceassembly.registry :as registry]
            [deviceassembly.robotics :as robotics]
            [deviceassembly.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the device-unit, EMC-deviation figures or
  jurisdiction. High confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "デバイスユニット記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :device-unit/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-requirements
  "Per-jurisdiction EMC/product-safety compliance evidence checklist
  draft. `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `deviceassembly.facts` -- the Assembly Governor must reject this
  (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [a (store/device-unit db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction a))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "deviceassembly.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :verification/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :verification/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-eol-defect
  "End-of-line-defect screening draft. `:eol-defect-unresolved?` on the
  device-unit record injects the failure mode: the Assembly Governor
  must HOLD, un-overridably, on any unresolved defect."
  [db {:keys [subject]}]
  (let [a (store/device-unit db subject)]
    (cond
      (nil? a)
      {:summary "対象デバイスユニット記録が見つかりません" :rationale "no device-unit record"
       :cites [] :effect :eol-screen/set :value {:device-unit-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:eol-defect-unresolved? a))
      {:summary    (str (:device-unit-name a) ": 未解決の完成検査欠陥を検出")
       :rationale  "完成検査スクリーニングが未解決の欠陥を検出。人手確認とホールドが必須。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:device-unit-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:device-unit-name a) ": 未解決の完成検査欠陥なし")
       :rationale  "完成検査欠陥スクリーニング完了。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:device-unit-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- simulate-burn-in-cell
  "Runs the robot burn-in-cell verification mission
  (`deviceassembly.robotics`) and drafts its result as a proposal. High
  confidence -- the mission itself is deterministic simulated telemetry
  derived from the device-unit's own recorded thermal-margin fields,
  not an LLM guess; the Assembly Governor still independently re-
  derives :passed? from those same fields before any `:actuation/
  ship-device-unit` proposal may commit -- see `deviceassembly.
  governor`'s `robotics-simulation-violations`."
  [db {:keys [subject]}]
  (let [a (store/device-unit db subject)]
    (if (nil? a)
      {:summary "対象デバイスユニット記録が見つかりません" :rationale "no device-unit record"
       :cites [] :effect :device-unit/upsert :value {:id subject :robotics-sim-verified? false}
       :stake nil :confidence 0.0}
      (let [{:keys [mission actions passed?]} (robotics/simulate-burn-in-cell subject a)]
        {:summary    (str subject ": バーンインセル検証ミッション " (if passed? "合格" "不合格"))
         :rationale  (str "mission=" (:mission/id mission) " actions=" (count actions)
                          " thermal-margin-deviation-actual=" (:thermal-margin-deviation-actual a))
         :cites      [(:mission/id mission)]
         :effect     :device-unit/upsert
         :value      {:id subject
                      :robotics-sim-verified? passed?
                      :robotics-sim-record {:mission-id (:mission/id mission)
                                            :actions (mapv #(dissoc % :action) actions)
                                            :passed? passed?}}
         :stake      nil
         :confidence 0.95}))))

(defn- propose-device-unit-shipment
  "Draft the actual DEVICE-UNIT-SHIPMENT action -- releasing a real
  finished device-unit for shipment. ALWAYS `:stake :actuation/ship-
  device-unit` -- this is a REAL-WORLD safety-critical act, never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`deviceassembly.phase`); the
  governor also always escalates on `:actuation/ship-device-unit`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [a (store/device-unit db subject)]
    {:summary    (str subject " 向け出荷提案"
                      (when a (str " (device-unit=" (:device-unit-name a) ")")))
     :rationale  (if a
                   (str "emc-emission-deviation-actual=" (:emc-emission-deviation-actual a)
                        " spec=[" (:emc-emission-deviation-min a) "," (:emc-emission-deviation-max a) "]")
                   "デバイスユニット記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :device-unit/mark-shipped
     :value      {:device-unit-id subject}
     :stake      :actuation/ship-device-unit
     :confidence (if (and a (not (registry/device-unit-emc-emission-out-of-range? a))) 0.9 0.3)}))

(defn- propose-declaration-of-conformity
  "Draft the actual DECLARATION-OF-CONFORMITY action -- issuing a real
  self-declaration of conformity certifying a device-unit as
  compliance-worthy. ALWAYS `:stake :actuation/issue-declaration-of-
  conformity` -- this is a REAL-WORLD safety-critical act, never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`deviceassembly.phase`); the
  governor also always escalates on `:actuation/issue-declaration-of-
  conformity`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [a (store/device-unit db subject)]
    {:summary    (str subject " 向け適合宣言発行提案"
                      (when a (str " (device-unit=" (:device-unit-name a) ")")))
     :rationale  (if a
                   "jurisdiction-evidence-checklist referenced"
                   "デバイスユニット記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :device-unit/mark-declared
     :value      {:device-unit-id subject}
     :stake      :actuation/issue-declaration-of-conformity
     :confidence (if a 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :device-unit/intake                          (normalize-intake db request)
    :compliance-rules/verify                     (verify-requirements db request)
    :end-of-line-quality/screen                  (screen-eol-defect db request)
    :robotics/simulate-burn-in-cell               (simulate-burn-in-cell db request)
    :actuation/ship-device-unit                  (propose-device-unit-shipment db request)
    :actuation/issue-declaration-of-conformity    (propose-declaration-of-conformity db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはデバイス組立工場の出荷・適合宣言発行エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:device-unit/upsert|:verification/set|:eol-screen/set|"
       ":device-unit/mark-shipped|:device-unit/mark-declared) "
       "(:robotics/simulate-burn-in-cell も :device-unit/upsert で "
       ":robotics-sim-verified? を提案する) "
       ":stake(:actuation/ship-device-unit か :actuation/issue-declaration-of-conformity か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :compliance-rules/verify                     {:device-unit (store/device-unit st subject)}
    :end-of-line-quality/screen                   {:device-unit (store/device-unit st subject)}
    :robotics/simulate-burn-in-cell                {:device-unit (store/device-unit st subject)}
    :actuation/ship-device-unit                   {:device-unit (store/device-unit st subject)}
    :actuation/issue-declaration-of-conformity     {:device-unit (store/device-unit st subject)}
    {:device-unit (store/device-unit st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Assembly Governor escalates/
  holds -- an LLM hiccup can never auto-ship a device-unit or
  auto-issue a Declaration of Conformity."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :deviceassemblyadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
