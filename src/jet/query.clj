(ns jet.query
  {:no-doc true}
  (:refer-clojure :exclude [comparator])
  (:require [clojure.set :as set]))

(declare query)

(defn comparator [[c q v]]
  (let [c-f (case c
              = =
              < <
              <= <=
              >= >=)]
    #(c-f (query % q) v)))

(defn promote-function-query [q]
  (if (symbol? q)
    (list q)
    q))

(defn sexpr-query [x q]
  (let [op (first q)
        res (case op
              take (take (second q) x)
              drop (drop (second q) x)
              nth (try (nth x (second q))
                       (catch Exception _e
                         (last x)))
              keys (vec (keys x))
              vals (vec (vals x))
              first (first x)
              last (last x)
              map (map #(query % (let [f (second q)]
                                   (promote-function-query f))) x)
              juxt (vec (for [q (rest q)]
                          (if (symbol? q)
                            (sexpr-query x (list q))
                            (query x q))))
              map-vals (zipmap (keys x)
                               (map #(query % (second q)) (vals x)))
              zipmap (zipmap (first x) (second x))
              (filter remove) (let [op-f (case op
                                           filter filter
                                           remove remove)
                                    f (second q)
                                    c (if (list? f)
                                        (comparator f)
                                        #(query % f))]
                                (op-f c x))
              count (count x)
              select-keys (select-keys x (second q))
              dissoc (apply dissoc x (rest q))
              rename-keys (set/rename-keys x (second q))
              quote (second q)
              hash-map (let [args (rest q)
                             keys (take-nth 2 args)
                             vals (take-nth 2 (rest args))
                             vals (map #(query x %) vals)]
                         (zipmap keys vals))
              assoc (let [args (rest q)
                          keys (take-nth 2 args)
                          vals (take-nth 2 (rest args))
                          vals (map #(query x %) vals)]
                      (merge x (zipmap keys vals)))
              update (let [[k update-query] (rest q)
                           update-query (promote-function-query update-query)
                           v (get x k)]
                       (assoc x k (query v update-query)))
              assoc-in (let [[path assoc-in-query] (rest q)
                             v (query x assoc-in-query)]
                         (assoc-in x path v))
              update-in (let [[path update-in-query] (rest q)
                              update-in-query (promote-function-query update-in-query)
                             v (get-in x path)
                             v (query v update-in-query)]
                         (assoc-in x path v))
              x)]
    (if (and (vector? x) (sequential? res))
      (vec res)
      res)))

(defn nested-query [x q]
  (reduce-kv
   (fn [m k v]
     (if (and v (contains? x k))
       (assoc m k (query (get x k) v))
       m))
   x q))

(defn query
  [x q]
  (cond
    (not q) nil
    (set? q) (if (map? x)
               (select-keys x q)
               (mapv #(select-keys % q) x))
    (vector? q) (if-let [next-op (first q)]
                  (recur (query x next-op) (vec (rest q)))
                  x)
    (list? q) (sexpr-query x q)
    (sequential? x)
    (mapv #(query % q) x)
    (map? q)
    (nested-query x q)
    (map? x) (get x q)))

;;;; Scratch

(comment
  (query {:a 1 :b 2} {:a true})
  (query {:a {:a/a 1 :a/b 2} :b 2} {:a {:a/a true}})
  (query {:a [{:a/a 1 :a/b 2}] :b 2} {:a {:a/a true}})
  (query {:a 1 :b 2 :c 3} {:jet.query/all true :b false})
  (query [1 2 3] '(take 1))
  (query [1 2 3] '(drop 1))
  (query {:a [1 2 3]} '{:a (take 1)})
  (query {:a [1 2 3]} '{:a (nth 1)})
  (query {:a [1 2 3]} '{:a (last)})
  )
