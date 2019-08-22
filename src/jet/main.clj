(ns jet.main
  {:no-doc true}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [starts-with?]]
   [jet.data-readers]
   [jet.formats :as formats]
   [jet.jeti :refer [start-jeti!]]
   [jet.query :as q]
   [sci.core :refer [eval-string]])
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
                     (if (empty? k) true
                         (let [f (first k)]
                             (= "true" f) true
                             (= "false" f) false
                             :else (eval-string f))))
        version (boolean (get opts "--version"))
        pretty (boolean (get opts "--pretty"))
        query (first (get opts "--query"))
        interactive (get opts "--interactive")
        collect (boolean (get opts "--collect"))]
    {:from (or from :edn)
     :to (or to :edn)
     :keywordize keywordize
     :version version
     :pretty pretty
     :query (when query
              (edn/read-string
               {:readers *data-readers*}
               (format "[%s]" query)))
     :interactive (or (and interactive (empty? interactive))
                      (not-empty (str/join " " interactive)))
     :collect collect}))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version :query
                :interactive :collect]} (parse-opts args)]
    (cond version
          (println (str/trim (slurp (io/resource "JET_VERSION"))))
          interactive (start-jeti! interactive)
          :else
          (let [reader (case from
                         :json (formats/json-parser)
                         :transit (formats/transit-reader)
                         :edn nil)
                next-val (case from
                           :edn #(formats/parse-edn *in*)
                           :json #(formats/parse-json reader keywordize)
                           :transit #(formats/parse-transit reader))
                collected (when collect (vec (take-while #(not= % ::formats/EOF)
                                                         (repeatedly next-val))))]
            (loop []
              (let [input (if collect collected (next-val))]
                (when-not (identical? ::formats/EOF input)
                  (let [input (if query (q/query input query)
                                  input)]
                    (case to
                      :edn (println (formats/generate-edn input pretty))
                      :json (println (formats/generate-json input pretty))
                      :transit (println (formats/generate-transit input))))
                  (when-not collect (recur)))))))))

;;;; Scratch

(comment
)
