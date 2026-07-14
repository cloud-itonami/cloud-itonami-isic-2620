(ns deviceassembly.facts
  "Per-jurisdiction computer/peripheral-equipment EMC and product-safety
  compliance catalog -- the G2-style spec-basis table the Assembly
  Governor checks every `:compliance-rules/verify` proposal against.

  Coverage is reported HONESTLY: a jurisdiction not in this table has
  NO spec-basis. Seed values cite official EMC/product-safety
  regulators and self-declaration regimes for IT equipment; this is a
  starting catalog, not a survey of every market -- the same honest,
  non-fabricating discipline `automotive.facts` (cloud-itonami-isic-2910)
  established for vehicle type-approval.")

(def catalog
  {"JPN" {:name "Japan"
          :owner-authority "一般財団法人VCCI協会 (VCCI Council, Voluntary Control Council for Interference by Information Technology Equipment) / 経済産業省 (METI) 電気用品安全法 (PSE)"
          :legal-basis "電気用品安全法 (Electrical Appliance and Material Safety Law, PSE) / VCCI協会技術基準 (参考、自主規制)"
          :national-spec "VCCI自主規制によるEMC適合確認 + PSE表示 (電気用品安全法の技術基準)"
          :provenance "https://www.vcci.jp/"
          :required-evidence ["EMC試験報告書 (EMC-test-report)"
                              "PSE適合性確認記録 (PSE-conformity-record)"
                              "IEC-62368-1安全性試験報告書 (IEC-62368-1-safety-test-report)"
                              "完成検査連鎖記録 (end-of-line-quality-chain-of-custody-record)"]}
   "USA" {:name "United States"
          :owner-authority "FCC (Federal Communications Commission), Office of Engineering and Technology"
          :legal-basis "47 CFR Part 15, Subpart B (Unintentional Radiators) -- Supplier's Declaration of Conformity (SDoC) self-certification (reference)"
          :national-spec "US FCC Part 15 unintentional-radiator self-certification for digital devices"
          :provenance "https://www.fcc.gov/oet/ea/fccid"
          :required-evidence ["FCC-Part-15-EMC-test-report"
                              "IEC-62368-1-safety-test-report"
                              "end-of-line-quality-chain-of-custody-record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "OPSS (Office for Product Safety and Standards) / UKCA marking"
          :legal-basis "Electromagnetic Compatibility Regulations 2016 (retained EU law) -- UKCA self-declaration (reference)"
          :national-spec "UK UKCA-marking conformity via manufacturer self-declaration (Declaration of Conformity)"
          :provenance "https://www.gov.uk/guidance/using-the-ukca-marking"
          :required-evidence ["EMC-test-report"
                              "IEC-62368-1-safety-test-report"
                              "end-of-line-quality-chain-of-custody-record"]}
   "DEU" {:name "Germany (EU CE marking)"
          :owner-authority "Bundesnetzagentur (EMC-Marktüberwachung) / EU CE-Kennzeichnung (Herstellerselbsterklärung)"
          :legal-basis "EMV-Richtlinie 2014/30/EU + RoHS-Richtlinie 2011/65/EU (Referenz)"
          :national-spec "EU CE-Kennzeichnung Konformitätserklärung (Declaration of Conformity) für IT-Geräte"
          :provenance "https://www.bundesnetzagentur.de/"
          :required-evidence ["EMC-Prüfbericht (EMC-test-report)"
                              "RoHS-Werkstofferklärung (RoHS-material-declaration)"
                              "IEC-62368-1-Sicherheitsprüfbericht (IEC-62368-1-safety-test-report)"
                              "Endkontroll-Rückverfolgbarkeitsnachweis (end-of-line-quality-chain-of-custody-record)"]}})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2620 R0: " (count catalog)
                 " jurisdictions seeded. Extend `deviceassembly.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
