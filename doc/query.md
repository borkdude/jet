# Query

The `--query` option allows to select or remove specific parts of the output. A
query is written in EDN.

NOTE: some parts of this query language may change in the coming months after it
has seen more usage (2019-08-04).

Single values can be selected by using a key:

``` clojure
echo '{:a 1}' | jet --from edn --to edn --query ':a'
1
```

Multiple values can be selected using set notation:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '#{:a :b}'
{:a 1, :b 2}
```

or more explicitly using `select-keys`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '(select-keys [:a :b])'
{:a 1, :b 2}
```

Removing keys can be achieved with `dissoc`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '(dissoc :c)'
{:a 1, :b 2}
```

If the query is applied to a list-like value, the query is applied to all the
elements inside the list-like value:

``` clojure
echo '[{:a 1 :b 2} {:a 2 :b 3}]' | jet --from edn --to edn --query '#{:a}'
[{:a 1} {:a 2}]
```

Applying multiple queries after one another can be achieved using vector
notation. Queries on nested keys are written using nested maps.

``` clojure
echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --from edn --to edn --query '[#{:a} {:a #{:a/a}}]'
{:a {:a/a 1}}
```

The following Clojure-like functions are supported:

Some Clojure-like functions are supported:

- functions that operate on maps: `keys`, `vals`, `rename-keys`, `select-keys`,
  `dissoc`, `map-vals`, `juxt`, `count`
- functions that operate on list-like values: `first`, `last`, `take`, `drop`,
  `nth`, `map`, `zipmap`, `filter`, `remove`, `juxt`, `count`

``` clojure
echo '{"foo bar": 1}' | jet --from json --to json --query '(rename-keys {"foo bar" "foo-bar"})'
```

``` clojure
echo '[1 2 3]' | jet --from edn --to edn --query '(first)'
1
```

``` clojure
echo '[1 2 3]' | jet --from edn --to edn --query '(last)'
3
```

``` clojure
echo '[[1 2 3] [4 5 6]]' | jet --from edn --to edn --query '(map last)'
[3 6]
```

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '{:a (take 2)}'
{:a [1 2]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '{:a (drop 2)}'
{:a [3]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --from edn --to edn --query '{:a (nth 2)}'
{:a 3}
```

``` clojure
$ echo '{:a [1 2 3]}' | jet --from edn --to edn --query '{:a (juxt first last)}'
{:a [1 3]}
```

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(juxt :a :b)'
[[1 2 3] [4 5 6]]
```

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(keys)'
[:a :b]
```

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --from edn --to edn --query '(vals)'
[[1 2 3] [4 5 6]]
```

``` clojure
$ echo '{:foo {:a 1 :b 2} :bar {:a 1 :b 2}}' | jet --from edn --to edn --query '(map-vals :a)'
{:foo 1 :bar 2}
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
