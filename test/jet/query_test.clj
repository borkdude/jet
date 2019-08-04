(ns jet.query-test
  (:require
   [clojure.test :as test :refer [deftest is]]
   [jet.query :refer [query]]))

(deftest query-test
  (is (= '{:a 1} (query {:a 1 :b 2} {:a true})))
  (is (= {:a #:a{:a 1}} (query {:a {:a/a 1 :a/b 2} :b 2} {:a {:a/a true}})))
  (is (= {:a [#:a{:a 1}]} (query {:a [{:a/a 1 :a/b 2}] :b 2} {:a {:a/a true}})))
  (is (= {:a 1, :c 3} (query {:a 1 :b 2 :c 3} {:jet.query/all true :b false})))
  (is (= [1] (query [1 2 3] '(take 1))))
  (is (= [2 3] (query [1 2 3] '(drop 1))))
  (is (= {:a [1]} (query {:a [1 2 3]} '{:a (take 1)})))
  (is (= {:a 2} (query {:a [1 2 3]} '{:a (nth 1)})))
  (is (= {:a 3} (query {:a [1 2 3]} '{:a (last)})))
  (is (= {:foo [:bar]} (query {:foo {:bar 2}} '{:foo (keys)})))
  (is (= {:foo [2]} (query {:foo {:bar 2}} '{:foo (vals)}))))
