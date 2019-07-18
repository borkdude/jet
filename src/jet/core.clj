(ns jet.core
  (:require
   [cheshire.core :as cheshire]
   [clojure.string :as str :refer [starts-with?]]
   [clojure.edn :as edn]
   [cognitect.transit :as transit]
   [clojure.java.io :as io])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn- parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          (assoc opts-map opt [])
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        from (-> (get opts "--from") first keyword)
        to (-> (get opts "--to") first keyword)
        keywordize (-> (get opts "--keywordize") first)
        version (boolean (get opts "--version"))]
    {:from from
     :to to
     :keywordize (= "true" keywordize)
     :version version}))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize :version]} (parse-opts args)]
    (if version
      (println (str/trim (slurp (io/resource "JET_VERSION"))))
      (let [in (slurp *in*)
            input (case from
                    :edn (edn/read-string in)
                    :json (cheshire/parse-string in keywordize)
                    :transit (transit/read
                              (transit/reader (io/input-stream (.getBytes in)) :json)))
            output (case to
                     :edn input
                     :json (cheshire/encode input)
                     :transit (let [bos (java.io.ByteArrayOutputStream. 1024)
                                    writer (transit/writer (io/output-stream bos) :json)]
                                (transit/write writer input)
                                (String. (.toByteArray bos) "UTF-8")))]
        (println output)))))
