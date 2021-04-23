(ns jet.main
  {:no-doc true}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [starts-with?]]
   [clojure.tools.cli :as cli]
   [jet.data-readers]
   [jet.formats :as formats]
   [jet.jeti :refer [start-jeti!]]
   [jet.query :as q]
   [sci.core :refer [eval-string]])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-h" "--help" "print this help text."]
   ["-v" "--version" "print the current version of jet."]
   ["-i" "--from [json | edn | transit]" "edn, transit or json, defaults to edn."
    :parse-fn keyword
    :default :edn
    :default-desc ""]
   ["-o" "--to [json | edn | transit]" "edn, transit or json, defaults to edn."
    :parse-fn keyword
    :default :edn
    :default-desc ""]
   ["-k" "--keywordize" "if present, keywordizes JSON keys."]
   [nil "--keywordize-fn <keywordize-function>" "if present, keywordizes JSON keys with provided transformation function."
    :parse-fn eval-string
    :id :keywordize]
   ["-p" "--pretty" "if present, pretty-prints JSON and EDN output."]
   ["-f" "--func <function-or-path>" "a single-arg Clojure function, or a path to a file that contains a function, that transforms input."]
   [nil "--edn-reader-opts <edn-options>" "options passed to the EDN reader."
    :default {:default tagged-literal}
    :default-desc ""
    :parse-fn eval-string]
   ["-q" "--query <query>" "given a jet-lang query, transforms input. See doc/query.md for more."
    :parse-fn #(when %
                 (edn/read-string
                   {:readers *data-readers*}
                   (format "[%s]" %)))]
   ["-c", "--collect" "given separate values, collects them in a vector."]
   [nil "--interactive" "if present, starts an interactive shell. See README.md for more."]
   [nil "--interactive-cmd <initial-cmd>" "if present, starts an interactive shell with an initial command. See README.md for more."
    :id :interactive]])

(defn parse-opts [options]
  (let [parse-result (cli/parse-opts options cli-options)]
    (if-let [errors (:errors parse-result)]
      (do
        (run! println errors)
        (System/exit 1))
      (:options parse-result))))


(defn get-version
 "Gets the current version of the tool"
 []
 (str/trim (slurp (io/resource "JET_VERSION"))))

(defn print-help
  "Prints the help text"
  []
  (println (str "jet v" (get-version)))
  (println (:summary (cli/parse-opts nil cli-options)))
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
)
