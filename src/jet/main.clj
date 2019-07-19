(ns jet.main
  {:no-doc true}
  (:require
   [cheshire.core :as cheshire]
   [clojure.string :as str :refer [starts-with?]]
   [clojure.edn :as edn]
   [cognitect.transit :as transit]
   [clojure.java.io :as io]
   [fipp.edn :refer [pprint] :rename {pprint fipp}])
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
        keywordize (when-let [k (get opts "--keywordize")]
                     (cond (empty? k) true
                           (= "true" (first k)) true
                           :else false))
        version (boolean (get opts "--version"))
        pretty (boolean (get opts "--pretty"))]
    {:from from
     :to to
     :keywordize keywordize
     :version version
     :pretty pretty}))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version]} (parse-opts args)]
    (if version
      (println (str/trim (slurp (io/resource "JET_VERSION"))))
      (let [in (slurp *in*)
            input (case from
                    :edn (edn/read-string in)
                    :json (cheshire/parse-string in keywordize)
                    :transit (transit/read
                              (transit/reader (io/input-stream (.getBytes in)) :json)))]
        (case to
          :edn (if pretty (fipp input) (prn input))
          :json (println (cheshire/generate-string input {:pretty pretty}))
          :transit (let [bos (java.io.ByteArrayOutputStream. 1024)
                         writer (transit/writer bos :json)]
                     (transit/write writer input)
                     (println (String. (.toByteArray bos) "UTF-8"))))))))

;;;; Scratch

(comment
  *out*
  System/out
  (io/writer *out*)
  (fipp {:a 1}))
