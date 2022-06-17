(ns jet.main
  {:no-doc true}
  (:require
   [babashka.cli :as cli]
   [camel-snake-kebab.core :as csk]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [starts-with?]]
   [jet.data-readers]
   [jet.formats :as formats]
   [jet.jeti :refer [start-jeti!]]
   [jet.query :as q]
   [jet.specter :as specter :refer [config]]
   [sci.core :as sci])
  (:gen-class))

(set! *warn-on-reflection* true)

(def ctx
  (-> (sci/init {:namespaces {'camel-snake-kebab.core
                              {'->PascalCase csk/->PascalCase
                               '->camelCase csk/->camelCase
                               '->SCREAMING_SNAKE_CASE csk/->SCREAMING_SNAKE_CASE
                               '->snake_case csk/->snake_case
                               '->kebab-case csk/->kebab-case
                               '->Camel_Snake_Case csk/->Camel_Snake_Case
                               '->HTTP-Header-Case csk/->HTTP-Header-Case}
                              'com.rpl.specter
                              {}}
                 :aliases {'str 'clojure.string
                           's 'com.rpl.specter}})
      (sci/merge-opts config)))

(sci/eval-form ctx '(require '[camel-snake-kebab.core :as csk]))

(defn coerce-file [s]
  (if (.exists (io/as-file s))
    (slurp s)
    s))

(defn parse-opts* [options]
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
                                 :else (sci/eval-string* ctx f)))))
        version (boolean (or (get opts "--version")
                             (get opts "-v")))
        no-pretty (boolean (get opts "--no-pretty"))
        query (first (or (get opts "--query")
                         (get opts "-q")))
        interactive (get opts "--interactive")
        collect (boolean (or (get opts "--collect")
                             (get opts "-c")))
        edn-reader-opts (let [opts (first (get opts "--edn-reader-opts"))]
                          (if opts
                            (sci/eval-string* ctx opts)
                            {:default tagged-literal}))
        help (boolean (or (get opts "--help")
                          (get opts "-h")))
        thread-last (some-> (first (or (get opts "--thread-last")
                                       (get opts "-t")))
                            coerce-file)
        thread-first (some-> (first (or (get opts "--thread-first")
                                        (get opts "-T")))
                             coerce-file)
        func (or (some-> (first (or (get opts "--func")
                                    (get opts "-f")))
                         coerce-file)
                 (when thread-first
                   (format "#(-> %% %s)" thread-first))
                 (when thread-last
                   (format "#(->> %% %s)" thread-last)))
        colors (or (some-> (get opts "--colors")
                           first
                           keyword)
                   :auto)]
    {:from (or from :edn)
     :to (or to :edn)
     :keywordize keywordize
     :version version
     :no-pretty no-pretty
     :query (when query
              (edn/read-string
               {:readers *data-readers*}
               (format "[%s]" query)))
     :func func
     :interactive (or (and interactive (empty? interactive))
                      (not-empty (str/join " " interactive)))
     :collect collect
     :edn-reader-opts edn-reader-opts
     :colors colors
     :help help}))

(defn coerce-keywordize [x]
  (cli/coerce x (fn [x]
                  (cond (= "true" x) true
                        (= "false" x) false
                        :else (sci/eval-string* ctx x)))))

(defn coerce-query [query]
  (edn/read-string
   {:readers *data-readers*}
   (format "[%s]" query)))

(defn coerce-thread-last [s]
  (->> s coerce-file
       (format "#(->> %% %s)")))

(defn coerce-thread-first [s]
  (->> s coerce-file
       (format "#(-> %% %s)")))

(defn coerce-interactive [interactive]
  (not-empty (str/join " " interactive)))

(defn parse-opts [args]
  (cli/parse-opts
   args
   {:coerce {:from :keyword
             :to :keyword
             :colors :boolean
             :edn-reader-opts :edn
             :keywordize coerce-keywordize
             :func coerce-file
             :thread-last coerce-thread-last
             :thread-first coerce-thread-first
             :query coerce-query}
    :aliases {:i :from
              :o :to
              :t :thread-last
              :T :thread-first
              :f :func
              :q :query
              :c :collect
              :v :version
              :h :help}
    :exec-args {:from :edn :to :edn}}))

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
  --no-pretty: disable pretty-printing.
  --colors [auto | true | false]: use colored output while pretty-printing. Defaults to auto.
  -f, --func: a single-arg Clojure function, or a path to a file that contains a function, that transforms input.
  -t, --thread-last: implicit thread last
  -T, --thread-first: implicit thread first
  --edn-reader-opts: options passed to the EDN reader.
  -q, --query: given a jet-lang query, transforms input. See https://github.com/borkdude/jet/blob/master/doc/query.md for more.
  -c, --collect: given separate values, collects them in a vector.
  --interactive [ cmd ]: if present, starts an interactive shell. An initial command may be provided. See README.md for more.")
  (println))

(defn main [& args]
  (let [{:keys [:from :to :keywordize
                :no-pretty :version :query
                :func :thread-first :thread-last :interactive :collect
                :edn-reader-opts
                :help :colors]} (parse-opts args)]
    (cond
      version (println (get-version))
      interactive (start-jeti! interactive colors)
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
                                                     (repeatedly next-val))))
            func (or func thread-first thread-last)]
        (loop []
          (let [input (if collect collected (next-val))]
            (when-not (identical? ::formats/EOF input)
              (let [input (if query (q/query input query)
                              input)
                    input (if func
                            (let [f (sci/eval-string* ctx func)]
                              (f input))
                            input)]
                (case to
                  :edn (some->
                        input
                        (formats/generate-edn (not no-pretty) colors)
                        println)
                  :json (some->
                         input
                         (formats/generate-json (not no-pretty))
                         println)
                  :transit (some->
                            (formats/generate-transit input)
                            println)))
              (when-not collect (recur)))))))))

;; enable println, prn etc.
(sci/alter-var-root sci/out (constantly *out*))
(sci/alter-var-root sci/err (constantly *err*))
(vreset! specter/sci-ctx ctx)

(when (System/getProperty "jet.native")
  (require 'jet.patches))

(def musl?
  "Captured at compile time, to know if we are running inside a
  statically compiled executable with musl."
  (and (= "true" (System/getenv "BABASHKA_STATIC"))
       (= "true" (System/getenv "BABASHKA_MUSL"))))

(defmacro run [args]
  (if musl?
    ;; When running in musl-compiled static executable we lift execution of bb
    ;; inside a thread, so we have a larger than default stack size, set by an
    ;; argument to the linker. See https://github.com/oracle/graal/issues/3398
    `(let [v# (volatile! nil)
           f# (fn []
                (vreset! v# (apply main ~args)))]
       (doto (Thread. nil f# "main")
         (.start)
         (.join))
       @v#)
    `(apply main ~args)))

(defn -main
  [& args]
  (run args))
