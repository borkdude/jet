(ns jet.main-test
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :as test :refer [deftest is testing]]
   [jet.test-utils :refer [jet]]))

(def named-keywordize-json "{\"onceMoreUntoTheBreach\": \"dear friends\"}")
(deftest named-camel-keywordize-test
  (is (= "{:onceMoreUntoTheBreach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":camelCase")
         )))
(deftest named-pascal-keywordize-test
  (is (= "{:OnceMoreUntoTheBreach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":PascalCase"))))

(deftest named-snake-keywordize-test
  (is (= "{:once_more_unto_the_breach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":snake_case"))))

(deftest named-kebab-keywordize-test
  (is (= "{:once-more-unto-the-breach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":kebab-case"))))

(deftest named-screaming-snake-keywordize-test
  (is (= "{:ONCE_MORE_UNTO_THE_BREACH \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":SCREAMING_SNAKE_CASE"))))

(deftest named-http-header-keywordize-test
  (is (= "{:Once-More-Unto-The-Breach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":HTTP-Header-Case"))))

(deftest named-camel-snake-keywordize-test
  (is (= "{:Once_More_Unto_The_Breach \"dear friends\"}\n"
         (jet named-keywordize-json
              "--from" "json"
              "--keywordize" ":Camel_Snake_Case"))))

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
              "--to" "json" "--no-pretty")))
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
              "--to" "json" "--no-pretty")))

  (is (= "{\"a\" 1}\n"
         (jet "{a: 1}"
              "--from" "yaml"
              "--to" "edn")))
  ;; YAML coll should convert to EDN vector
  (is (= "[1 2]\n"
         (jet "- 1\n- 2\n\n"
              "--from" "yaml"
              "--to" "edn")))
  (is (= "{:a 1}\n"
         (jet "a: 1"
              "--from" "yaml"
              "--keywordize"
              "--to" "edn")))
  (is (= "{:a-b 1}\n"
         (jet "a_b: 1"
              "--from" "yaml"
              "--keywordize"
              "#(keyword (str/replace % \"_\" \"-\"))"
              "--to" "edn")))
  (is (= "{:x/a 1}\n"
         (jet "a: 1"
              "--from" "yaml"
              "--keywordize"
              "#(keyword \"x\" %)"
              "--to" "edn")))
  (is (= "[\"^ \",\"~:a\",1]\n"
         (jet "{a: 1}"
              "--from" "yaml"
              "--keywordize" "true"
              "--to" "transit")))
  (is (= "{\"a\":1}\n"
         (jet "{a: 1}"
              "--from" "yaml"
              "--to" "json" "--no-pretty")))
  (is (= "[\"^ \",\"a\",1]\n"
         (jet "{a: 1}"
              "--from" "yaml"
              "--to" "transit")))
  (is (= "a: 1\n\n"
         (jet "[\"^ \",\"~:a\",1]\n"
              "--from" "transit"
              "--to" "yaml")))
  (is (= "{a: 1}\n\n"
         (jet "[\"^ \",\"~:a\",1]\n"
              "--from" "transit"
              "--to" "yaml" "--no-pretty")))
  (is (= "a: 1\n\n"
         (jet "{:a 1}\n"
              "--from" "edn"
              "--to" "yaml")))
  (is (= "a: 1\n\n"
         (jet "{\"a\":1}\n"
              "--from" "json"
              "--to" "yaml")))

  (testing "pretty printing"
    (is (= "{\n  \"a\" : [ {\n    \"b\" : {\n      \"c\" : \"d\"\n    }\n  } ]\n}\n"
           (jet "{:a [{:b {:c :d}}]}" "--from" "edn" "--to" "json" "--pretty")))
    (is (= "{:a [{:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}\n     {:b {:c :d}}]}\n"
           (jet "{:a [{:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}} {:b {:c :d}}]}" "--no-colors" "--from" "edn" "--to" "edn" "--pretty"))))
  (testing "query"
    (is (= "1\n" (jet "{:a 1 :b 2}" "--from" "edn" "--to" "edn" "--query" ":a"))))
  (testing "from and to default to edn"
    (is (= "1\n" (jet "{:a 1 :b 2}" "--query" ":a"))))
  (testing "implicity wrapping multiple queries"
    (is (= "1\n" (jet "{:a {:b 1}}" "--query" ":a :b")))))

(deftest interactive-test
  (testing "passing correct query will please jeti"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" [{:a 1} "Y" :a "Y"]) "--interactive" "--no-colors"))))
  (testing "passing --interactive arg as edn"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" ["Y" :a "Y"]) "--no-colors" "--interactive" "{:a 1}"))))
  (testing "passing --interactive arg as edn"
    (is (re-find #"[0-9a-f]{4}> 1"
                 (jet (str/join "\n" ["Y" :a "Y"]) "--no-colors" "--interactive" ":jeti/set-val {:a 1}"))))
  (testing "slurping json file"
    (is (re-find #"[0-9a-f]{4}> 30"
                 (jet (str/join "\n" ["Y" "count" "Y"]) "--no-colors" "--interactive" ":jeti/slurp test/data/commits.json {:format :json}"))))
  (testing "jeti doesn't get stuck in a loop and executes the command only once"
    (is (re-find #"Available commands"
                 (jet "" "--no-colors" "--interactive" ":jeti/help")))))

(deftest stream-test
  (is (= "2\n3\n4\n" (jet "2 3 4" "--from" "edn" "--to" "edn")))
  (is (= "2\n3\n4\n" (jet "{:x 2} {:x 3} {:x 4}" "--query" ":x")))
  (is (= "2\n3\n4\n" (jet "{\"a\":2} {\"a\":3} {\"a\":4}" "--from" "json" "--keywordize" "--query" ":a")))
  (is (= "2\n3\n4\n" (jet "[\"^ \",\"~:a\",2] [\"^ \",\"~:a\",3] [\"^ \",\"~:a\",4]" "--from" "transit" "--query" ":a"))))

(deftest collect-test
  (is (= "[{:x 2} {:x 3} {:x 4}]\n" (jet "{:x 2} {:x 3} {:x 4}" "--collect"))))

(deftest key-fn-test
  (is (= "[{:x 2} {:x 3} {:x 4}]\n"
         (jet "{\" x \": 2} {\" x \": 3} {\" x \": 4}" "--collect" "--from" "json" "--keywordize" "(comp keyword str/trim)")))
  (let [casing-samples "{\"PascalCase\":1}
                        {\"camelCase\":2}
                        {\"SCREAMING_SNAKE_CASE\":3}
                        {\"snake_case\":4}
                        {\"kebab-case\":5}
                        {\"Camel_Snake_Case\":6}
                        {\"HTTP-Header-Case\":7}"]
    (is (= "{:pascal-case 1}\n{:camel-case 2}\n{:screaming-snake-case 3}\n{:snake-case 4}\n{:kebab-case 5}\n{:camel-snake-case 6}\n{:http-header-case 7}\n"
           (jet casing-samples "--from" "json" "--keywordize" "#(-> % csk/->kebab-case keyword)")))
    (is (= "{:pascal_case 1}\n{:camel_case 2}\n{:screaming_snake_case 3}\n{:snake_case 4}\n{:kebab_case 5}\n{:camel_snake_case 6}\n{:http_header_case 7}\n"
           (jet casing-samples "--from" "json" "--keywordize" "#(-> % csk/->snake_case keyword)")))))

(deftest edn-reader-opts-test
  (is (= "#foo {:a 1}\n" (jet "#foo{:a 1}" "--no-pretty")))
  (is (= "#foo {:a 1}\n" (jet "#foo{:a 1}" "--no-pretty" "--edn-reader-opts" "{:default tagged-literal}")))
  (is (= "[:foo {:a 1}]\n" (jet "#foo{:a 1}" "--no-pretty" "--edn-reader-opts" "{:readers {'foo (fn [x] [:foo x])}}"))))

(deftest func-test
  (is (= "1\n" (jet "{:a {:b {:c 1}}}" "--func" "#(-> % :a :b :c)")))
  (testing "when function is in a file"
    (is (= "1\n" (jet "{:a {:b {:c 1}}}" "--func" "test-resources/fn.clj")))))

(deftest thread-first-test
  (is (= "3\n"
         (jet "1" "-T" "inc inc"))))

(deftest thread-last-test
  (is (= "{:a {:a 2}}\n"
         (jet "{:a {:a 1}}" "-t" "(s/transform [s/MAP-VALS s/MAP-VALS] inc)"))))

(deftest specter-test
  (is (= "[{:a {:aa 2}, :b {:ba 0, :bb 3}} [3 3 18 6 12] [2 1 3 6 10 4 8] {:a [1 2 3]} {:a {:b {}}} {} [0 2 2 4 4 5 6 7] [0 1 :a :b :c :d :e 4 5 6 7 8 9] [[1 :a :b] (1 2 :a :b) [:c :a :b]] [2 1 2 6 7 4 1 2] [10] [0 1 2 3 10 5 8 7 6 9 4 11 12 13 14 15] [[1 2 3 4 5 6 :c :d] [7 0 -1] [8 8 :c :d] []] [{:a 1, :b 3} {:a -8, :b -10} {:a 14, :b 10} {:a 3}] {:a 11, :b 3} [{:a 2, :c [2 3], :d 4} {:a 4, :c [1 11 0]} {:a -1, :c [1 1 1], :d 2}]]\n"
         (jet "{:a {:a 1}}" "--no-pretty" "-f" "test-resources/specter-test.clj"))))

(deftest base64-test
  (is (= "\"Zm9v\"\n" (jet "{:a \"foo\"}" "-t" ":a base64/encode")))
  (is (= "\"foo\"\n" (jet "{:a \"foo\"}" "-t" ":a base64/encode base64/decode"))))

(deftest paths-test
  (is (= [[:a :b 0] [:a :b 2] [:a :c :d]]
         (edn/read-string (jet "{:a {:b [1 2 3 {:x 2}] :c {:d 3}}}" "-t" "(jet/paths) (filter (comp (jet/when-pred odd?) :val)) (mapv :path)")))))

(deftest no-commas-test
  (is (= "[{:a 1 :b 2}]\n"
         (jet "[{:a 1 :b 2}]" "--no-commas"))))

(deftest update-keys-and-vals-test
  (is (= "{:a 1}\n"
         (jet "{\"a\" \"1\"}" "-T" "(update-vals parse-long) (update-keys keyword)"))))

(deftest parse-functions-test
  (is (= "1\n" (jet "\"1\"" "-t" "parse-long")))
  (is (= "true\n" (jet "\"true\"" "-t" "parse-boolean")))
  (is (= "0.5\n" (jet "\"0.5\"" "-t" "parse-double")))
  (is (= "#uuid \"00000000-0000-0000-0000-000000000000\"\n" (jet "\"00000000-0000-0000-0000-000000000000\"" "-t" "parse-uuid"))))