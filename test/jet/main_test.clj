(ns jet.main-test
  (:require
   [clojure.test :as test :refer [deftest is testing]]
   [jet.main :as main]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]))

(set! *warn-on-reflection* true)

(defn jet-jvm [input & args]
  (with-out-str
    (with-in-str input
      (apply main/-main args))))

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

(if (= jet jet-jvm)
  (println "==== Testing JVM version")
  (println "==== Testing native version"))

(deftest json->edn-test
  (is (= "{\"a\" 1}\n"
         (jet "{\"a\": 1}"
              "--from" "json"
              "--to" "edn")))
  (is (= "{:a 1}\n"
         (jet "{\"a\": 1}"
              "--from" "json"
              "--keywordize" "true"
              "--to" "edn")))
  (is (= "[\"^ \",\"~:a\",1]\n"
         (jet "{\"a\": 1}"
              "--from" "json"
              "--keywordize" "true"
              "--to" "transit")))
  (is (= "{\"a\":1}\n"
         (jet "{:a 1}"
              "--from" "edn"
              "--to" "json")))
  (is (= "[\"^ \",\"~:a\",1]\n"
         (jet "{:a 1}"
              "--from" "edn"
              "--to" "transit")))
  (is (= "{:a 1}\n"
         (jet "[\"^ \",\"~:a\",1]\n"
              "--from" "transit"
              "--to" "edn")))
  (is (= "{\"a\":1}\n"
         (jet "[\"^ \",\"~:a\",1]\n"
              "--from" "transit"
              "--to" "json"))))
