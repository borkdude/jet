(ns jet.parse-opts-test
  (:require
   [clojure.test :as test :refer [are deftest is testing]]
   [jet.main :refer [parse-opts]]))

(defn opts-match?
  "Compare `short` & `long` options."
  [short long]
  (= (parse-opts short)
     (parse-opts long)))

(defmacro opt-test
  "Helper macro to test command line options in a terser fashion."
  [{:keys [opt short-opt long-opt outcome]}]
  `(testing ~(name opt)
     (are [result opts] (= result (~opt (parse-opts opts)))
       ~outcome ~short-opt
       ~outcome ~long-opt)
     (is (true? (opts-match? ~short-opt ~long-opt)))))

(deftest parse-opts-test
  (testing "collect"
    (is (true? (:collect (parse-opts '("--collect"))))))
  (testing "edn-reader-opts"
    (is (nil? (:edn-reader-opts (parse-opts '("--edn-reader-opts" "nil"))))))
  (testing "interactive"
    (is (true? (:interactive (parse-opts '("--interactive"))))))
  (opt-test {:opt :collect
             :short-opt '("-c")
             :long-opt '("--collect")
             :outcome true})
  (opt-test {:opt :from
             :short-opt '("-i" "json")
             :long-opt '("--from" "json")
             :outcome :json})
  (opt-test {:opt :to
             :short-opt '("-o" "json")
             :long-opt '("--to" "json")
             :outcome :json})
  (opt-test {:opt :version
             :short-opt '("-v")
             :long-opt '("--version")
             :outcome true})
  (opt-test {:opt :help
             :short-opt '("-h")
             :long-opt '("--help")
             :outcome true})
  (opt-test {:opt :keywordize
             :short-opt '("-k")
             :long-opt '("--keywordize")
             :outcome true})
  (opt-test {:opt :func
             :short-opt '("-f" "nil")
             :long-opt '("--func" "nil")
             :outcome nil}))
