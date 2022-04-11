# Changelog

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
