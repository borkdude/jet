# jet

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

Transform JSON into EDN into Transit and vice versa.

## Usage

`jet` supports the following options:

   - `--from`: allowed values: `edn`, `transit` or `json`
   - `--to`: allowed values: `edn`, `transit` or `json`
   - `--keywordize`: allowed values: `true` or `false`. If `true`, keywordizes JSON keys.
   - `--version`: if present, prints current version of `jet` and exits.

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn --keywordize false
{"a" 1}
$ echo '{"a": 1}' | jet --from json --to edn --keywordize true
{:a 1}
$ echo '{"a": 1}' | jet --from json --to transit
["^ ","a",1]
```

## Installation

Linux and macOS binaries are provided via brew.

Install:

    brew install borkdude/brew/jet

Upgrade:

    brew upgrade jet

This tool can also be used via the JVM. If you use leiningen, you can put the
following in your `.lein/profiles`:

``` clojure
{:user
 {:dependencies [[borkdude/jet "0.0.2-SNAPSHOT"]]
  :aliases {"jet" ["run" "-m" "jet.main"]}}}
```

And then call `jet` like:

``` shellsession
$ echo '["^ ","~:a",1]' | lein jet --from transit --to edn
{:a 1}
```

## Build

You will need leiningen and GraalVM.

    script/compile

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
