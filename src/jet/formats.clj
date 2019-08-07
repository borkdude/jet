(ns jet.formats
  {:no-doc true}
  (:require
   [cheshire.core :as cheshire]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cognitect.transit :as transit]
   [fipp.edn :as fipp]
   [jet.data-readers]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn parse-json [s keywordize?]
  (cheshire/parse-string s keywordize?))

(defn generate-json [o pretty]
  (cheshire/generate-string o {:pretty pretty}))

(defn parse-edn [s]
  (edn/read-string s))

(defn generate-edn [o pretty]
  (if pretty (str/trim (with-out-str (fipp/pprint o)))
      (pr-str o)))

(defn parse-transit [^String s]
  (transit/read
   (transit/reader
    (io/input-stream (.getBytes s)) :json)))

(defn generate-transit [o]
  (let [bos (java.io.ByteArrayOutputStream. 1024)
        writer (transit/writer bos :json)]
    (transit/write writer o)
    (String. (.toByteArray bos) "UTF-8")))
