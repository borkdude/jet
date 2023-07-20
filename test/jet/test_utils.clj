(ns jet.test-utils
  (:require
    [clojure.string :as str]
    [jet.main :as main]
    [me.raynes.conch :refer [let-programs] :as sh]))

(set! *warn-on-reflection* true)

(defn jet-jvm [input & args]
  (str/replace
    (with-out-str
      (with-in-str input
        (apply main/-main args)))
    "\r\n"
    "\n"))

(defn jet-native [input & args]
  (let-programs [jet "./jet"]
    (binding [sh/*throw* false]
      (apply jet (conj (vec args)
                       {:in input})))))

(def jet
  (case (System/getenv "JET_TEST_ENV")
    "jvm" #'jet-jvm
    "native" #'jet-native
    #'jet-jvm))

(if (= jet #'jet-jvm)
  (println "==== Testing JVM version")
  (println "==== Testing native version"))


