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

(if (= jet #'jet-jvm)
  (println "==== Testing JVM version")
  (println "==== Testing native version"))

(deftest main-test
  (is (= "{\"a\" 1}\n"
         (jet "{\"a\": 1}"
              "--from" "json"
              "--to" "edn")))
  (is (= "{:a 1}\n"
         (jet "{\"a\": 1}"
              "--from" "json"
              "--keywordize"
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
              "--to" "json")))
  (testing "pretty printing"
    (is (= "{\n  \"a\" : [ {\n    \"b\" : {\n      \"c\" : \"d\"\n    }\n  } ]\n}\n"
           (jet "{:a [{:b {:c :d}}]}" "--from" "edn" "--to" "json" "--pretty")))
    (is (= "{:a [{:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}]}\n"
           (jet "{:a [{:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}}]}" "--from" "edn" "--to" "edn" "--pretty")))))
