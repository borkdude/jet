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
     :help help}))

(defn get-version
 "Gets the current version of the tool"
 []
 (str/trim (slurp (io/resource "JET_VERSION"))))

(defn get-usage
  "Gets the usage of the tool"
  []
  (str "Usage: jet [ --from <format> ] [ --to <format> ] [ --keywordize [ <key-fn> ] ] [ --pretty ] [--query <query> ] [ --collect ] | [ --interactive <cmd> ]"))

(defn print-help
  "Prints the help text"
  []
  (println (str "jet v" (get-version)))
  (println)
  (println (get-usage))
  (println)
  (println "
  --help: print this help text.
  --version: print the current version of jet.
  --from: edn, transit or json, defaults to edn.
  --to: edn, transit or json, defaults to edn.
  --keywordize [ <key-fn> ]: if present, keywordizes JSON keys. The default transformation function is keyword unless you provide your own.
  --pretty: if present, pretty-prints JSON and EDN output.
  --query: given a jet-lang query, transforms input. See doc/query.md for more.
  --collect: given separate values, collects them in a vector.
  --interactive [ cmd ]: if present, starts an interactive shell. An initial command may be provided. See README.md for more."))

(defn -main
  [& args]
  (let [{:keys [:from :to :keywordize
                :pretty :version :query
                :interactive :collect :help]} (parse-opts args)]
    (cond version
          (println (get-version))
          interactive (start-jeti! interactive)
          help (print-help)
          :else
          (let [reader (case from
                         :json (formats/json-parser)
                         :transit (formats/transit-reader)
                         (:edn :edn+hiccup :edn+hickory :html :html-fragment) nil)
                next-val (case from
                           (:edn :edn+hiccup :edn+hickory) #(formats/parse-edn *in*)
                           :html #(formats/parse-html-document *in*)
                           :html-fragment #(formats/parse-html-fragment *in*)
                           :json #(formats/parse-json reader keywordize)
                           :transit #(formats/parse-transit reader))
                collected (when collect (vec (take-while #(not= % ::formats/EOF)
                                                         (repeatedly next-val))))]
            (reset! formats/html-read? false)
            (loop []
              (let [input (if collect collected (next-val))]
                (def i input)
                (when-not (identical? ::formats/EOF input)
                  (let [input (if query (q/query input query)
                                  input)]
                    (case from
                      (:html :html-fragment)
                      (case to
                        (:edn :edn+hiccup) (println (formats/generate-hiccup input pretty))
                        :edn+hickory (println (formats/generate-hickory input pretty)))
                      (case to
                        :edn (println (formats/generate-edn input pretty))
                        :json (println (formats/generate-json input pretty))
                        :transit (println (formats/generate-transit input))
                        :html (println (formats/generate-html input)))))
                  (when-not collect (recur)))))))))

;;;; Scratch

(comment
  (with-out-str
    (with-in-str "[1 2 3]"
      (-main "--from" "edn" "--to" "json")))
  (with-out-str
    (with-in-str "<a href=\"foo\">hello</a>"
      (-main "--from" "html-fragment" "--to" "edn+hiccup")))
  (with-out-str
    (with-in-str "<html><body><a href=\"foo\">hello</a></body></html>"
      (-main "--from" "html" "--to" "edn+hiccup")))
  i
)
