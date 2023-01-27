(ns jet.base64
  (:import [java.util Base64]))

(set! *warn-on-reflection* true)

(defn decode [x]
  (let [bytes (.decode (Base64/getDecoder) (str x))]
    (String. bytes "utf-8")))

(defn encode [x]
  (.encodeToString (Base64/getEncoder) (.getBytes (str x))))

(def base64-namespace
  {'decode decode
   'encode encode})
