(ns jet.formats
  {:no-doc true}
  (:require
   [cheshire.core :as cheshire]
   [cheshire.factory :as factory]
   [cheshire.parse :as cheshire-parse]
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [cognitect.transit :as transit]
   [fipp.edn :as fipp]
   [jet.data-readers]
   [puget.printer :as puget]
   [rewrite-clj.zip :as z])
  (:import
   [com.fasterxml.jackson.core JsonFactory]
   [java.io Reader]
   [org.apache.commons.io.input ReaderInputStream]))

(set! *warn-on-reflection* true)

(def ^:private with-c-lib?
  (boolean (resolve 'org.babashka.CLibrary)))

(def in-native-image?
  (= "true"
     (System/getProperty "com.oracle.graalvm.isaot") ))

(defmacro ^:no-doc
  if-c-lib [then else]
  (if with-c-lib?
    then else))

(defn in-terminal? []
  (if-c-lib
      (when in-native-image?
        (pos? (org.babashka.CLibrary/isatty 1)))
    false))

(defn colorize? [colors]
  (or (= true colors)
      (= :always colors)
      (and (= :auto colors)
           (in-terminal?))))

(defn pprint [x colors {:keys [uncomma print-width]}]
  (if colors
    (puget/cprint x {:map-delimiter (if uncomma "" ",")
                     :width print-width})
    (fipp/pprint x {:width print-width})))

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

(defn uncomma-edn [edn-str]
  (-> (let [loc (z/of-string edn-str)]
        (loop [loc loc]
          (let [next (z/next* loc)]
            (if (z/end? loc) (z/root loc)
                (if (= :comma (z/tag loc))
                  (recur (z/remove* loc))
                  (recur next))))))
      str))

(defn generate-edn [o pretty color uncomma {:keys [print-width]}]
  (let [edn-str (if pretty (str/trim (with-out-str (pprint o color {:uncomma uncomma
                                                                    :print-width print-width})))
                (pr-str o))]
    (if (and uncomma (not color))
      (uncomma-edn edn-str)
      edn-str)))

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

(defn parse-yaml [rdr keywordize]
  (try
    (or (some->> (if (fn? keywordize)
                   [:key-fn #(-> % :key keywordize)]
                   [:keywords (boolean keywordize)])
                 (concat [rdr])
                 (apply yaml/parse-stream)
                 (walk/postwalk (fn [x] (if (seq? x) (vec x) x))))
        ::EOF)
    (catch org.yaml.snakeyaml.parser.ParserException e
      (if (str/includes? (ex-message e) "but got <stream end>")
        ::EOF
        (throw e)))))

(defn generate-yaml [o pretty]
  (yaml/generate-string o :dumper-options {:flow-style (if pretty :block :auto)}))
