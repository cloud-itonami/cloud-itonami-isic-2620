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
                              "Endkontroll-Rückverfolgbarkeitsnachweis (end-of-line-quality-chain-of-custody-record)"]}
   ;; ITA gap disclosure (honest, non-fabrication discipline per the ns
   ;; docstring above): every decree number/date/title below was fetched
   ;; and read directly from normattiva.it this session (Italy's official
   ;; legislative-text portal) -- not recalled from training data. Two
   ;; things could NOT be independently confirmed and are deliberately
   ;; left out rather than guessed: (i) the exact vigilanza
   ;; (market-surveillance) authority split for RoHS enforcement across
   ;; MASE / MIMIT / regions (D.Lgs. 27/2014 Capo IV, artt. 19-21) --
   ;; normattiva.it only serves the preamble + Art. 1 of each atto
   ;; through the plain uri-res permalink; its per-article
   ;; `caricaArticolo` AJAX endpoint 500'd when fetched directly outside
   ;; its own session flow, so :owner-authority below only claims what
   ;; the fetched preamble text itself supports (MASE as the decree's
   ;; proposing ministry) and does NOT name a specific market-surveillance
   ;; body; (ii) any specific "Registro Nazionale AEE" producer-registry
   ;; name/number -- :national-spec below cites only the "Sistemi
   ;; Collettivi" / CdC RAEE producer-compliance mechanism that MASE's own
   ;; mase.gov.it news page (fetched this session, dated 15 luglio 2026)
   ;; names directly.
   "ITA" {:name "Italy (EU CE marking)"
          :owner-authority "Ministero dell'Ambiente e della Sicurezza Energetica (MASE -- proposing ministry for the RoHS and RAEE/WEEE decreti legislativi below) + Centro di Coordinamento RAEE (CdC RAEE -- operational coordinator of the Sistemi Collettivi producer-compliance schemes named on mase.gov.it) / EU CE marking (Dichiarazione di Conformità del fabbricante, manufacturer self-declaration)"
          :legal-basis "D.Lgs. 6 novembre 2007, n. 194 (attuazione direttiva EMC 2004/108/CE; testo vigente aggiornato -- 'Ultimo aggiornamento all'atto pubblicato il 25/05/2016' -- dal D.Lgs. 18 maggio 2016, n. 80 per recepire la direttiva EMC 2014/30/UE, rifusione) + D.Lgs. 4 marzo 2014, n. 27 (attuazione della direttiva RoHS 2011/65/UE) + D.Lgs. 14 marzo 2014, n. 49 (attuazione della direttiva WEEE 2012/19/UE sui rifiuti di apparecchiature elettriche ed elettroniche -- RAEE) -- tutti e tre confermati vigenti su normattiva.it in questa sessione (Referenz, non un’estrazione articolo-per-articolo)"
          :national-spec "EU CE-marking Dichiarazione di Conformità per apparecchiature IT/AEE (autodichiarazione del fabbricante) + restrizione RoHS delle sostanze pericolose ai sensi del D.Lgs. 27/2014 + responsabilità estesa del produttore per i RAEE tramite i Sistemi Collettivi coordinati dal CdC RAEE ai sensi del D.Lgs. 49/2014"
          :provenance "https://www.normattiva.it/uri-res/N2Ls?urn:nir:stato:decreto.legislativo:2007-11-06;194 (EMC, D.Lgs. 194/2007 as amended by D.Lgs. 80/2016) ; https://www.normattiva.it/uri-res/N2Ls?urn:nir:stato:decreto.legislativo:2014-03-04;27 (RoHS, D.Lgs. 27/2014) ; https://www.normattiva.it/uri-res/N2Ls?urn:nir:stato:decreto.legislativo:2014-03-14;49 (WEEE/RAEE, D.Lgs. 49/2014) ; https://www.mase.gov.it/portale/web/guest/-/mase-e-cdc-raee-siglano-un-accordo-per-potenziare-la-raccolta-dei-raee-in-istituzioni-e-aziende (MASE/CdC RAEE, Sistemi Collettivi)"
          :required-evidence ["EMC-test-report (rapporto di prova EMC ai sensi del D.Lgs. 194/2007 come aggiornato dal D.Lgs. 80/2016)"
                              "RoHS-material-declaration (dichiarazione di conformità RoHS ai sensi del D.Lgs. 27/2014)"
                              "IEC-62368-1-safety-test-report"
                              "RAEE-producer-compliance-record (adesione a un Sistema Collettivo RAEE coordinato dal CdC RAEE ai sensi del D.Lgs. 49/2014)"
                              "end-of-line-quality-chain-of-custody-record"]}
   ;; CAN gap disclosure (honest, non-fabrication discipline per the ns
   ;; docstring above): every quote below was fetched and read directly
   ;; this session from ised-isde.canada.ca (ISED's own site) and
   ;; laws-lois.justice.gc.ca (Canada's official consolidated-statutes
   ;; portal) -- not recalled from training data. The ICES-003 (Issue 7,
   ;; October 2020) and ICES-Gen (Issue 2, February 23 2024) PDFs were
   ;; fetched and read page-by-page directly (not just summarized);
   ;; ICES-Gen s.3.3 is the source for the Category II / Supplier's
   ;; Declaration of Conformity (SDoC) self-certification characterization
   ;; below ("Category II equipment is exempt from certification and
   ;; registration. The label placed on each unit of the interference-
   ;; causing equipment model, according to the applicable ICES standard,
   ;; represents the SDoC with ISED requirements."). One thing could NOT
   ;; be independently confirmed and is deliberately left out rather than
   ;; guessed: the specific Canadian national safety-standard number for
   ;; IT-equipment electrical safety (expected to be a CSA/UL binational
   ;; standard such as "CAN/CSA-C22.2 No. 62368-1") -- the CSA Group store
   ;; page returned HTTP 403 to a direct fetch and had no Wayback Machine
   ;; snapshot, so per this fleet's hard rule against bypassing bot
   ;; blocks, :required-evidence below cites only the jurisdiction-neutral
   ;; `IEC-62368-1-safety-test-report` shared anchor already established
   ;; by every other entry in this catalog, not a Canada-specific standard
   ;; number.
   "CAN" {:name "Canada"
          :owner-authority "Innovation, Science and Economic Development Canada (ISED), Spectrum Management and Telecommunications / Engineering, Planning and Standards Branch -- Category II equipment Supplier's Declaration of Conformity (SDoC); no ISED certification or registration required (Radiocommunication Regulations, SOR/96-484, s.21(5))"
          :legal-basis "Radiocommunication Act (R.S.C., 1985, c. R-2) s.2 (\"interference-causing equipment means any device, machinery or equipment, other than radio apparatus, that causes or is capable of causing interference to radiocommunication\") + s.5(1) (Minister's power to issue technical acceptance certificates and to establish technical requirements and technical standards for interference-causing equipment) + Radiocommunication Regulations (SOR/96-484) s.21(5) (Category II equipment listed in the Category II Equipment Standards List does not require a TAC) -- ICES-003, Issue 7, October 2020, \"Information Technology Equipment (including Digital Apparatus)\" + ICES-Gen, Issue 2, February 23 2024, \"General Requirements for Compliance of Interference-Causing Equipment\" s.3.3 (Supplier's Declaration of Conformity, reference)"
          :national-spec "Canada Supplier's Declaration of Conformity (SDoC) self-certification under ICES-003 (radio-frequency emission limits + administrative/labelling requirements) for information technology equipment and digital apparatus generating/using timing signals at or above 9 kHz -- Category II interference-causing equipment, exempt from ISED certification and registration; the CAN ICES-003(A or B) / NMB-003(A ou B) compliance label itself represents the SDoC"
          :provenance "https://ised-isde.canada.ca/site/spectrum-management-telecommunications/en/devices-and-equipment/interference-causing-equipment-standards-ices/ices-003-information-technology-equipment-including-digital-apparatus (ICES-003 standard page) ; https://ised-isde.canada.ca/site/spectrum-management-telecommunications/sites/default/files/attachments/2022/ICES-003-i7-2020-10EN.pdf (ICES-003 Issue 7 PDF, ss.1.1/4.1/4.2) ; https://ised-isde.canada.ca/site/spectrum-management-telecommunications/sites/default/files/documents/ices-gen_issue2.pdf (ICES-Gen Issue 2 PDF, s.3.3 SDoC/Category II) ; https://laws-lois.justice.gc.ca/eng/acts/R-2/ (Radiocommunication Act, ss.2/5) ; https://laws-lois.justice.gc.ca/eng/regulations/SOR-96-484/FullText.html (Radiocommunication Regulations, s.21(5))"
          :required-evidence ["ICES-003-EMC-test-report (conducted + radiated emission test per CAN/CSA-CISPR 32:17 or ANSI C63.4)"
                               "CAN-ICES-003-NMB-003-compliance-label (Class A or Class B, manufacturer self-declared per ICES-Gen s.6)"
                               "IEC-62368-1-safety-test-report"
                               "end-of-line-quality-chain-of-custody-record"]}})

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
