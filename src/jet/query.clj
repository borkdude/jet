(ns jet.query
  {:no-doc true}
  (:refer-clojure :exclude [comparator])
  (:require [clojure.set :as set]))

(declare query)

(defn safe-nth [x n]
  (try (nth x n)
       (catch Exception _e
         nil)))

(defn var-lookup [sym]
  (case sym
    id identity
    identity identity
    conj conj
    count count
    first first
    last last
    vals vals
    keys keys
    take take
    drop drop
    inc inc
    dec dec
    nth safe-nth
    = =
    < <
    <= <=
    >= >=
    not= not=
    map map
    filter filter
    remove remove
    distinct distinct
    dedupe dedupe
    + +
    - -
    * *
    / /
    nil))

(defn promote-query* [q]
  (if (symbol? q)
    (list q)
    q))

(defn create-map [x args]
  (let [keys (take-nth 2 args)
        vals (take-nth 2 (rest args))
        vals (map #(query x %) vals)]
    (zipmap keys vals)))

(defn query* [x q]
  (let [[op & args] q
        f (var-lookup op)
        [arg1 arg2 arg3] args
        res (case op
              ;; special case
              quote arg1
              ;; spy
              debug (do (println (if arg1
                                   (query x arg1)
                                   x))
                        x)
              ;; accessor, arg is not a query
              get (get x arg1)
              ;; control flow
              if (if (query x arg1) (query x arg2) (query x arg3))
              while (if (query x arg1)
                      (query* (query x arg2)
                              (list 'while arg1 arg2))
                      x)
              ;; functions with 1 arg
              (first last keys vals inc dec identity id count distinct dedupe)
              (if arg1
                (f (query x arg1))
                (f x))
              ;; macros with 1 arg
              not (if arg1
                    (not (query x arg1))
                    (not x))

              ;; functions with 2 args
              ;; index first
              (take drop)
              (if arg2
                (f (query x arg1) (query x arg2))
                ;; index first
                (f (query x arg1) x))
              nth (if arg2
                    (f (query x arg1) (query x arg2))
                    ;; index last
                    (f x (query x arg1)))
              map-vals (zipmap (keys x)
                               (map #(query % arg1) (vals x)))
              zipmap (zipmap (first x) (second x))
              (map filter remove)
              (f #(query % (promote-query* arg1)) x)
              select-keys (select-keys x arg1)
              rename-keys (set/rename-keys x arg1)
              update (let [[k update-query] args
                           update-query (promote-query* update-query)
                           v (get x k)]
                       (assoc x k (query v update-query)))
              assoc-in (let [[path assoc-in-query] args
                             v (query x assoc-in-query)]
                         (assoc-in x path v))
              update-in (let [[path update-in-query] args
                              update-in-query (promote-query* update-in-query)
                              v (get-in x path)
                              v (query v update-in-query)]
                          (assoc-in x path v))

              ;; functions/macros with varargs
              ;; vector (into [] (map #(query x %) args))
              into (if-not arg2
                     (into x (query x arg1))
                     (into (query x arg1) (query x arg2)))
              conj (apply conj (query x arg1) (map #(query x %) (rest args)))
              juxt (vec (for [q args
                              :let [q (promote-query* q)]]
                          (query x q)))
              and (reduce (fn [_ q]
                            (let [v (query x q)]
                              (if v v (reduced v))))
                          nil args)
              or (first (for [q args
                              :let [v (query x q)]
                              :when v]
                          v))
              dissoc (apply dissoc x (rest q))
              hash-map (create-map x args)
              assoc (let [args args
                          keys (take-nth 2 args)
                          vals (take-nth 2 (rest args))
                          vals (map #(query x %) vals)]
                      (merge x (zipmap keys vals)))
              (= < <= >= not= + - * /)
              (apply f (map #(query x %) args))
              $ (select-keys x args)

              ;; special cases
              str (apply str (map #(query x %) args))
              re-find (re-find (re-pattern (query x arg1)) (query x arg2))

              ;; fallback
              (get x q))]
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
  (if-let [[_ v]
           (when (map? x)
             (find x q))]
    v
    (cond
      (not q) nil
      (vector? q)
      (if-let [next-op (first q)]
        (recur (query x next-op) (vec (rest q)))
        x)
      (list? q) (query* x q)
      (symbol? q) (query* x [q])
      (map? q) (create-map x (apply concat (seq q)))
      (number? q) (safe-nth x q))))

;;;; Scratch

(comment
  )
