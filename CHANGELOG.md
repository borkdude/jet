# Changelog

[jet](https://github.com/borkdude/jet): CLI to transform between JSON, EDN, YAML and Transit using Clojure.

## 0.4.24 (2023.04.09)

- [#127](https://github.com/borkdude/jet/issues/127): support linux aarch64 binary

## 0.4.23 (2023.01.27)

- [#123](https://github.com/borkdude/jet/issues/123): Add `base64/encode` and `base64/decode`
- Add `jet/paths` and `jet/when-pred`
- Deprecate interactive mode
- Deprecate `--query` in favor of `--thread-last`, `--thread-first` or `--func`

## 0.3.21 (2022.10.28)

- [#74](https://github.com/borkdude/jet/issues/74): Add yaml support ([@qdzo](https://github.com/qdzo))

## 0.2.20 (2022.06.17)

- Make suitable as clj tool

## 0.2.19 (2022.06.17)

- Migrate CLI to [babashka CLI](https://github.com/babashka/cli)
- Introduce `jet.main/exec` which can be called as clojure CLI exec fn

## 0.2.18 (2022.06.16)

- Provide macOS `aarch64` binary
- Reverse `--pretty` to `--no-pretty`: pretty printing is now done by default
- Introduce colored output when pretty-printing. Defaults to `--colors auto`
  which only prints colors when connected to terminal. Toggle manually with
  `--colors true` or `--colors false`.
- Integrate [specter](https://github.com/redplanetlabs/specter)
- Introduce `--thread-last` and `--thread-first`

## 0.1.1

- Compile linux binary as static with musl

## 0.1.0

- Allow keywordize fn to access all available conversion functions from camel-snake-kebab lib. e.g. `csk/->PascalCase`

## 0.0.15

- Add short options: `-q` for `--query`, `-c` for `--collect` ([@dotemacs](https://github.com/dotemacs))
- Add tests for option parsing ([@dotemacs](https://github.com/dotemacs))
- The `-f` / `--func` option can now read from a file ([@dotemacs](https://github.com/dotemacs))

## 0.0.14

- Add short options: `-i` (input) for `--from`, `-o` (output) for `--to`
- Add `--func` / `-f` option for executing function over input
- Use `{:default tagged-literal}` as default data reader options when none is provided
- Upgrade GraalVM to 21.0.0
- Upgrade sci to 0.2.4
