(ns jet.query)

(declare query)

(defn sexpr-query [x q]
  (let [res (case (first q)
              take (take (second q) x)
              drop (drop (second q) x)
              nth (try (nth x (second q))
                       (catch Exception _e
                         (last x)))
              keys (vec (keys x))
              vals (vec (vals x))
              first (first x)
              last (last x)
              map (map #(sexpr-query % (list (second q))) x)
              juxt (vec (for [q (rest q)]
                          (if (symbol? q)
                            (sexpr-query x (list q))
                            (query x q))))
              x)]
    (if (and (vector? x) (sequential? res))
      (vec res)
      res)))

(defn query
  [x q]
  (cond
    (not query) nil
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
