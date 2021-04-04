<img src="logo/logo-300dpi.png" width="140px">

[![CircleCI](https://circleci.com/gh/borkdude/jet/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/jet/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/jet.svg)](https://clojars.org/borkdude/jet)
[![cljdoc badge](https://cljdoc.org/badge/borkdude/jet)](https://cljdoc.org/d/borkdude/jet/CURRENT)

CLI to transform between [JSON](https://www.json.org/), [EDN](https://github.com/edn-format/edn) and [Transit](https://github.com/cognitect/transit-format), powered with a minimal query
language.

## Quickstart

``` shellsession
$ bash <(curl -s https://raw.githubusercontent.com/borkdude/jet/master/install)
$ echo '{:a 1}' | jet --to json
{"a":1}
```

## Rationale

This is a command line tool to transform between JSON, EDN and Transit, powered
with a minimal query language. It runs as a GraalVM binary with fast startup
time which makes it suited for shell scripting. It comes with a query language
to do intermediate transformation. It may seem familiar to users of `jq`.
Although in 2021, you may just want to use the `--func` option instead (who
needs a DSL if you can use normal Clojure?)

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

   - `-i`, `--from`: `edn`, `transit` or `json`, defaults to `edn`
   - `-o`, `--to`: `edn`, `transit` or `json`, defaults to `edn`
   - `-k`, `--keywordize [ <key-fn> ]`: if present, keywordizes JSON keys. The default
     transformation function is `keyword` unless you provide your own.
   - `-p`, `--pretty`: if present, pretty-prints JSON and EDN output.
   - `--edn-reader-opts`: options passed to the EDN reader.
   - `-f`, `--func`: a single-arg Clojure function that transforms input.
   - `-q`, `--query`: given a jet-lang query, transforms input. See [jet-lang docs](doc/query.md).
   - `-C`, `--collect`: given separate values, collects them in a vector.
   - `-v`, `--version`: if present, prints current version of `jet` and exits.
   - `--interactive [ cmd ]`: if present, starts an interactive shell. An
     initial command may be provided. See [here](#interactive-shell).

Examples:

``` shellsession
$ echo '{"a": 1}' | jet --from json --to edn
{"a" 1}

$ echo '{"a": 1}' | jet -i json --keywordize -o edn
{:a 1}

$ echo '{"my key": 1}' | jet -i json -k '#(keyword (str/replace % " " "_"))' -o edn
{:my_key 1}

$ echo '{"a": 1}' | jet -i json -o transit
["^ ","a",1]

$ echo '{:a {:b {:c 1}}}' | jet --query ':a :b :c'
1

$ echo '{:a {:b {:c 1}}}' | jet --func '#(-> % :a :b :c)'
1

$ echo '[1 2 3]' | jet -f '#(map inc %)'
(2 3 4)
```

- Get the latest commit SHA and date for a project from Github:

``` shellsession
$ curl -s https://api.github.com/repos/borkdude/clj-kondo/commits \
| jet --from json --keywordize --to edn \
--query '[0 {:sha :sha :date [:commit :author :date]}]'
{:sha "bde8b1cbacb2b44ad2cd57d5875338f0926c8c0b", :date "2019-08-05T21:11:56Z"}
```

- Get raw output from query rather than wrapped in quotes

```shellsession
$ echo '{"a": "hello there"}' | jet --from json --keywordize --query ":a" --to edn
"hello there"

$ echo '{"a": "hello there"}' | jet --from json --keywordize --query ":a symbol" --to edn
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
$ echo '{"a": 1} {"a": 1}' | jet --from json --keywordize --query ':a' --to edn
1
1
```

When you want to collect multiple values into a vector, you can use `--collect`:

``` shellsession
$ echo '{"a": 1} {"a": 1}' | lein jet --from json --keywordize --collect --to edn
[{:a 1} {:a 1}]
```

## Query

EDIT 2021: if the query syntax isn't clear, you can now use `--func`to pass a
normal Clojure function instead.

The `--query` option supports an intermediate EDN transformation.

``` shellsession
$ echo '{:a 1 :b 2 :c 3}' | jet --query '(select-keys [:a :b])'
{:a 1, :b 2}
$ echo '{:a {:b 1}}' | jet --query '[:a :b]'
1
```

The query language should be pretty familiar to users of Clojure and `jq`. For
more information about the query language, read the docs [here](doc/query.md).

## Interactive shell

The jet interactive shell can be started with the `--interactive`
flag. Optionally you can provide the first command for the shell as an argument:

``` shellsession
$ jet --interactive ':jeti/set-val {:a 1}'
```

``` shellsession
$ curl -sL https://api.github.com/repos/clojure/clojure/commits > /tmp/commits.json
$ jet --interactive ':jeti/slurp "/tmp/commits.json" {:format :json}'
```

Note that a jeti command has to be valid EDN.  To see a list of available
commands, type `:jeti/help` in the shell:

``` shellsession
> :jeti/help
Available commands:
:jeti/set-val {:a 1}   : set value.
:jeti/jump "34d4"      : jump to a previous state.
:jeti/quit, :jeti/exit : exit this shell.
:jeti/slurp            : read a file from disk. Type :jeti/help :jeti/slurp for more details.
:jeti/spit             : writes file to disk. Type :jeti/help :jeti/spit for more details.
:jeti/bookmark "name"  : save a bookmark.
:jeti/bookmarks        : show bookmarks.
:jeti/print-length     : set *print-length*
:jeti/print-level      : set *print-level*
```

## Emacs integration

Sometimes it's useful to reformat REPL output in Emacs to make it more
readable. You can do this by piping the relevant region via jet. Just
put this function somewhere in your Emacs configuration, highlight a
region, and press `M-x jet-pretty <enter>`:

``` emacs-lisp
(defun jet ()
  (interactive)
  (shell-command-on-region
   (region-beginning)
   (region-end)
   "jet --pretty --edn-reader-opts '{:default tagged-literal}'"
   (current-buffer)
   t
   "*jet error buffer*"
   t))
```

Passing `--edn-reader-opts` ensures that jet will respect any tagged
literals that are particular to your codebase.

## Test

Test the JVM version:

    script/test

Test the native version:

    JET_TEST_ENV=native script/test

## Build

You will need leiningen and GraalVM.

    script/compile

## License

Copyright © 2019-2021 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
