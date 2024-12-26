# C-- to JVM compiler
A compiler that translates C-- (C minus minus, a subset of C) to JVM bytecode (Jasmin) with an additional interpreter mode. Built using ANTLR4 for lexing and parsing. The subset of C functionality implemented in this version of C-- is arbitrary.

## Background
This project began as assignments in a course called Programming Language Technology, where the assignments originally used BNFC for parsing and lexing. I really enjoyed compiler development and decided to keep it as a hobby project, combining and rewriting it to use ANTLR4 instead of BNFC and also adding other features that seemed interesting to code.

This version of C-- is basically baby C, it has some C features, but what exactly gets implemented is pretty arbitrary.

The built JAR can either:

- Generate Jasmin assembly code for JVM execution
- Directly interpret CMM code through an built-in interpreter

The compiler generates Jasmin assembly code as a middle-step, which is then assembled into JVM bytecode using Jasmin. 

For example, a simple CMM function:
```c
int add(int a, int b) {
    return a + b;
}
```

Gets compiled to the Jasmin assembly:
```jasmin
.method public static add(II)I
    .limit stack 2
    .limit locals 2
    iload_0    ; load first parameter a
    iload_1    ; load second parameter b
    iadd       ; add them 
    ireturn    ; return result
.end method
```

The Jasmin assembler then converts this into the final JVM bytecode (.class file).

## Building

Gradle is used for build automation, with all dependencies (including ANTLR4 parser/lexer generation) managed through it.

Build the project using Gradle wrapper:
```bash
./gradlew build
```

This will automatically:
- Download dependencies
- Generate ANTLR4 parser and lexer Java code
- Compile all Java files

For Windows use:
```bash
gradlew.bat build
```
## Usage
TODO
