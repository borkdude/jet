# jet

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/borkdude/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

CLI to transform between JSON, EDN and Transit, powered with a minimal query
language.

## Rationale

- This CLI is suited for shell-scripting because it has proper startup time.

- The query language jet-lang is a minimal language for transformation of input,
before writing out to JSON, EDN, or Transit.

- The interactive shell enables you to learn jet-lang and work out the
correctness of a query. It can also be useful on servers where you want to inspect
data, but don't have REPL.

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
 {:dependencies [[borkdude/jet "0.0.6"]]
  :aliases {"jet" ["run" "-m" "jet.main"]}}}
```

And then call `jet` like:

``` shellsession
$ echo '["^ ","~:a",1]' | lein jet --from transit --to edn
{:a 1}
```

## Usage

`jet` supports the following options:

   - `--from`: `edn`, `transit` or `json`, defaults to `edn`
   - `--to`: `edn`, `transit` or `json`, defaults to `edn`
   - `--keywordize`: if present, keywordizes JSON keys.
   - `--pretty`: if present, pretty-prints JSON and EDN output.
   - `--query`: given a jet-lang query, transforms input. See [jet-lang docs](doc/query.md).
   - `--interactive`: if present, starts an interactive shell.
   - `--version`: if present, prints current version of `jet` and exits.

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn
{"a" 1}

$ echo '{"a": 1}' | jet --from json --keywordize --to edn
{:a 1}

$ echo '{"a": 1}' | jet --from json --to transit
["^ ","a",1]

$ echo '[{:a {:b 1}} {:a {:b 2}}]' \
| jet --from edn --to edn --query '(filter (= [:a :b] #jet/lit 1))'
[{:a {:b 1}}]
```

- Get the latest commit SHA and date for a project from Github:

``` shellsession
$ curl -s https://api.github.com/repos/borkdude/clj-kondo/commits \
| jet --from json --keywordize --to edn \
--query '[0 {:sha :sha :date [:commit :author :date]}]'
{:sha "bde8b1cbacb2b44ad2cd57d5875338f0926c8c0b", :date "2019-08-05T21:11:56Z"}
```

## [Query language](doc/query.md)

## Caveats

When using `--keywordize` and `--to edn` ensure that your keys do not have
whitespace, as this will result in invalid EDN.

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
