# Query

NOTE: This query language is work in progress. Consider it experimental, suited for exploratory programming, but not suited for production usage yet (2019-08-04).

NOTE: In this document, the word list also applies to arrays and vectors.

The `--query` option allows to select or remove specific parts of the output. A
query is written in EDN.

Single values can be selected by using a key:

``` clojure
echo '{:a 1}' | jet --from edn --to edn --query ':a'
1
```

A subselection of a map can be made with `select-keys`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '(select-keys [:a :b])'
{:a 1, :b 2}
```

Removing keys can be achieved with `dissoc`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '(dissoc :c)'
{:a 1, :b 2}
```

A query can be applied to every element in a list using `map`:

``` clojure
$ echo '[{:a 1 :b 2} {:a 2 :b 3}]' | jet --from edn --to edn --query '(map (select-keys [:a]))'
[{:a 1} {:a 2}]
```

Associating a new key and value in a map is done with `assoc`:

``` clojure
$ echo '{:a 1}' | jet --from edn --to edn --query '(assoc :b :a)'
{:a 1, :b 1}
```

Updating an existing key and value can be done with `update`:

``` clojure
$ echo '{:a {:b 1}}' | jet --from edn --to edn --query '(update :a :b)'
{:a 1}
```

<!-- Like any query, functions can be applied in a nested fashion: -->

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '(update :a (take 2))'
{:a [1 2]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '(update :a (drop 2))'
{:a [3]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '(update :a (nth 2))'
{:a 3}
```

The difference between `assoc` and `update` is that the query provided to the
former begins at the root and the query provided to the latter begins at place
to be updated.

There are also `assoc-in` and `update-in` which behave in similar ways but allow
changing nested values:

``` clojure
$ echo '{:a 1}' | lein jet --from edn --to edn --query '(assoc-in [:b :c] :a)'
{:a 1, :b {:c 1}}

$ echo '{:a {:b [1 2 3]}}' | lein jet --from edn --to edn --query '(update-in [:a :b] last)'
{:a {:b 3}}
```

Creating a new map from scratch is done with `hash-map`:

``` clojure
$ echo '{:a 1 :b 2}' | jet --from edn --to edn --query '(hash-map :foo :a :bar :b)'
{:foo 1, :bar 2}
```

and creating new values from scratch is done with `quote`:

``` clojure
$ echo '{:a 1}' | jet --from edn --to edn --query '(hash-map :foo :a :bar (quote "hello"))'
{:foo 1, :bar "hello"}
```

Applying multiple queries after one another can be achieved using vector
notation. 

``` clojure
$ echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --from edn --to edn --query '[(select-keys [:a]) (update :a :a/a)]'
{:a 1}
```

In addition to the functions we've already covered, these Clojure-like functions are supported:

- functions that operate on maps: `assoc`, `assoc-in`, `update`, `update-in`, `keys`, `vals`, `rename-keys`, `select-keys`,
  `dissoc`, `map-vals`, `juxt`, `count`.
- functions that operate on lists: `first`, `last`, `take`, `drop`,
  `nth`, `map`, `zipmap`, `filter`, `remove`, `juxt`, `count`.
- function that produces a literal value: `quote`

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(keys)'
[:a :b]
```

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(vals)'
[[1 2 3] [4 5 6]]
```

``` clojure
echo '{"foo bar": 1}' | jet --from json --to json --query '(rename-keys {"foo bar" "foo-bar"})'
{"foo-bar":1}
```

``` clojure
echo '[1 2 3]' | jet --from edn --to edn --query '(first)'
1
```

``` clojure
echo '[1 2 3]' | jet --from edn --to edn --query '(last)'
3
```

<!-- To avoid ambiguity when applying a function to list elements, use `map`
explicity. -->

``` clojure
echo '[[1 2 3] [4 5 6]]' | jet --from edn --to edn --query '(last)'
[4 5 6]
```

``` clojure
echo '[[1 2 3] [4 5 6]]' | jet --from edn --to edn --query '(map last)'
[3 6]
```

``` clojure
echo '[{:a 1} {:a 2}]' | jet --from edn --to edn --query '(count)'
2
```

``` clojure
echo '[{:a 1} {:a 2}]' | jet --from edn --to edn --query '(map count)'
[1 1]
```

To apply a function on a all map values, use `map-vals`:

``` clojure
$ echo '{:foo {:a 1 :b 2} :bar {:a 1 :b 2}}' | jet --from edn --to edn --query '(map-vals :a)'
{:foo 1 :bar 2}
```

Use `juxt` to apply multiple queries to the same element. The result is a list
of multiple results.

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(juxt :a :b)'
[[1 2 3] [4 5 6]]
```

``` clojure
$ echo '{:a [1 2 3]}' | jet --from edn --to edn --query '(update :a (juxt first last))'
{:a [1 3]}
```

An example with `zipmap`:

``` clojure
$ echo '{:keys [:a :b :c] :vals [1 2 3]}' \
| jet --from edn --to edn --query '[(juxt :keys :vals) (zipmap)]'
{:a 1, :b 2, :c 3}
```

Examples with `filter` and `remove`:

``` clojure
$ curl -s https://jsonplaceholder.typicode.com/todos \
| jet --from json --keywordize --to edn --query '[(filter :completed) (count)]'
90
```

``` clojure
$ curl -s https://jsonplaceholder.typicode.com/todos \
| jet --from json --keywordize --to edn --query '[(remove :completed) (count)]'
110
```

Comparing values can be done with `=`, `>`, `>=`, `<` and `<=`.

``` clojure
$ echo '[{:a 1} {:a 2} {:a 3}]' | jet --from edn --to edn --query '(filter (>= :a 2))'
[{:a 2} {:a 3}]
```

``` clojure
echo '[{:a {:b 1}} {:a {:b 2}}]' \
| jet --from edn --to edn --query '(filter (= [:a :b] 1))'
[{:a {:b 1}}]
```
