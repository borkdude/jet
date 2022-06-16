(ns jet.formats
  {:no-doc true}
  (:require
   [cheshire.core :as cheshire]
   [cheshire.factory :as factory]
   [cheshire.parse :as cheshire-parse]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [cognitect.transit :as transit]
   [fipp.edn :as fipp]
   [jet.data-readers]
   [puget.printer :as puget])
  (:import
   [com.fasterxml.jackson.core JsonFactory]
   [java.io Reader]
   [org.apache.commons.io.input ReaderInputStream]
   [org.fusesource.jansi.internal CLibrary]))

(set! *warn-on-reflection* true)

(defn pprint [x]
  (if (pos? (CLibrary/isatty CLibrary/STDOUT_FILENO))
    (puget/cprint x)
    (fipp/pprint x)))

(defn json-parser []
  (.createParser ^JsonFactory (or factory/*json-factory*
                                  factory/json-factory)
                 ^Reader *in*))

(defn parse-json [json-reader keywordize]
  (cheshire-parse/parse-strict json-reader keywordize ::EOF nil))

(defn generate-json [o pretty]
  (cheshire/generate-string o {:pretty pretty}))

(defn parse-edn [opts *in*]
  (edn/read (assoc opts :eof ::EOF) *in*))

(defn generate-edn [o pretty]
  (if pretty (str/trim (with-out-str (pprint o)))
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
