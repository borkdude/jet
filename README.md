# jet

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

CLI to transform between JSON, EDN and Transit.

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

   - `--from`: allowed values: `edn`, `transit` or `json`
   - `--to`: allowed values: `edn`, `transit` or `json`
   - `--keywordize`: if present, keywordizes JSON keys.
   - `--pretty`: if present, pretty-prints JSON and EDN output.
   - `--query`: if present, applies query to output. See [Query](doc/query.md).
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
| jet --from edn --to edn --query '(filter (= [:a :b] 1))'
[{:a {:b 1}}]
```

Get the latest commit SHA and date for a project from Github:
``` shellsession
$ curl -s https://api.github.com/repos/borkdude/clj-kondo/commits \
| jet --from json --keywordize --to edn \
--query '[0 {:sha :sha :date [:commit :author :date]}]'
{:sha "bde8b1cbacb2b44ad2cd57d5875338f0926c8c0b", :date "2019-08-05T21:11:56Z"}
```

## [Query](doc/query.md)

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
