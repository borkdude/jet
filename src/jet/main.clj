(ns jet.main
  {:no-doc true}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [starts-with?]]
   [jet.data-readers]
   [jet.formats :as formats]
   [jet.jeti :refer [start-jeti!]]
   [jet.query :as q])
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
        from (some-> (get opts "--from") first keyword)
        to (some-> (get opts "--to") first keyword)
        keywordize (when-let [k (get opts "--keywordize")]
                     (cond (empty? k) true
                           (= "true" (first k)) true
                           :else false))
        version (boolean (get opts "--version"))
        pretty (boolean (get opts "--pretty"))
        query (first (get opts "--query"))
        interactive (boolean (get opts "--interactive"))]
    {:from (or from :edn)
     :to (or to :edn)
     :keywordize keywordize
     :version version
     :pretty pretty
     :query (when query
              (edn/read-string
               {:readers *data-readers*}
               (format "[%s]" query)))
     :interactive interactive}))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version :query
                :interactive]} (parse-opts args)]
    (cond version
          (println (str/trim (slurp (io/resource "JET_VERSION"))))
          interactive (start-jeti!)
          :else
          (let [reader (case from
                         :json (formats/json-parser)
                         :transit (formats/transit-reader)
                         :edn nil)]
            (loop []
              (let [input (case from
                            :edn (formats/parse-edn *in*)
                            :json (formats/parse-json reader keywordize)
                            :transit (formats/parse-transit reader))]
                (when-not (identical? ::formats/EOF input)
                  (let [input (if query (q/query input query)
                                  input)]
                    (case to
                      :edn (println (formats/generate-edn input pretty))
                      :json (println (formats/generate-json input pretty))
                      :transit (println (formats/generate-transit input))))
                  (recur))))))))

;;;; Scratch

(comment)
