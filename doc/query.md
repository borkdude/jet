# jet-lang: a query language you probably already know

NOTE: This query language is work in progress. Consider it experimental, suited
for exploratory programming, but not suited for production usage yet
(2019-08-04).

NOTE: In this document, the word list also applies to arrays and vectors.

The `--query` option allows to select or remove specific parts of the output. A
query is written in EDN.

These Clojure-like functions are supported in jet-lang:

- for maps: `assoc`, `assoc-in`, `update`, `update-in`, `keys`, `vals`,
  `set/rename-keys`, `select-keys`, `dissoc`, `map-vals`, `juxt`, `count`, `into`.
- for lists: `first`, `last`, `take`, `drop`, `nth`, `map`, `zipmap`, `filter`,
  `remove`, `juxt`, `count`, `distinct`, `dedupe`, `conj`, `into`.
- working with strings: `str`, `re-find`.
- logic: `and`, `or`, `not`, `if`, `=`, `not=`, `>`, `>=`, `<`, `<=`.
- literal values: `quote`/`#jet/lit`.
- copy the entire input value: `identity` (for short `id`, thanks Haskell).
- print the result of an intermediate query: `jet/debug`.
- arithmetic: `+`, `-`, `*`, `/`, `inc`, `dec`.

To learn more about how to use them, read the [tutorial](#tutorial) or go
straight to the [gallery](#gallery).

## Tutorial

Single values can be selected by using a key:

``` clojure
echo '{:a 1}' | jet --query ':a'
1
```

or more explicity with `get`:

``` clojure
echo '{:a 1}' | jet --query '(get :a)'
1
```

Numbers can be used for looking up by position in lists:
``` clojure
$ echo '[1 2 3]' | lein jet --query '0'
1
$ echo '[1 2 3]' | lein jet --query '100'
nil
```

You can also use `nth` for this:

``` clojure
$ echo '[1 2 3]' | lein jet --query '(nth #jet/lit 0)'
1
```

A subselection of a map can be made with `select-keys`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --query '(select-keys [:a :b])'
{:a 1, :b 2}
```

The function `$` is a short-hand for `select-keys` that doesn't wrap the keys in
a sequence:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --query '($ :a :b)'
{:a 1, :b 2}
```

Removing keys can be achieved with `dissoc`:

``` clojure
echo '{:a 1 :b 2 :c 3}' | jet --query '(dissoc :c)'
{:a 1, :b 2}
```

A query can be applied to every element in a list using `map`:

``` clojure
$ echo '[{:a 1 :b 2} {:a 2 :b 3}]' | jet --query '(map ($ :a))'
[{:a 1} {:a 2}]
```

Associating a new key and value in a map is done with `assoc`:

``` clojure
$ echo '{:a 1}' | jet --query '(assoc :b :a)'
{:a 1, :b 1}
```

Updating an existing key and value can be done with `update`:

``` clojure
$ echo '{:a {:b 1}}' | jet --query '(update :a :b)'
{:a 1}
```

<!-- Like any query, functions can be applied in a nested fashion: -->

``` clojure
echo '{:a [1 2 3]}' | jet --query '(update :a (take #jet/lit 2))'
{:a [1 2]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --query '(update :a (drop #jet/lit 2))'
{:a [3]}
```

``` clojure
echo '{:a [1 2 3]}' | jet --query '(update :a (nth #jet/lit 2))'
{:a 3}
```

The difference between `assoc` and `update` is that the query provided to the
former begins at the root and the query provided to the latter begins at place
to be updated.

There are also `assoc-in` and `update-in` which behave in similar ways but allow
changing nested values:

``` clojure
$ echo '{:a 1}' | jet --query '(assoc-in [:b :c] :a)'
{:a 1, :b {:c 1}}

$ echo '{:a {:b [1 2 3]}}' | jet --query '(update-in [:a :b] last)'
{:a {:b 3}}
```

Creating a new map from scratch is done with `hash-map`:

``` clojure
$ echo '{:a 1 :b 2}' | jet --query '(hash-map :foo :a :bar :b)'
{:foo 1, :bar 2}
```

or using a map literal:

``` clojure
$ echo '{:a 1 :b 2}' | jet --query '{:foo :a :bar :b}'
{:foo 1, :bar 2}
```

Inserting literal values can be done with done with `quote`:

``` clojure
$ echo '{:a 1}' | jet --query '{:foo :a :bar (quote "hello")}'
{:foo 1, :bar "hello"}
```

or prefixing it with the tag `#jet/lit`:

``` clojure
$ echo '{:a 1}' | jet --query '{:foo :a :bar #jet/lit "hello"}'
{:foo 1, :bar "hello"}
```

Applying multiple queries after one another can be achieved using vector
notation.

``` clojure
$ echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --query '[($ :a) (update :a :a/a)]'
{:a 1}
```

The outer query is implicitly wrapped, so you don't have to wrap it yourself:

``` clojure
$ echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --query '($ :a) (update :a :a/a)'
{:a 1}
```

Copy the input value:

``` shellsession
$ echo '{:a 1}' | jet --query '{:input (identity)}'
{:input {:a 1}}
```

Or for short:

``` shellsession
$ echo '{:a 1}' | jet --query '{:input (id)}'
{:input {:a 1}}
```

When functions with only one (implicit input) argument are used, the wrapping
parens may be left out, so even shorter:

``` shellsession
$ echo '{:a 1}' | jet --query '{:input id}'
{:input {:a 1}}
```

You can print the result of an intermediate query using `jet/debug`:

``` clojure
$ echo '{:a {:a/a 1 :a/b 2} :b 2}' | jet --query '($ :a) jet/debug (update :a :a/a)'
{:a #:a{:a 1, :b 2}}
{:a 1}
```

Keys and values:

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --query '(keys)'
[:a :b]
```

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --query '(vals)'
[[1 2 3] [4 5 6]]
```

``` clojure
echo '{"foo bar": 1}' | jet --from json --to json --query '(set/rename-keys {"foo bar" "foo-bar"})'
{"foo-bar":1}
```

To apply a function on all map values, use `map-vals`:

``` clojure
$ echo '{:foo {:a 1 :b 2} :bar {:a 1 :b 2}}' | jet --query '(map-vals :a)'
{:foo 1 :bar 2}
```

Miscellaneous list functions:

``` clojure
echo '[1 2 3]' | jet --query '(first)'
1
```

``` clojure
echo '[1 2 3]' | jet --query '(last)'
3
```

<!-- To avoid ambiguity when applying a function to list elements, use `map`
explicity. -->

``` clojure
echo '[[1 2 3] [4 5 6]]' | jet --query '(last)'
[4 5 6]
```

``` clojure
echo '[[1 2 3] [4 5 6]]' | jet --query '(map last)'
[3 6]
```

``` clojure
echo '[{:a 1} {:a 2}]' | jet --query '(count)'
2
```

``` clojure
echo '[{:a 1} {:a 2}]' | jet --query '(map count)'
[1 1]
```

Use `juxt` to apply multiple queries to the same element. The result is a list
of multiple results.

``` clojure
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --query '(juxt :a :b)'
[[1 2 3] [4 5 6]]
```

``` clojure
$ echo '{:a [1 2 3]}' | jet --query '(update :a (juxt first last))'
{:a [1 3]}
```

``` clojure
$ echo '[1 2 3]' | jet --query '(juxt 0 1 2 3)'
[1 2 3 nil]
```

An example with `zipmap`:

``` clojure
$ echo '{:keys [:a :b :c] :vals [1 2 3]}' \
| jet --query '[(juxt :keys :vals) (zipmap)]'
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

Remove duplicate values with `distinct` and `dedupe`:

``` clojure
$ echo '{:a [1 1 2 2 3 3 1 1]}' | jet --query '[:a (distinct)]'
[1 2 3]

$ echo '{:a [1 1 2 2 3 3 1 1]}' | jet --query '[:a (dedupe)]'
[1 2 3 1]
```

Producing a string can be done with `str`:

``` shellsession
 echo '{:a 1 :b 2}' | jet --query '(str :a #jet/lit "/" :b)'
"1/2"
```

Comparing values can be done with `=`, `not=`, `>`, `>=`, `<` and `<=`.

``` clojure
$ echo '[{:a 1} {:a 2} {:a 3}]' | jet --query '(filter (>= :a #jet/lit 2))'
[{:a 2} {:a 3}]
```

``` clojure
echo '[{:a {:b 1}} {:a {:b 2}}]' \
| jet --query '(filter (= [:a :b] #jet/lit 1))'
[{:a {:b 1}}]
```

Applying a regex can be done with `re-find`:

``` shellsession
$ echo '{:a "foo bar" :b 2}' | lein jet --query '(re-find #jet/lit "foo" :a)'
"foo"
```

Control flow with `if`:

``` shellsession
$ echo '{:a "foo bar" :b 2}' | jet --query '(if (re-find #jet/lit "foo" :a) :a :b)'
"foo bar"
```
There is also `while`, which repeats a query until the condition is not met.

``` shellsession
$ echo '0' | jet --query '(while (< id #jet/lit 10) (inc id))'
10
```

The Fibonacci sequence using `while`:

``` shellsession
$ echo '{:fib0 0 :fib1 1 :n 0 :fib []}' | lein jet --query '
(while (<= :n #jet/lit 10)
 {:fib0 :fib1 :fib1 (+ :fib0 :fib1) :n (inc :n) :fib (conj :fib :fib0)})
:fib'
[0 1 1 2 3 5 8 13 21 34 55]
```

Boolean logic:

``` shellsession
$ echo '{:a 1 :b 3}' | jet --query '(and :a :b)'
3
```

``` shellsession
$ echo '{:a 1 :b 3}' | jet --query '(or :a :b)'
1
```

``` shellsession
$ echo '{:a 1 :b 3}' | jet --query '(not :a)'
false
```

Arithmetic:

``` shellsession
$ echo '{:a 3 :b 2}]' | jet --query '(inc :a)'
4
```

``` shellsession
$ echo '{:a 3 :b 2}]' | jet --query '(* :a :b) (- id #jet/lit 2)'
4
```

Use `conj` for adding elements to a list:

``` shellsession
$ echo '[1 2 3]' | jet --query '(conj id #jet/lit 4)'
[1 2 3 4]
```

Because `conj` is varargs, it always takes an explicit input query.

Concatenating two lists or merging two maps can be done with `into`:

``` shellsession
$ echo '{:a [1 2 3] :b [4 5 6]}' | jet --query '(into :a :b)'
[1 2 3 4 5 6]
$ echo '{:a {:x 1} :b {:y 2}}' | jet --query '(into :a :b)'
{:x 1, :y 2}
```

## Gallery

### jq example

The last example of the [jq](https://stedolan.github.io/jq/tutorial/) tutorial
using jet:

``` shellsession
$ curl -s 'https://api.github.com/repos/stedolan/jq/commits?per_page=5' | \
jet --from json --keywordize --to edn --pretty --query '
(map
 {:message [:commit :message]
  :name [:commit :committer :name]
  :parents [:parents (map :html_url)]})'

({:message "Merge pull request #1948 from eli-schwartz/no-pacman-sy\n\ndocs: fix seriously dangerous download instructions for Arch Linux",
  :name "GitHub",
...
```

### Latest commit SHA

Get the latest commit SHA and date for a project from Github:
``` shellsession
$ curl -s https://api.github.com/repos/borkdude/clj-kondo/commits \
| jet --from json --keywordize --to edn \
--query '[0 {:sha :sha :date [:commit :author :date]}]'
{:sha "bde8b1cbacb2b44ad2cd57d5875338f0926c8c0b", :date "2019-08-05T21:11:56Z"}
```
