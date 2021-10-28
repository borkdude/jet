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
   [sci.core :as sci :refer [eval-string eval-string*]]
   [camel-snake-kebab.core :as csk])
  (:gen-class))

(set! *warn-on-reflection* true)

(def csk-ctx
  (sci/init {:namespaces {'csk
                          {'->PascalCase csk/->PascalCase
                           '->camelCase csk/->camelCase
                           '->SCREAMING_SNAKE_CASE csk/->SCREAMING_SNAKE_CASE
                           '->snake_case csk/->snake_case
                           '->kebab-case csk/->kebab-case
                           '->Camel_Snake_Case csk/->Camel_Snake_Case
                           '->HTTP-Header-Case csk/->HTTP-Header-Case}}}))

(defn parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "-")
                   (recur (rest options)
                          (assoc opts-map opt [])
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        from (some-> (or (get opts "--from")
                         (get opts "-i")) first keyword)
        to (some-> (or (get opts "--to")
                       (get opts "-o"))
                   first keyword)
        keywordize (when-let [k (or (get opts "--keywordize")
                                    (get opts "-k"))]
                     (if (empty? k) true
                         (let [f (first k)]
                           (cond (= "true" f) true
                                 (= "false" f) false
                                 :else (eval-string* csk-ctx f)))))
        version (boolean (or (get opts "--version")
                             (get opts "-v")))
        pretty (boolean (or (get opts "--pretty")
                            (get opts "-p")))
        query (first (or (get opts "--query")
                         (get opts "-q")))
        interactive (get opts "--interactive")
        collect (boolean (or (get opts "--collect")
                             (get opts "-c")))
        edn-reader-opts (let [opts (first (get opts "--edn-reader-opts"))]
                          (if opts
                            (eval-string opts)
                            {:default tagged-literal}))
        help (boolean (or (get opts "--help")
                          (get opts "-h")))
        func (first (or (get opts "--func")
                        (get opts "-f")))]
    {:from (or from :edn)
     :to (or to :edn)
     :keywordize keywordize
     :version version
     :pretty pretty
     :query (when query
              (edn/read-string
               {:readers *data-readers*}
               (format "[%s]" query)))
     :func func
     :interactive (or (and interactive (empty? interactive))
                      (not-empty (str/join " " interactive)))
     :collect collect
     :edn-reader-opts edn-reader-opts
     :help help}))

(defn get-version
  "Gets the current version of the tool"
  []
  (str/trim (slurp (io/resource "JET_VERSION"))))

(defn print-help
  "Prints the help text"
  []
  (println (str "jet v" (get-version)))
  (println "
  -h, --help: print this help text.
  -v, --version: print the current version of jet.
  -i, --from: edn, transit or json, defaults to edn.
  -o, --to: edn, transit or json, defaults to edn.
  -k, --keywordize [ <key-fn> ]: if present, keywordizes JSON keys. The default transformation function is keyword unless you provide your own.
  -p, --pretty: if present, pretty-prints JSON and EDN output.
  -f, --func: a single-arg Clojure function, or a path to a file that contains a function, that transforms input.
  --edn-reader-opts: options passed to the EDN reader.
  -q, --query: given a jet-lang query, transforms input. See doc/query.md for more.
  -c, --collect: given separate values, collects them in a vector.
  --interactive [ cmd ]: if present, starts an interactive shell. An initial command may be provided. See README.md for more.")
  (println))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version :query
                :func :interactive :collect
                :edn-reader-opts
                :help]} (parse-opts args)]
    (cond
      (nil? args) (print-help)
      version (println (get-version))
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
                              input)
                    input (if func
                            (let [f (eval-string (if (.exists (io/as-file func))
                                                   (slurp func)
                                                   func))]
                              (f input))
                            input)]
                (case to
                  :edn (println (formats/generate-edn input pretty))
                  :json (println (formats/generate-json input pretty))
                  :transit (println (formats/generate-transit input))))
              (when-not collect (recur)))))))))

;;;; Scratch

(comment
  (eval-string "(keyword (csk/->kebab-case \"anApple\"))"
    {:namespaces {'csk {'->kebab-case csk/->kebab-case}}})
  (eval-string* csk-ctx "(keyword (csk/->kebab-case \"anApple\"))")
  (defn jet [input & args]
    (with-out-str
      (with-in-str input
        (apply -main args))))
  (jet "{\"anApple\":2} {\"HelloWorld\":3} {\"looks_good\":4}" "--from" "json" "--keywordize" "#(-> % csk/->kebab-case keyword)")
  (jet "{\"anApple\":2} {\"a\":3} {\"a\":4}" "--from" "json" "--keywordize" "#(-> % csk/->snake_case keyword)")
  ,)
