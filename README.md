# jet

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

Transform JSON into EDN into Transit.

## Usage

`jet` supports three options:

   `--from`: allowed values: `edn`, `transit` or `json`
   `--to`: allowed values: `edn`, `transit` or `json`
   `--keywordize`: allowed values: `true` or `false`
   `--version`: prints current version of `jet`

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn --keywordize false
{a 1}
$ echo '{"a": 1}' | jet --from json --to edn --keywordize true
{:a 1}
$ echo '{"a": 1}' | jet --from json --to transit
["^ ","a",1]
```

## Installation

Install via brew:

    brew install borkdude/brew/jet

To upgrade:

    brew upgrade jet

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
