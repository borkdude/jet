# jet

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

CLI to transform JSON into EDN into Transit and vice versa.

## Installation

Linux and macOS binaries are provided via brew.

Install:

    brew install borkdude/brew/jet

Upgrade:

    brew upgrade jet

You may also download a binary from [Github](https://github.com/borkdude/jet/releases).

This tool can also be used via the JVM. If you use leiningen, you can put the
following in your `.lein/profiles`:

``` clojure
{:user
 {:dependencies [[borkdude/jet "0.0.2"]]
  :aliases {"jet" ["run" "-m" "jet.main"]}}}
```

And then call `jet` like:

``` shellsession
$ echo '["^ ","~:a",1]' | lein jet --from transit --to edn
{:a 1}
```

## Usage

`jet` supports the following options:

   - `--from`: allowed values: `edn`, `transit` or `json`
   - `--to`: allowed values: `edn`, `transit` or `json`
   - `--keywordize`: if present, keywordizes JSON keys.
   - `--pretty`: if present, pretty-prints JSON and EDN output.
   - `--query`: if present, applies query to output. See [query](#query).
   - `--version`: if present, prints current version of `jet` and exits.

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn
{"a" 1}
$ echo '{"a": 1}' | jet --from json --keywordize --to edn
{:a 1}
$ echo '{"a": 1}' | jet --from json --to transit
["^ ","a",1]
```

## Query

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

Composed queries should be wrapped in a vector:

``` clojure
$ echo '{:a 1 :b 2 :c 3}' | jet --from edn --to edn --query '[{:c false} (vals)]'
[1 2]
```

## Test

Test the JVM version:

    script/test

Test the native version:

    JET_TEST_ENV=native script/test

## Build

You will need leiningen and GraalVM.

    script/compile

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
