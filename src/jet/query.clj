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
              >= >=
              not= not=)]
    #(c-f (query % q) v)))

(defn promote-function-query [q]
  (if (symbol? q)
    (list q)
    q))

(defn create-map [x args]
  (let [keys (take-nth 2 args)
        vals (take-nth 2 (rest args))
        vals (map #(query x %) vals)]
    (zipmap keys vals)))

(defn safe-nth [x n]
  (try (nth x n)
       (catch Exception _e
         nil)))

(defn sexpr-query [x q]
  (let [[op & args] q
        [arg1 arg2 arg3] args
        res (case op
              take (take arg1 x)
              drop (drop arg1 x)
              nth (safe-nth x arg1)
              keys (vec (keys x))
              vals (vec (vals x))
              first (first x)
              last (last x)
              map (map #(query % (let [f arg1]
                                   (promote-function-query f))) x)
              juxt (vec (for [q args]
                          (if (symbol? q)
                            (sexpr-query x (list q))
                            (query x q))))
              map-vals (zipmap (keys x)
                               (map #(query % arg1) (vals x)))
              zipmap (zipmap (first x) (second x))
              (filter remove) (let [op-f (case op
                                           filter filter
                                           remove remove)
                                    f arg1
                                    c (if (list? f)
                                        (comparator f)
                                        #(query % f))]
                                (op-f c x))
              count (count x)
              select-keys (select-keys x args)
              dissoc (apply dissoc x (rest q))
              rename-keys (set/rename-keys x arg1)
              quote arg1
              hash-map (create-map x args)
              assoc (let [args args
                          keys (take-nth 2 args)
                          vals (take-nth 2 (rest args))
                          vals (map #(query x %) vals)]
                      (merge x (zipmap keys vals)))
              update (let [[k update-query] args
                           update-query (promote-function-query update-query)
                           v (get x k)]
                       (assoc x k (query v update-query)))
              assoc-in (let [[path assoc-in-query] args
                             v (query x assoc-in-query)]
                         (assoc-in x path v))
              update-in (let [[path update-in-query] args
                              update-in-query (promote-function-query update-in-query)
                             v (get-in x path)
                             v (query v update-in-query)]
                          (assoc-in x path v))
              get (get x arg1)
              distinct (distinct x)
              dedupe (dedupe x)
              str (apply str (map #(query x %) args))
              re-find (re-find (re-pattern arg1) (query x arg2))
              if (if (query x arg1) (query x arg2) (query x arg3))
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
    (vector? q)
    (if-let [next-op (first q)]
      (recur (query x next-op) (vec (rest q)))
      x)
    (list? q) (sexpr-query x q)
    (map? q) (create-map x (apply concat (seq q)))
    (map? x) (get x q)
    (number? q) (safe-nth x q)
    :else (get x q)))

;;;; Scratch

(comment
  )
