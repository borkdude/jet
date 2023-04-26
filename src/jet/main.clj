(ns jet.main
  {:no-doc true}
  (:require
   [babashka.cli :as cli]
   [camel-snake-kebab.core :as csk]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [jet.base64 :refer [base64-namespace]]
   [jet.data-readers]
   [jet.formats :as formats]
   [jet.jeti :refer [start-jeti!]]
   [jet.query :as q]
   [jet.specter :as specter :refer [config]]
   [sci.core :as sci])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -paths [m opts]
  (cond
    (map? m)
    (vec
     (concat (map vector (keys m))
             (mapcat (fn [[k v]]
                       (let [sub (-paths v opts)
                             nested (map #(into [k] %) (filter (comp not empty?) sub))]
                         (when (seq nested)
                           nested)))
                     m)))
    (vector? m)
    (vec (concat (map vector (range (count m)))
                 (mapcat (fn [idx]
                           (let [sub (-paths (get m idx) opts)
                                 nested (map #(into [idx] %) (filter (comp not empty?) sub))]
                             (when (seq nested)
                               nested)))
                         (range (count m)))))
    :else []))

(defn paths
  [m]
  (let [paths (-paths m nil)
        paths (mapv (fn [path]
                      (let [v (get-in m path)]
                        {:path path
                         :val v}))
                    paths)]
    paths))

(defn when-pred [pred]
  (fn [x]
    (try (when (pred x)
           x)
         (catch Exception _ nil))))

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
                              {}
                              'base64 base64-namespace
                              'jet {'paths paths
                                    'when-pred when-pred}}
                 :aliases '{str clojure.string
                            s com.rpl.specter
                            csk camel-snake-kebab.core}})
      (sci/merge-opts config)))

(defn coerce-file [s]
  (if (.exists (io/as-file s))
    (slurp s)
    s))

(defn coerce-eval-string [x]
  (->> x coerce-file (sci/eval-string* ctx)))

(defn coerce-keywordize [x]
  (cli/coerce x (fn [x]
                  (cond (= "true" x) true
                        (= "false" x) false
                        :else (coerce-eval-string x)))))

(defn coerce-query [query]
  (edn/read-string
   {:readers *data-readers*}
   (format "[%s]" query)))

(defn coerce-thread-last [s]
  (->> s coerce-file
       (format "(fn [edn] (->> edn %s))")
       (sci/eval-string* ctx)))

(defn coerce-thread-first [s]
  (->> s coerce-file
       (format "(fn [edn] (-> edn %s))")
       (sci/eval-string* ctx)))

(defn coerce-interactive [interactive]
  (not-empty (str/join " " interactive)))

(def cli-spec
  {:from            {:coerce :keyword
                     :alias  :i
                     :ref "[ edn | transit | json | yaml ]"
                     :desc   "defaults to edn."}
   :to              {:coerce :keyword
                     :alias  :o
                     :ref "[ edn | transit | json | yaml ]"
                     :desc   "defaults to edn."}
   :colors          {:ref  "[ auto | true | false]"
                     :desc "use colored output while pretty-printing. Defaults to auto."}
   :thread-last     {:alias :t
                     :desc  "implicit thread last"}
   :thread-first    {:alias :T
                     :desc  "implicit thread first"}
   :func            {:alias :f
                     :desc  "a single-arg Clojure function, or a path to a file that contains a function, that transforms input."}
   :query           {:alias :q
                     :desc  "DEPRECATED, prefer -t, -T or -f. Given a jet-lang query, transforms input."}
   :collect         {:alias :c
                     :desc  "given separate values, collects them in a vector."}
   :version         {:alias :v
                     :desc  "print the current version of jet."}
   :help            {:alias :h
                     :desc  "print this help text."}
   :keywordize      {:alias :k
                     :ref   "[ <key-fn> ]"
                     :desc  "if present, keywordizes JSON/YAML keys. The default transformation function is keyword unless you provide your own."}
   :no-pretty       {:coerce :boolean
                     :desc   "disable pretty printing"}
   :edn-reader-opts {:desc "options passed to the EDN reader."}
   :no-commas       {:coerce :boolean
                     :desc "remove commas from EDN"}})

(def cli-opts
  {:spec cli-spec
   :order (let [first-ks [:from :to
                          :thread-last :thread-first :func]]
            (into first-ks (remove (set first-ks) (keys cli-spec))))
   :no-keyword-opts true})

(defn parse-opts [args]
  (cli/parse-opts
   args
   cli-opts))

(defn get-version
  "Gets the current version of the tool"
  []
  (str/trim (slurp (io/resource "JET_VERSION"))))

(defn print-help
  "Prints the help text"
  []
  (println (str "jet v" (get-version)))
  (println)
  (println "Options:")
  (println)
  (println (cli/format-opts cli-opts))
  (println))

(defn exec [{:keys [from to keywordize
                    no-pretty version query
                    func thread-first thread-last interactive collect
                    edn-reader-opts
                    help colors no-commas]
             :or {from :edn
                  to :edn
                  colors :auto}}]
  (let [colors (formats/colorize? colors)
        [func thread-first thread-last keywordize edn-reader-opts query]
        [(cli/coerce func coerce-eval-string) (cli/coerce thread-first coerce-thread-first)
         (cli/coerce thread-last coerce-thread-last)
         (cli/coerce keywordize coerce-eval-string)
         (cli/coerce edn-reader-opts coerce-eval-string)
         (cli/coerce query coerce-query)]]
    (cond
      version (println (get-version))
      interactive (start-jeti! interactive colors)
      help (print-help)
      :else
      (let [reader (case from
                     :json (formats/json-parser)
                     :transit (formats/transit-reader)
                     :yaml nil
                     :edn nil)
            next-val (case from
                       :edn #(formats/parse-edn edn-reader-opts *in*)
                       :json #(formats/parse-json reader keywordize)
                       :transit #(formats/parse-transit reader)
                       :yaml #(formats/parse-yaml *in* keywordize))
            collected (when collect (vec (take-while #(not= % ::formats/EOF)
                                                     (repeatedly next-val))))
            func (or func thread-first thread-last)]
        (loop []
          (let [input (if collect collected (next-val))]
            (when-not (identical? ::formats/EOF input)
              (let [input (if query (q/query input query)
                              input)
                    input (if func
                            (func input)
                            input)]
                (case to
                  :edn (some->
                        input
                        (formats/generate-edn (not no-pretty) colors no-commas)
                        println)
                  :json (some->
                         input
                         (formats/generate-json (not no-pretty))
                         println)
                  :transit (some->
                            (formats/generate-transit input)
                            println)
                  :yaml (some->
                         input
                         (formats/generate-yaml (not no-pretty))
                         println)))
              (when-not collect (recur)))))))))

(defn main [& args]
  (let [opts (parse-opts args)]
    (exec opts)))

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
