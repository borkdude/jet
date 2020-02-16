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
        collect (boolean (get opts "--collect"))
        edn-reader-opts (let [opts (first (get opts "--edn-reader-opts"))]
                          (when opts
                            (eval-string opts)))
        help (boolean (get opts "--help"))]
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
     :collect collect
     :edn-reader-opts edn-reader-opts
     :help help}))

(defn get-version
 "Gets the current version of the tool"
 []
 (str/trim (slurp (io/resource "JET_VERSION"))))

(defn get-usage
  "Gets the usage of the tool"
  []
  (str "Usage: jet [ --from <format> ] [ --to <format> ] [ --keywordize [ <key-fn> ] ] [ --pretty ] [ --edn-reader-opts ] [--query <query> ] [ --collect ] | [ --interactive <cmd> ]"))

(defn print-help
  "Prints the help text"
  []
  (println (str "jet v" (get-version)))
  (println)
  (println (get-usage))
  (println "
  --help: print this help text.
  --version: print the current version of jet.
  --from: edn, transit or json, defaults to edn.
  --to: edn, transit or json, defaults to edn.
  --keywordize [ <key-fn> ]: if present, keywordizes JSON keys. The default transformation function is keyword unless you provide your own.
  --pretty: if present, pretty-prints JSON and EDN output.
  --edn-reader-opts: options passed to the EDN reader.
  --query: given a jet-lang query, transforms input. See doc/query.md for more.
  --collect: given separate values, collects them in a vector.
  --interactive [ cmd ]: if present, starts an interactive shell. An initial command may be provided. See README.md for more.")
  (println))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version :query
                :interactive :collect
                :edn-reader-opts
                :help]} (parse-opts args)]
    (cond version
          (println (get-version))
          interactive (start-jeti! interactive)
          help (print-help)
          :else
          (let [reader (case from
                         :json (formats/json-parser)
                         :transit (formats/transit-reader)
                         :edn nil)
                next-val (case from
                           :edn #(formats/parse-edn edn-reader-opts *in*)
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
