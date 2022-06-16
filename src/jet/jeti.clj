(ns jet.jeti
  {:no-doc true}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [jet.formats :as formats]
   [jet.query :refer [query]]))

(set! *warn-on-reflection* true)

(defn new-id [state]
  (let [uuid (subs (str (java.util.UUID/randomUUID)) 0 4)]
    (if (contains? state uuid)
      (recur state)
      uuid)))

(defn print-help [sub-command]
  (if sub-command
    (case sub-command
      :jeti/slurp
      (println ":jeti/slurp <file> [{:format ..., :keywordize ...}]: slurps a file into the shell from disk. :format is one of :json, :edn or :transit (defaults to :edn) and :keywordize is a boolean that only applies to :json.")
      :jeti/spit
      (println ":jeti/spit <file> [{:format ..., :pretty ...}]: spits a file to disk. :format is one of :json, :edn or :transit (defaults to :edn) and :pretty is a boolean that indicates if the output should be pretty printed.")
      (println "Help for" sub-command "not found."))
    (do (println "Available commands:")
        (println ":jeti/set-val {:a 1}   : set value.")
        (println ":jeti/jump \"34d4\"      : jump to a previous state.")
        (println ":jeti/quit, :jeti/exit : exit this shell.")
        (println ":jeti/slurp            : read a file from disk. Type :jeti/help :jeti/slurp for more details.")
        (println ":jeti/spit             : writes file to disk. Type :jeti/help :jeti/spit for more details.")
        (println ":jeti/bookmark \"name\"  : save a bookmark.")
        (println ":jeti/bookmarks        : show bookmarks.")
        (println ":jeti/print-length     : set *print-length*")
        (println ":jeti/print-level      : set *print-level*")
        (println))))

(defn jeti-set! [state current-id init-val]
  (try (let [next-id (new-id state)]
         {:state (assoc state next-id init-val)
          :next-id next-id})
       (catch Exception e
         (println "Could not read" format init-val e)
         {:state state
          :next-id current-id})))

(defn read-file [state current-id file {:keys [:format :keywordize]
                                        :or {format :edn}}]
  (try (let [next-id (new-id state)
             file-as-string (slurp (str file))
             file-as-edn (case format
                           :edn (with-in-str file-as-string (formats/parse-edn nil *in*))
                           :transit (with-in-str file-as-string (formats/parse-transit
                                                                 (formats/transit-reader)))
                           :json (with-in-str file-as-string
                                   (formats/parse-json (formats/json-parser) keywordize)))]
         {:state (assoc state next-id file-as-edn)
          :next-id next-id})
       (catch Exception e
         (println "Could not read" format file e)
         {:state state
          :next-id current-id})))

(defn write-file [value file {:keys [:format :pretty]
                              :or {format :edn}}]
  (try (let [out-string (case format
                          :edn (formats/generate-edn value pretty true)
                          :transit (formats/generate-transit value)
                          :json (formats/generate-json value pretty))]
         (spit file out-string))
       (catch Exception e
         (println "Could not write to" (str file ":") (.getMessage e)))))

(defn start-jeti! [init-cmd colors]
  (println "Welcome to jeti. The answer is just a few queries away!")
  (println "Running jet" (str "v" (str/trim (slurp (io/resource "JET_VERSION"))) "."))
  (println "Type :jeti/help to print help.")
  (println)
  (let [init-cmd (when (string? init-cmd) init-cmd)
        init-id (new-id nil)]
    (loop [{:keys [:bookmarks :print-level :print-length :init-cmd] :as state}
           {:init-cmd init-cmd
            init-id ::start
            :bookmarks []
            :print-length 5
            :print-level 5}
           previous-id nil
           current-id init-id]
      (let [current-val (get state current-id)
            start? (identical? ::start current-val)
            same? (= current-id previous-id)
            _ (when (and (not start?)
                         (not same?))
                (println (binding [*print-length* print-length
                                   *print-level* print-level]
                           (formats/generate-edn current-val true colors)))
                (println))
            proceed? (cond (and start? init-cmd)
                           (do (println ">" init-cmd)
                               true)
                           same? false
                           start? true
                           :else
                           (= "Y" (str/trim
                                   (do
                                     (print "Type Y to enter this state. ")
                                     (flush)
                                     (read-line)))))
            current-id (cond start? current-id
                             proceed? current-id
                             :else previous-id)
            current-val (get state current-id)
            start-val? (identical? ::start current-val)
            bookmark-name (some #(when (= current-id (:id %))
                                   (:name %))
                                bookmarks)]
        (when-not (and init-cmd start?)
          (print (str (when-not start-val? current-id)
                      (when bookmark-name
                        (format "(%s)" bookmark-name))
                      "> ")))
        (flush)
        (when-let [q (if (and start? init-cmd)
                       init-cmd
                       (read-line))]
          (let [state (dissoc state :init-cmd)
                [fst :as q] (try (edn/read-string
                                  {:readers *data-readers*}
                                  (format "[%s]" q))
                                 (catch Exception e
                                   (println "Invalid input:" (.getMessage e))
                                   nil))
                [cmd & opts] (when fst
                               (when
                                   (and (keyword? fst)
                                        (= "jeti" (namespace fst)))
                                 q))
                first-opts (first opts)]
            (cond (not q)
                  (recur state current-id current-id)
                  cmd
                  (case cmd
                    :jeti/jump
                    (let [id first-opts]
                      (cond (not (string? id))
                            (do
                              (println ":jeti/jump expects a string")
                              (recur state current-id current-id))
                            (contains? state id)
                            (recur state current-id id)
                            :else
                            (do
                              (println id "is not a valid state.")
                              (recur state current-id current-id))))
                    :jeti/help
                    (do (print-help first-opts)
                        (recur state current-id current-id))
                    (:jeti/quit :jeti/exit) (println "Goodbye for now!")
                    :jeti/set-val (let [{:keys [:state :next-id]}
                                        (jeti-set! state current-id first-opts)]
                                    (recur state current-id next-id))
                    :jeti/slurp
                    (let [{:keys [:state :next-id]}
                          (read-file state current-id first-opts (second opts))]
                      (recur state current-id next-id))
                    :jeti/spit
                    (do (write-file current-val first-opts (second opts))
                        (recur state current-id current-id))
                    :jeti/bookmark
                    (let [bookmark-name first-opts
                          bookmark {:id current-id
                                    :name bookmark-name}
                          next-state (update state :bookmarks conj bookmark)]
                      (recur next-state current-id current-id))
                    :jeti/bookmarks
                    (do (doseq [{:keys [:name :id]} (:bookmarks state)]
                          (println (str id ": " name)))
                        (recur state current-id current-id))
                    :jeti/print-length
                    (recur (assoc state :print-length
                                  (if (number? first-opts) first-opts
                                      (do (println "You didn't enter a number:" first-opts)
                                          print-length)))
                           current-id current-id)
                    :jeti/print-level
                    (recur (assoc state :print-level
                                  (if (number? first-opts) first-opts
                                      (do (println "You didn't enter a number:" first-opts)
                                          print-level)))
                           current-id current-id)
                    :jeti/debug (do (prn state)
                                    (recur state current-id current-id))
                    (do
                      (println "I did not understand your command.")
                      (recur state current-id current-id)))
                  :else
                  (let [next-input (try (if start?
                                          (first q)
                                          (query current-val q))
                                        (catch Exception e
                                          (println "Error while executing query:"
                                                   (.getMessage e))
                                          ::error))
                        [next-id next-input] (if (identical? ::error next-input)
                                               [current-id current-val]
                                               [(new-id state) next-input])]
                    (recur (assoc state next-id next-input)
                           current-id next-id)))))))))
