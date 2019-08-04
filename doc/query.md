# Query

The `--query` option allows to select or remove specific parts of the output. A
query is written in EDN. NOTE: some parts of this query language may change in
the coming months after I've used it more (2019-08-04).

Single values can be selected by using a key:

``` clojure
echo '{:a 1}' | jet --from edn --to edn --query ':a'
1
```

Multiple values can be selected using a map:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '{:a true :b true}'
{:a 1, :b 2}
```

By default, only keys that have truthy values in the query will be selected from
the output. However, if one of the values has a falsy value, this behavior is
reversed and other keys are left in:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '{:c false}'
{:a 1, :b 2}
```

``` clojure
$ echo '{:a {:a/a 1 :a/b 2} :b 2 :c 3}' \
| jet --from edn --to edn --query '{:c false :a {:a/b true}}'
{:b 2, :a #:a{:b 2}}
```

If the query is applied to a list-like value, the query is applied to all the
elements inside the list-like value:

``` clojure
echo '[{:a 1 :b 2} {:a 2 :b 3}]' | jet --from edn --to edn --query '{:a true}'
[{:a 1} {:a 2}]
```

Nested values can be selected by using a nested query:

``` clojure
echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --from edn --to edn --query '{:a {:a/a true}}'
{:a {:a/a 1}}
```

Some Clojure-like functions are supported which are mostly intented to operate
on list-like values, except for `keys`, `vals` and `map-vals` which operate on
maps:

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

Multiple queries in a vector are applied after one another:

``` clojure
$ echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '[{:c false} (vals)]'
[1 2]
```

``` clojure
$ echo '{:keys [:a :b :c] :vals [1 2 3]}' \
| lein jet --from edn --to edn --query '[(juxt :keys :vals) (zipmap)]'
{:a 1, :b 2, :c 3}
```

``` clojure
$ curl -s https://jsonplaceholder.typicode.com/todos \
| lein jet --from json --keywordize --to edn --query '[(filter :completed) (count)]'
90
```

``` clojure
$ curl -s https://jsonplaceholder.typicode.com/todos \
| lein jet --from json --keywordize --to edn --query '[(remove :completed) (count)]'
110
```

Comparing values can be done with `=`, `>`, `>=`, `<` and `<=`.

``` clojure
$ echo '[{:a 1} {:a 2} {:a 3}]' | \
lein jet --from edn --keywordize --to edn --query '(filter (>= :a 2))'
[{:a 2} {:a 3}]
```
