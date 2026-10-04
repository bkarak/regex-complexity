# Regular expression complexity

Counts the operators in a regular expression, and computes the four complexity
measures of Ehrenfeucht and Zeiger (Journal of Computer and System Sciences,
1976).

The operator census is the one behind the occurrence table in

> Vassilios Karakoidas and Diomidis Spinellis, *FIRE/J: Optimizing Regular
> Expression Searches with Generative Programming*, Software: Practice and
> Experience, 38(6):557–573, 2008.
> [HTML](https://www.spinellis.gr/pubs/jrnl/2007-SPANDE-FIRE/html/KS07.html)

That evaluation used two of the four measures, size and length, to line the
benchmark expressions up. Star height and width are the other two from the
same definitions, so they are reported as well.

## The four measures

Size (N) is the number of alphabetical symbols written in the expression.
Star height (H) is how deep unbounded repetition is nested. Length (L) is the
longest path that does not go around a loop. Width (W) is the dual of length.

| | a symbol | E\|F | E·F | E* |
| --- | --- | --- | --- | --- |
| Size | 1 | N(E) + N(F) | N(E) + N(F) | N(E) |
| Star height | 0 | max | max | H(E) + 1 |
| Length | 1 | max | L(E) + L(F) | L(E) |
| Width | 1 | W(E) + W(F) | max | W(E) |

`+` and `{n,}` raise star height the way `*` does. `?` and a finite bound
such as `{2,4}` do not. A quantifier does not copy its body, so `a{10}` has
size 1 and length 1: the symbol is written once, and the path is not unrolled.

A few expressions, as the tool reports them:

| Expression | N | H | L | W |
| --- | --- | --- | --- | --- |
| `ab\|c` | 3 | 0 | 2 | 2 |
| `(a\|b)(c\|d\|e)` | 5 | 0 | 2 | 3 |
| `(a*)*` | 1 | 2 | 1 | 1 |
| `(a*)(b*)` | 2 | 1 | 2 | 1 |
| `[a-z]` | 26 | 0 | 1 | 26 |
| `[a-z]+` | 26 | 1 | 1 | 26 |
| `a{10}` | 1 | 0 | 1 | 1 |

`[a-z]` is a union of 26 symbols, so its width is 26 and its length is 1.
`(a*)(b*)` has star height 1, because the two stars sit side by side; `(a*)*`
has height 2, because one contains the other.

These are measures of the expression as written. Ehrenfeucht and Zeiger also
define the complexity of a *language* as the minimum over every expression for
that language. This program does not search for a smaller expression.

## What gets expanded

Complements are taken over ASCII, U+0000 through U+007F. That is `\D`, `\W`,
`\S`, `\P{...}` and a negated class. A code point written above U+007F still
counts, so `é` has size 1, and `[^é]` has size 128 (every ASCII character,
since `é` was not one of them).

The dot is one symbol, not the 128 characters it can match. A character class
counts as the symbols it names: `\d` is 10, `\w` is 63, `\s` is 6, `[a-c]` is
3. Case flags do not multiply letters, so `(?i)[a-z]` stays 26.

`\h` and `\v` are Java's horizontal and vertical whitespace, 19 and 7
symbols, some of them above ASCII. Their complements `\H` and `\V` are taken
over ASCII like the rest. A backreference such as `\1` counts as one symbol.

## Operators

The occurrence report counts `*`, `+`, `.`, `?`, `|`, `[]` and `()`. A lazy
or possessive mark (`*?`, `*+`) belongs to the quantifier it modifies. The
`?` in `(?:` and `(?=` is not the optional quantifier. Braces are not in that
table; they still affect star height.

## Running it

One expression:

```bash
java -cp build org.fire.complexity.RegexComplexity -e '(ab|c)*'
```

```text
(ab|c)*
Size (N) = 3
Star height (H) = 1
Length (L) = 2
Width (W) = 2
Star (*) = 1
Plus (+) = 0
Dot (.) = 0
Question (?) = 0
Or (|) = 1
Character Classes ([]) = 0
Groups ( () ) = 1
```

Several expressions, from `-e`, from files, or from standard input, one per
line. Empty lines are skipped. A syntax error is reported and the rest are
still counted. Two or more expressions print averages:

```bash
java -cp build org.fire.complexity.RegexComplexity -e 'a*' -e 'ab|c'
```

```text
Average Operator Occurrences:
Star (*) = 0.5
Plus (+) = 0.0
Dot (.) = 0.0
Question (?) = 0.0
Or (|) = 0.5
Character Classes ([]) = 0.0
Groups ( () ) = 0.0
Average complexity:
Size (N) = 2.0
Star height (H) = 0.5
Length (L) = 1.5
Width (W) = 1.5
Complements are taken over ASCII (U+0000-U+007F).
```

`-v` prints each expression before the averages. `-h` prints the flags.

From Java, `RegularExpression` parses one expression and
`ExpressionComplexity` holds N, H, L and W. `OperatorUsage` and
`ComplexityUsage` average a list.

## Building

Ant, and a JDK. There are no other dependencies.

```bash
ant          # compiles, writes dist/regex-complexity.jar
ant test     # operator checks, then the complexity checks
ant clean
```

`ant test` runs `MeasureTest` and `ComplexityTest`.

## Where it stops

An unknown family (`\p{L}`, a script, a block), and `\R`, `\X`, `\N`, are
rejected rather than given a made-up size. The same for a bad quantifier
(`{2,1}`, a dangling `*`).

It is not a matcher, and it does not look for super-linear matching. Star
height greater than 1 is a property of the expression, not a verdict on the
engine.

## Further reading

[Four numbers for a regular expression](https://bkarak.wizhut.tech/blog/2026/04102026-2),
a blog post on what the four measures say about an expression, and the
choices behind counting them.

## License

Apache License 2.0. See [License](License).
