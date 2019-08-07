(ns jet.query-test
  (:require
   [clojure.test :as test :refer [deftest is]]
   [jet.query :refer [query]]))

(deftest query-test
  ;; TODO: rewrite with clojure.test/are...
  (is (= nil (query [] false)))
  (is (= '1 (query {:a 1 :b 2} :a)))
  (is (= '1 (query {1 1} 1)))
  (is (= '1 (query {"1" 1} "1")))
  (is (= '{:a 1} (query {:a 1 :b 2} '(select-keys [:a]))))
  (is (= {:a #:a{:a 1}} (query {:a {:a/a 1 :a/b 2} :b 2} '[(select-keys [:a]) (update :a (select-keys [:a/a]))])))
  (is (= {:a [#:a{:a 1}]} (query {:a [{:a/a 1 :a/b 2}] :b 2} '[(select-keys [:a]) (update :a (map (select-keys [:a/a])))])))
  (is (= {:a 1, :c 3} (query {:a 1 :b 2 :c 3} '(dissoc :b))))
  (is (= [1] (query [1 2 3] '(take 1))))
  (is (= [2 3] (query [1 2 3] '(drop 1))))
  (is (= {:a [1]} (query {:a [1 2 3]} '(update :a (take 1)))))
  (is (= {:a 2} (query {:a [1 2 3]} '(update :a (nth 1)))))
  (is (= {:a 1} (query {:a [1 2 3]} '(update :a first))))
  (is (= 3 (query {:a [1 2 3]} '(last :a))))
  (is (= {:a 3} (query {:a [1 2 3]} '(update :a last))))
  (is (= {:foo [:bar]} (query {:foo {:bar 2}} '(update :foo keys))))
  (is (= {:a [:a :b]} (query {:b {:a 1 :b 2}} '{:a (keys :b)})))
  (is (= {:foo [2]} (query {:foo {:bar 2}} '(update :foo vals))))
  (is (= [3 6] (query [[1 2 3] [4 5 6]] '(map last))))
  (is (= [1 2] (query [{:a 1} {:a 2}] '(map :a))))
  (is (= [1 2] (query [{:a 1} {:a 2}] '(map :a))))
  (is (= [1 2] (query {:a 1 :b 2 :c 3} '[(dissoc :c) (vals)])))
  (is (= {:foo 1 :bar 1}
         (query {:foo {:a 1 :b 2} :bar {:a 1 :b 2}} '(map-vals :a))))
  (is (= {:a 1 :b 2 :c 3}
         (query {:keys [:a :b :c] :vals [1 2 3]} '[(juxt :keys :vals) (zipmap)])))
  (is (= '[{:name foo :private true}]
         (query '[{:name foo :private true}
                  {:name bar :private false}] '(filter :private))))
  (is (= 1 (query '[{:name foo :private true}
                    {:name bar :private false}] '[(filter :private) (count)])))
  (is (= '[{:name foo, :private true}]
         (query '[{:name foo :private true}
                  {:name bar :private false}] '(filter (= :name (quote foo))))))
  (is (= '[{:a 2} {:a 3}]
         (query '[{:a 1} {:a 2} {:a 3}] '(filter (>= :a (quote 2))))))
  (is (= '[{:a 1} {:a 2}]
         (query '[{:a 1} {:a 2} {:a 3}] '(filter (<= :a (quote 2))))))
  (is (= '[{:a 1} {:a 2}]
         (query '[{:a 1} {:a 2} {:a 3}] '(filter (not= :a (quote 3))))))
  (is (= '[{:a 1} {:a 2}]
         (query '[{:a 1} {:a 2} {:a 3}] '(remove (= :a (quote 3))))))
  (is (= '[{:a 1}]
         (query '[{:a 1} [] []] '(filter first))))
  (is (= false (query {:a 1 :b 1 :c 1} '(not= :a :b :c))))
  (is (= '{:a 1 :b 2}
         (query '{:a 1 :b 2 :c 3} '(select-keys [:a :b]))))
  (is (= '{:c 3}
         (query '{:a 1 :b 2 :c 3} '(dissoc :a :b))))
  (is (= '{:b 1}
         (query '{:a 1} '(rename-keys {:a :b}))))
  (is (= '{:foo 1 :bar 2}
         (query '{:a 1 :b 2} '(hash-map :foo :a :bar :b))))
  (is (= '{:a 1 :b 3}
         (query '{:a 1 :b 2} '(assoc :b (quote 3)))))
  (is (= '{:a 1 :b 3}
         (query '{:a 1 :b 2} '(assoc :b #jet/lit 3))))
  (is (= '{:a 1}
         (query nil '(hash-map :a (quote 1)))))
  (is (= '{:a 1}
         (query nil '{:a (quote 1)})))
  (is (= '{:a 1 :b 1}
         (query {:a 1} '(assoc :b :a))))
  (is (= '{:a 1}
         (query {:a {:b 1}} '(update :a :b))))
  (is (= '{:a 1}
         (query {:a [1 2 3]} '(update :a first))))
  (is (= '{:a {:a 2} :b 2}
         (query {:a {:a 1} :b 2} '(assoc-in [:a :a] :b))))
  (is (= '{:a {:a 1}}
         (query {:a {:a {:b 1}}} '(update-in [:a :a] :b))))
  (is (= '{:a {:a 1}}
         (query {:a {:a [1 2 3]}} '(update-in [:a :a] first))))
  (is (= 1 (query {:a 1} '(get :a))))
  (is (= 100 (query [100 1 2] '(get 0))))
  (is (= 1 (query (list 1 2 3) '0)))
  (is (= [1 2 3] (query {:a [1 1 2 2 3 3 1 1]} '[:a (distinct)])))
  (is (= [1 2 3 1] (query {:a [1 1 2 2 3 3 1 1]} '[:a (dedupe)])))
  (is (= "foo bar" (query {:a "foo bar" :b 2} '(if (re-find #jet/lit "foo" :a) :a :b))))
  (is (= 2 (query {:a "foo bar" :b 2} '(if (re-find #jet/lit "baz" :a) :a :b))))
  (is (= "1/2" (query {:a 1 :b 2} '(str :a #jet/lit "/" :b))))
  (is (= {:input {:a 3, :b 2}, :product 6}
         (query {:a 3 :b 2} '{:input (identity) :product (* :a :b)})))
  (is (= 2 (query {:a 3 :b 2} '(identity :b))))
  (is (= nil (query {:b 3} '(and :a :b))))
  (is (= false (query {:a false} '(and :a :b))))
  (is (= 2 (query {:a 1 :b 2} '(and :a :b))))
  (is (= 3 (query {:b 3} '(or :a :b))))
  (is (= true (query {:b 3} '(not :a))))
  (is (= 4 (query {:b 3} '(inc :b))))
  (is (= 2 (query {:b 3} '(dec :b))))
  (is (= 3 (query [1 2 3] 'last)))
  (is (= 4 (query '{(inc :a) 4} '(inc :a))))
  (is (= {:a 1 :b 2} (query {:a 1 :b 2 :c 3 :d 4} '($ :a :b)))))
