(ns jet.utils
  {:no-doc true}
  (:refer-clojure :exclude [println prn]))

(defn println [& args]
  (binding [*print-length* 5
            *print-level* 5]
    (apply clojure.core/println args)))

(defn prn [& args]
  (binding [*print-length* 5
            *print-level* 5]
    (apply clojure.core/prn args)))
