# GoLangInterpreter

Educational interpreter for a simplified subset of the Go language.

This repository contains a university project for a Theory of Languages course. The project demonstrates a classic compiler/interpreter pipeline:

1. Lexical analysis (tokenization)
2. Parsing into an AST
3. Semantic analysis (type and symbol checks)
4. AST interpretation (execution)

## Project Goal

The goal is to study how programming languages are implemented in practice by building a working interpreter for a small, Go-like language.

## Implemented Pipeline

- Lexer: converts source code into tokens with line/column positions.
- Parser: builds an Abstract Syntax Tree (AST) from the token stream.
- Semantic analyzer: validates declarations, types, assignments, and function calls.
- Interpreter: executes the AST using runtime contexts and function scopes.

## Supported Language Features

- Declarations:
	- `var`, `const`, `type`
	- short variable declaration `:=`
- Types:
	- `int`, `float64`, `string`
	- arrays and slices
	- map type syntax is parsed and semantically checked
- Expressions and operators:
	- arithmetic: `+`, `-`, `*`, `/`, `%`
	- comparisons: `==`, `!=`, `<`, `>`, `<=`, `>=`
	- logical: `&&`, `||`, `!`
	- unary `+` / `-`
	- indexing: `arr[i]`
- Statements:
	- assignment (`=`, `+=`, `-=`, `*=`, `/=`, `%=`)
	- increment/decrement (`++`, `--`)
	- `if / else`
	- `for condition { ... }`
	- `for index, value := range arrayOrSlice { ... }`
	- `return`
- Functions:
	- function declarations with parameters
	- single return value
	- function calls
- Built-in functions:
	- `println(...)`
	- `numToStr(number)`
	- `strToNum(string)`

## Current Limitations

This is a learning project, not a full Go implementation.

- Source program is currently embedded as a string in `src/Main.java`.
- Struct field selectors are not implemented at runtime.
- Maps are recognized in parsing/semantic analysis, but map runtime behavior is not implemented.
- No packages, modules, imports, interfaces, pointers, goroutines, channels, or standard library support.

## Project Structure

- `src/lexer` - tokenizer, token model, token types
- `src/parser` - parser and parse errors
- `src/parser/ast` - AST node definitions
- `src/analyzer` - semantic analysis and symbol/type checks
- `src/interpreter` - runtime values, contexts, and AST execution
- `src/Main.java` - demo entry point running the full pipeline

## How to Run

Requirements:

- Java JDK 17+ (or compatible modern JDK)

### Option 1: IntelliJ IDEA

1. Open the project.
2. Run `src/Main.java`.

### Option 2: Command Line (PowerShell)

From the project root:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp out Main
```

## Example Program

The demo in `src/Main.java` includes:

- variable declarations and arithmetic
- custom function (`max`)
- multidimensional arrays and indexing
- nested loops
- string operations (including repetition via `"A" * 3`)
- short declarations (`:=`)
- numeric/string conversion helpers
- `for range` iteration over a slice literal

## Educational Value

This codebase is useful for understanding:

- tokenization and grammar-driven parsing
- AST design
- scope and symbol table handling
- semantic type checking
- interpreter-based execution models

It can be extended into a richer language implementation as future coursework.