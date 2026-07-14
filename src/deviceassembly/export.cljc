(ns deviceassembly.export
  "Audit-package export for social / regulatory hand-off.

  Produces plain EDN maps and CSV strings over a
  `deviceassembly.store/Store` snapshot -- the same append-only
  ledger, device-unit-shipment drafts and Declaration-of-Conformity
  drafts the governor writes. Pure data transforms only: no I/O, no
  network, no signature. The manufacturer's own act is to sign and
  file the package; this namespace only materializes the package
  body.

  This is the honest delivery of the industry-stack `:export?` contract
  (robotics / audit-ledger capabilities) for ISIC 2620."
  (:require [clojure.string :as str]
            [deviceassembly.store :as store]))

(defn- csv-escape [v]
  (let [s (str (if (nil? v) "" v))]
    (if (re-find #"[,\"\n\r]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn- csv-row [cols]
  (str/join "," (map csv-escape cols)))

(defn ledger-rows
  "Normalize ledger facts into flat row maps suitable for CSV."
  [st]
  (mapv (fn [i f]
          {:seq i
           :t (:t f)
           :op (str (:op f))
           :actor (:actor f)
           :subject (:subject f)
           :disposition (str (:disposition f))
           :basis (pr-str (:basis f))
           :summary (:summary f)})
        (range)
        (store/ledger st)))

(defn shipment-rows [st]
  (mapv (fn [i r]
          {:seq i
           :record_id (get r "record_id")
           :kind (get r "kind")
           :device_unit_id (get r "device_unit_id")
           :jurisdiction (get r "jurisdiction")})
        (range)
        (store/shipment-history st)))

(defn declaration-rows [st]
  (mapv (fn [i r]
          {:seq i
           :record_id (get r "record_id")
           :kind (get r "kind")
           :device_unit_id (get r "device_unit_id")
           :jurisdiction (get r "jurisdiction")})
        (range)
        (store/declaration-history st)))

(defn device-units-snapshot [st]
  (mapv (fn [b]
          (select-keys b [:id :device-unit-name :jurisdiction :status
                          :emc-emission-deviation-actual
                          :emc-emission-deviation-min
                          :emc-emission-deviation-max
                          :eol-defect-unresolved?
                          :device-unit-shipped?
                          :declaration-issued?
                          :shipment-number
                          :evidence-number]))
        (store/all-device-units st)))

(defn audit-package
  "Full audit package for a store snapshot -- the body a device-
  assembly manufacturer would hand to compliance inspectors, market-
  regulator inspectors or internal compliance. `:format` is always
  `:edn-maps` for the nested package; use `package->csv-bundle` for
  CSV strings."
  [st]
  {:isic "2620"
   :business-id "cloud-itonami-isic-2620"
   :format :edn-maps
   :device-units (device-units-snapshot st)
   :ledger (vec (store/ledger st))
   :shipments (vec (store/shipment-history st))
   :declarations-of-conformity (vec (store/declaration-history st))
   :counts {:device-units (count (store/all-device-units st))
            :ledger (count (store/ledger st))
            :shipments (count (store/shipment-history st))
            :declarations-of-conformity (count (store/declaration-history st))}})

(defn rows->csv
  "Render a seq of flat maps as CSV using `header` column order."
  [header rows]
  (let [lines (into [(csv-row (map name header))]
                    (map (fn [r] (csv-row (map #(get r %) header))) rows))]
    (str (str/join "\n" lines) (when (seq lines) "\n"))))

(defn package->csv-bundle
  "CSV bundle for spreadsheet hand-off. Keys are filenames; values are
  CSV body strings."
  [st]
  {"device-units.csv" (rows->csv [:id :device-unit-name :jurisdiction :status
                                 :emc-emission-deviation-actual
                                 :device-unit-shipped? :declaration-issued?
                                 :shipment-number :evidence-number]
                                (device-units-snapshot st))
   "ledger.csv" (rows->csv [:seq :t :op :actor :subject :disposition :basis :summary]
                           (ledger-rows st))
   "shipments.csv" (rows->csv [:seq :record_id :kind :device_unit_id :jurisdiction]
                              (shipment-rows st))
   "declarations-of-conformity.csv" (rows->csv [:seq :record_id :kind :device_unit_id :jurisdiction]
                                       (declaration-rows st))})

#?(:clj
(defn write-csv-bundle!
  "Write `package->csv-bundle` files under `dir` (created if missing).
  Returns the absolute path of `dir`. JVM-only I/O seam for social
  hand-off scripts; pure package construction stays in `package->csv-bundle`."
  [st dir]
  (let [d (java.io.File. (str dir))
        _ (.mkdirs d)
        bundle (package->csv-bundle st)]
    (doseq [[name body] bundle]
      (spit (java.io.File. d (str name)) body))
    (.getAbsolutePath d))))
