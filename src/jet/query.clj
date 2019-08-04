(ns jet.query
  (:refer-clojure :exclude [comparator]))

(declare query)

(defn comparator [[c q v]]
  (let [c-f (case c
              = =
              < <
              <= <=
              >= >=)]
    #(c-f (query % q) v)))

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
                                   (if (symbol? f)
                                     (list f)
                                     f))) x)
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
              x)]
    (if (and (vector? x) (sequential? res))
      (vec res)
      res)))

(defn query
  [x q]
  (cond
    (not query) nil
    (vector? q) (if-let [next-op (first q)]
                  (query (query x next-op) (vec (rest q)))
                  x)
    (list? q) (sexpr-query x q)
    (sequential? x)
    (mapv #(query % q) x)
    (map? q)
    (let [default (some #(or (nil? %) (false? %)) (vals q))
          kf (fn [[k v]]
               (when-not (contains? q k)
                 [k (query v default)]))
          init (if default (into {} (keep kf x)) {})
          rf (fn [m k v]
               (if (and v (contains? x k))
                 (assoc m k (query (get x k) v))
                 m))]
      (reduce-kv rf init q))
    (map? x) (get x q)
    :else x))

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
