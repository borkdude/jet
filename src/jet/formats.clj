(ns jet.formats
  {:no-doc true}
  (:require
   [cheshire.core :as cheshire]
   [cheshire.factory :as factory]
   [cheshire.parse :as cheshire-parse]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [jet.csv :as csv]
   [cognitect.transit :as transit]
   [fipp.edn :as fipp]
   [jet.data-readers])
  (:import [com.fasterxml.jackson.core JsonParser JsonFactory
            JsonGenerator PrettyPrinter
            JsonGenerator$Feature]
           [java.io StringWriter StringReader BufferedReader BufferedWriter
            ByteArrayOutputStream OutputStream Reader Writer PushbackReader]
           [org.apache.commons.io.input ReaderInputStream]))

(set! *warn-on-reflection* true)

(defn json-parser []
  (.createParser ^JsonFactory (or factory/*json-factory*
                                  factory/json-factory)
                 ^Reader *in*))

(defn parse-json [json-reader keywordize]
  (cheshire-parse/parse-strict json-reader keywordize ::EOF nil))

(defn generate-json [o pretty]
  (cheshire/generate-string o {:pretty pretty}))

(defn parse-edn [*in*]
  (edn/read {:eof ::EOF} *in*))

(defn generate-edn [o pretty]
  (if pretty (str/trim (with-out-str (fipp/pprint o)))
      (pr-str o)))

(defn transit-reader []
  (transit/reader (ReaderInputStream. *in*) :json))

(defn parse-transit [rdr]
  (try (transit/read rdr)
       (catch java.lang.RuntimeException e
         (if-let [cause (.getCause e)]
           (if (instance? java.io.EOFException cause)
             ::EOF
             (throw e))
           (throw e)))))

(defn generate-transit [o]
  (let [bos (java.io.ByteArrayOutputStream. 1024)
        writer (transit/writer bos :json)]
    (transit/write writer o)
    (String. (.toByteArray bos) "UTF-8")))

(defn push-back-reader []
  (PushbackReader. *in*))

(defn parse-csv [rdr]
  (let [[record sentinel] (csv/read-record rdr (int \,)  (int \"))]
    (if (and (= [""] record) (= :eof sentinel))
      ::EOF
      record)))

(defn parse-tsv [rdr]
  (let [[record sentinel] (csv/read-record rdr (int \tab)  (int \"))]
    (if (and (= [""] record) (= :eof sentinel))
      ::EOF
      record)))

(defn generate-csv [o]
  (let [s (StringWriter.)
        sep \,
        quote \"
        quote? #(some #{sep quote \return \newline} %)]
    (csv/write-record s o sep quote quote?)
    (str s)))

(defn generate-tsv [o]
  (let [s (StringWriter.)
        sep \tab
        quote \"
        quote? #(some #{sep quote \return \newline} %)]
    (csv/write-record s o sep quote quote?)
    (str s)))
