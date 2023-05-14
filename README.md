<img src="logo/logo-300dpi.png" width="140px">

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/borkdude/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

CLI to transform between [JSON](https://www.json.org/), [EDN](https://github.com/edn-format/edn), [YAML](https://yaml.org/) and [Transit](https://github.com/cognitect/transit-format) using Clojure.

## Quickstart

``` shellsession
$ bash < <(curl -s https://raw.githubusercontent.com/borkdude/jet/master/install)
$ echo '{:a 1}' | jet --to json
{"a":1}
```

## Rationale

This is a command line tool to transform between JSON, EDN and Transit using
Clojure. It runs as a GraalVM binary with fast startup time which makes it
suited for shell scripting. It may seem familiar to users of `jq`.

## Installation

### Brew

Linux and macOS binaries are provided via brew.

Install:

    brew install borkdude/brew/jet

Upgrade:

    brew upgrade jet

### Windows

On Windows you can install using [scoop](https://scoop.sh/) and the
[scoop-clojure](https://github.com/littleli/scoop-clojure) bucket.

### Installer script

Install via the installer script:

``` shellsession
$ bash <(curl -s https://raw.githubusercontent.com/borkdude/jet/master/install)
```

By default this will install into `/usr/local/bin`. To change this, provide the directory name:

``` shellsession
$ bash <(curl -s https://raw.githubusercontent.com/borkdude/jet/master/install) /tmp
```

### Download

You may also download a binary from [Github](https://github.com/borkdude/jet/releases).

### JVM

#### Leiningen

This tool can also be used via the JVM. If you use leiningen, you can put the
following in your `.lein/profiles`:

``` clojure
{:user
 {:dependencies [[borkdude/jet "0.4.23"]]
  :aliases {"jet" ["run" "-m" "jet.main"]}}}
```

And then call `jet` like:

``` shellsession
$ echo '["^ ","~:a",1]' | lein jet --from transit --to edn
{:a 1}
```

#### Deps.edn

In `deps.edn`:

``` clojure
:jet {:deps {borkdude/jet {:mvn/version "0.4.23"}}
      :exec-fn jet.main/exec
      :main-opts ["-m" "jet.main"]}
```

You can use both the `-M` and `-X` style invocation, whichever you prefer:

``` clojure
$ echo '[1 2 3]' | clj -M:jet --colors --func '#(-> % first inc)'
2

$ echo '[1 2 3]' | clj -X:jet :colors true :thread-last '"(map inc)"'
(2 3 4)
```

Or install jet as a clj tool:

``` clojure
$ clojure -Ttools install-latest :lib io.github.borkdude/jet :as jet

$ echo '[1 2 3]' | clj -Tjet exec :colors true :func '"#(-> % first inc)"'
2
```

## Usage

`jet` supports the following options:

``` shell
  -i, --from            [ edn | transit | json | yaml ] defaults to edn.
  -o, --to              [ edn | transit | json | yaml ] defaults to edn.
  -t, --thread-last                                     implicit thread last
  -T, --thread-first                                    implicit thread first
  -f, --func                                            a single-arg Clojure function, or a path to a file that contains a function, that transforms input.
      --no-pretty                                       disable pretty printing
  -k, --keywordize      [ <key-fn> ]                    if present, keywordizes JSON/YAML keys. The default transformation function is keyword unless you provide your own.
      --colors          [ auto | true | false]          use colored output while pretty-printing. Defaults to auto.
      --edn-reader-opts                                 options passed to the EDN reader.
      --no-commas                                       remove commas from EDN
  -c, --collect                                         given separate values, collects them in a vector.
  -h, --help                                            print this help text.
  -v, --version                                         print the current version of jet.
  -q, --query                                           DEPRECATED, prefer -t, -T or -f. Given a jet-lang query, transforms input.
```

Transform EDN using `--thread-last`, `--thread-first` or `--func`.

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn
{"a" 1}

$ echo '{"a": 1}' | jet -i json --keywordize -o edn
{:a 1}

$ echo '{"my key": 1}' | jet -i json -k '#(keyword (str/replace % " " "_"))' -o edn
{:my_key 1}

$ echo '{"anApple": 1}' | jet -i json -k '#(-> % csk/->kebab-case keyword)' -o edn
{:an-apple 1}

$ echo '{"a": 1}' | jet -i json -o yaml
a: 1

$ echo '{"a": 1}' | jet -i json -o transit
["^ ","a",1]

$ echo '{:a {:b {:c 1}}}' | jet --thread-last ':a :b :c'
1

$ echo '{:a {:b {:c 1}}}' | jet --func '#(-> % :a :b :c)'
1

$ echo '{:a {:b {:c [1 2]}}}' | jet -t ':a :b :c (map inc)'
(2 3)

$ cat /tmp/fn.clj
#(-> % :a :b :c)
$ echo '{:a {:b {:c 1}}}' | jet --func /tmp/fn.clj
1

$ echo '{:a {:a 1}}' | ./jet -t '(s/transform [s/MAP-VALS s/MAP-VALS] inc)'
{:a {:a 2}}
```

## Raw output

Get raw output from query rather than wrapped in quotes:

```shellsession
$ echo '{"a": "hello there"}' | jet --from json --keywordize -t ":a" --to edn
"hello there"

$ echo '{"a": "hello there"}' | jet --from json --keywordize -t ":a symbol" --to edn
hello there
```

or simply use `println` to get rid of the quotes:

``` clojure
$ echo '{"a": "hello there"}' | jet --from json --keywordize -t ":a println" --to edn
hello there
```

## Data readers

You can enable data readers by passing options to `--edn-reader-opts`:

``` shell
$ echo '#foo{:a 1}' | jet --edn-reader-opts '{:default tagged-literal}'
#foo {:a 1}
$ echo '#foo{:a 1}' | jet --edn-reader-opts "{:readers {'foo (fn [x] [:foo x])}}"
[:foo {:a 1}]
```

See this [blog](https://insideclojure.org/2018/06/21/tagged-literal/) by Alex Miller for more information on the `tagged-literal` function.

Since jet 0.0.14 `--edn-reader-opts` defaults to `{:default tagged-literal}`.

## Streaming

Jet supports streaming over multiple values, without reading the entire input
into memory:

``` shellsession
$ echo '{"a": 1} {"a": 1}' | jet --from json --keywordize -t ':a' --to edn
1
1
```

When you want to collect multiple values into a vector, you can use `--collect`:

``` shellsession
$ echo '{"a": 1} {"a": 1}' | lein jet --from json --keywordize --collect --to edn
[{:a 1} {:a 1}]
```

## Specter

As of version `0.2.18` the [specter](https://github.com/redplanetlabs/specter) library is available in `--func`, `--thread-first` and `--thread-last`:

``` clojure
$ echo '{:a {:a 1}}' | ./jet -t '(s/transform [s/MAP-VALS s/MAP-VALS] inc)'
{:a {:a 2}}
```

## Base64

To encode and decode base64 you can use `base64/encode` and `base64/decode`.

## Jet utility functions

In the `jet` namespace, the following utilities are available:

### `paths`

Return all paths (and sub-paths) in maps and vectors. Each result is a map of `:path` and `:val` (via `get-in`).

``` clojure
$ echo '{:a {:b [1 2 3 {:x 2}] :c {:d 3}}}' | jet -t '(jet/paths)'
[{:path [:a], :val {:b [1 2 3 {:x 2}], :c {:d 3}}}
 {:path [:a :b], :val [1 2 3 {:x 2}]}
 {:path [:a :c], :val {:d 3}}
 {:path [:a :b 0], :val 1}
 {:path [:a :b 1], :val 2}
 {:path [:a :b 2], :val 3}
 {:path [:a :b 3], :val {:x 2}}
 {:path [:a :b 3 :x], :val 2}
 {:path [:a :c :d], :val 3}]
```

### `when-pred`

Given a predicate, return predicate that returns the given argument when
predicate was truthy. In case of an exception during the predicate call, catches
and returns `nil`.

The following returns all paths for which the leafs are odd numbers:

``` clojure
$ echo '{:a {:b [1 2 3 {:x 2}] :c {:d 3}}}' | jet -t '(jet/paths) (filter (comp (jet/when-pred odd?) :val)) (mapv :path)'
[[:a :b 0] [:a :b 2] [:a :c :d]]
```

## Emacs integration

Sometimes it's useful to reformat REPL output in Emacs to make it more
readable, copy to clipboard or just pretty-print to another buffer.
All of that is avaiable in the [jet.el](https://github.com/ericdallo/jet.el) package.

## Vim integration

To convert data in vim buffers, you can select the data you want to convert in visual mode,
then invoke `jet` by typing for example `:'<,'>!jet -k --from json` (vim will insert the 
`'<,'>` for you as you type).

## Test

Test the JVM version:

    script/test

Test the native version:

    JET_TEST_ENV=native script/test

## Build

You will need leiningen and GraalVM.

    script/compile

## License

Copyright © 2019-2023 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
