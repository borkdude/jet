(ns jet.main-test
  (:require
   [clojure.test :as test :refer [deftest is testing]]
   [jet.test-utils :refer [jet]]
   [clojure.string :as str]))

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
           (jet "{:a [{:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}}]}" "--from" "edn" "--to" "edn" "--pretty"))))
  (testing "query"
    (is (= "1\n" (jet "{:a 1 :b 2}" "--from" "edn" "--to" "edn" "--query" ":a"))))
  (testing "from and to default to edn"
    (is (= "1\n" (jet "{:a 1 :b 2}" "--query" ":a"))))
  (testing "implicity wrapping multiple queries"
    (is (= "1\n" (jet "{:a {:b 1}}" "--query" ":a :b")))))

(deftest interactive-test
  (testing "passing correct query will please jeti"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" [{:a 1} "Y" :a "Y"]) "--interactive"))))
  (testing "passing --interactive arg as edn"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" ["Y" :a "Y"]) "--interactive" "{:a 1}"))))
  (testing "passing --interactive arg as edn"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" ["Y" :a "Y"]) "--interactive" ":jeti/set-val {:a 1}"))))
  (testing "slurping json file"
    (is (re-find #"[0-9a-f]{4}> 30"
                 (jet (str/join "\n" ["Y" "count" "Y"]) "--interactive" ":jeti/slurp test/data/commits.json {:format :json}"))))
  (testing "jeti doesn't get stuck in a loop and executes the command only once"
    (is (re-find #"Available commands"
                 (jet "" "--interactive" ":jeti/help")))))

(deftest stream-test
  (is (= "2\n3\n4\n" (jet "2 3 4")))
  (is (= "2\n3\n4\n" (jet "{:x 2} {:x 3} {:x 4}" "--query" ":x")))
  (is (= "2\n3\n4\n" (jet "{\"a\":2} {\"a\":3} {\"a\":4}" "--from" "json" "--keywordize" "--query" ":a")))
  (is (= "2\n3\n4\n" (jet "[\"^ \",\"~:a\",2] [\"^ \",\"~:a\",3] [\"^ \",\"~:a\",4]" "--from" "transit" "--query" ":a"))))

(deftest collect-test
  (is (= "[{:x 2} {:x 3} {:x 4}]\n" (jet "{:x 2} {:x 3} {:x 4}" "--collect"))))

(deftest key-fn-test
  (is (= "[{:x 2} {:x 3} {:x 4}]\n"
         (jet "{\" x \": 2} {\" x \": 3} {\" x \": 4}" "--collect" "--from" "json" "--keywordize" "(comp keyword str/trim)"))))

(deftest edn-reader-opts-test
  (is (= "#foo {:a 1}\n" (jet "#foo{:a 1}" "--edn-reader-opts" "{:default tagged-literal}")))
  (is (= "[:foo {:a 1}]\n" (jet "#foo{:a 1}" "--edn-reader-opts" "{:readers {'foo (fn [x] [:foo x])}}"))))
