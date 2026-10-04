package org.fire.complexity.syntax;

import org.fire.complexity.ExpressionComplexity;
import org.fire.complexity.OperatorProfile;
import org.fire.complexity.RegexSyntaxException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the regular-expression syntax counted by this tool and returns
 * its operator profile together with its Ehrenfeucht-Zeiger measures.
 */
public final class RegexParser {
	private final String s;
	private final int n;
	private final int groupTotal;
	private int i;
	private boolean extended;
	private int capturingSeen;
	private final HashSet<String> names = new HashSet<String>();

	private RegexParser(String expression, int groupTotal) {
		this.s = expression;
		this.n = expression.length();
		this.groupTotal = groupTotal;
	}

	public static Analysis parse(String expression) {
		if (expression == null) {
			throw new NullPointerException("expression");
		}
		RegexParser count = new RegexParser(expression, 0);
		count.parseTop();
		RegexParser parser = new RegexParser(expression, count.capturingSeen);
		return parser.parseTop();
	}

	private Analysis parseTop() {
		Expr expr = parseAlt();
		skipExtended();
		if (i < n) {
			if (rawPeek() == ')') {
				throw error("Unmatched closing ')'");
			}
			throw error("Unexpected character");
		}
		return new Analysis(s, expr.operators(), expr.complexity());
	}

	private Expr parseAlt() {
		ArrayList<Expr> alts = new ArrayList<Expr>();
		alts.add(parseConcat());
		while (true) {
			skipExtended();
			if (rawPeek() != '|') {
				break;
			}
			i++;
			alts.add(parseConcat());
		}
		if (alts.size() == 1) {
			return alts.get(0);
		}
		return new Union(alts);
	}

	private Expr parseConcat() {
		ArrayList<Expr> parts = new ArrayList<Expr>();
		while (true) {
			skipExtended();
			if (i >= n || rawPeek() == '|' || rawPeek() == ')') {
				break;
			}
			parts.add(parsePiece());
		}
		if (parts.isEmpty()) {
			return Empty.INSTANCE;
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		return new Concat(parts);
	}

	private Expr parsePiece() {
		skipExtended();
		if (startsWith("\\Q")) {
			return parseQuoted();
		}
		Expr atom = parseAtom();
		skipExtended();
		Quantifier quantifier = tryQuantifier();
		if (quantifier == null) {
			return atom;
		}
		return new Repeat(atom, quantifier);
	}

	private Expr parseQuoted() {
		i += 2;
		ArrayList<Expr> symbols = new ArrayList<Expr>();
		while (i < n && !startsWith("\\E")) {
			rawRead();
			symbols.add(Symbol.INSTANCE);
		}
		if (startsWith("\\E")) {
			i += 2;
		}
		if (symbols.isEmpty()) {
			skipExtended();
			if (tryQuantifier() != null) {
				throw error("Dangling meta character");
			}
			return Empty.INSTANCE;
		}
		skipExtended();
		Quantifier quantifier = tryQuantifier();
		int last = symbols.size() - 1;
		Expr tail = symbols.get(last);
		if (quantifier != null) {
			tail = new Repeat(tail, quantifier);
		}
		if (symbols.size() == 1) {
			return tail;
		}
		symbols.set(last, tail);
		return new Concat(symbols);
	}

	private Expr parseAtom() {
		skipExtended();
		if (i >= n) {
			throw error("Unexpected end");
		}
		int cp = rawPeek();
		if (cp == '(') {
			i++;
			return parseGroup();
		}
		if (cp == '[') {
			i++;
			ClassParse parsed = parseClass(true, true);
			return new CharClass(parsed.set.size(), parsed.brackets);
		}
		if (cp == '.') {
			i++;
			return Dot.INSTANCE;
		}
		if (cp == '\\') {
			i++;
			return escapeToExpr(parseEscape(false));
		}
		if (cp == '^' || cp == '$') {
			i++;
			return Anchor.INSTANCE;
		}
		if (cp == '*' || cp == '+' || cp == '?') {
			throw error("Dangling meta character '" + (char) cp + "'");
		}
		if (cp == '{') {
			throw error("Dangling meta character '{'");
		}
		rawRead();
		return Symbol.INSTANCE;
	}

	private Expr parseGroup() {
		boolean capturing = true;
		boolean savedExtended = extended;
		if (rawPeek() == '?') {
			i++;
			int kind = rawPeek();
			if (kind == ':') {
				i++;
				capturing = false;
			} else if (kind == '=' || kind == '!') {
				i++;
				capturing = false;
			} else if (kind == '>') {
				i++;
				capturing = false;
			} else if (kind == '<') {
				i++;
				int next = rawPeek();
				if (next == '=' || next == '!') {
					i++;
					capturing = false;
				} else {
					String name = readGroupName();
					expect('>');
					if (!names.add(name)) {
						throw error("Named capturing group <" + name + "> is already defined");
					}
				}
			} else {
				capturing = false;
				if (!parseFlags()) {
					expect(')');
					return new Group(Empty.INSTANCE);
				}
			}
		}
		if (capturing) {
			capturingSeen++;
		}
		Expr body = parseAlt();
		expect(')');
		// Inline flags set inside a group end with it, as in java.util.regex.
		extended = savedExtended;
		return new Group(body);
	}

	/** @return true when the flags are scoped to a following group body */
	private boolean parseFlags() {
		boolean off = false;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '-') {
				off = true;
				i++;
				continue;
			}
			if (c == ':') {
				i++;
				return true;
			}
			if (c == ')') {
				return false;
			}
			if ("idmsuxU".indexOf(c) < 0) {
				throw error("Unknown inline modifier");
			}
			if (c == 'x') {
				extended = !off;
			}
			i++;
		}
		throw error("Unclosed group");
	}

	private String readGroupName() {
		int start = rawPeek();
		if (!isNameStart(start)) {
			throw error("Illegal group name");
		}
		StringBuilder name = new StringBuilder();
		name.appendCodePoint(rawRead());
		while (isNamePart(rawPeek())) {
			name.appendCodePoint(rawRead());
		}
		return name.toString();
	}

	private Quantifier tryQuantifier() {
		skipExtended();
		if (i >= n) {
			return null;
		}
		char c = s.charAt(i);
		Quantifier quantifier = null;
		if (c == '*') {
			i++;
			quantifier = Quantifier.star();
		} else if (c == '+') {
			i++;
			quantifier = Quantifier.plus();
		} else if (c == '?') {
			i++;
			quantifier = Quantifier.optional();
		} else if (c == '{') {
			quantifier = parseBrace();
		}
		if (quantifier == null) {
			return null;
		}
		if (i < n && (s.charAt(i) == '?' || s.charAt(i) == '+')) {
			i++;
		}
		return quantifier;
	}

	private Quantifier parseBrace() {
		int start = i;
		i++;
		if (i >= n || !isDigit(s.charAt(i))) {
			i = start;
			throw error("Illegal repetition");
		}
		int min = readNumber();
		int max = min;
		if (i < n && s.charAt(i) == ',') {
			i++;
			if (i < n && isDigit(s.charAt(i))) {
				max = readNumber();
			} else {
				max = -1;
			}
		}
		if (i >= n || s.charAt(i) != '}') {
			throw error("Illegal repetition");
		}
		i++;
		if (max >= 0 && min > max) {
			throw error("Illegal repetition range");
		}
		return Quantifier.brace(max);
	}

	private int readNumber() {
		long value = 0;
		int digits = 0;
		while (i < n && isDigit(s.charAt(i))) {
			value = value * 10 + (s.charAt(i) - '0');
			if (value > Integer.MAX_VALUE) {
				throw error("Illegal repetition");
			}
			i++;
			digits++;
		}
		if (digits == 0) {
			throw error("Illegal repetition");
		}
		return (int) value;
	}

	private ClassParse parseClass(boolean bracketed, boolean consumeClose) {
		int brackets = bracketed ? 1 : 0;
		ClassSet prev = null;
		ClassSet bits = new ClassSet();
		boolean hasBits = false;
		boolean negate = false;
		if (bracketed && peek() == '^') {
			read();
			negate = true;
		}
		while (true) {
			if (peek() < 0) {
				throw error("Unclosed character class");
			}
			if (peek() == '[') {
				read();
				ClassParse nested = parseClass(true, true);
				prev = union(prev, nested.set);
				brackets += nested.brackets;
				continue;
			}
			if (atIntersection()) {
				i += 2;
				ClassParse right = parseIntersectionOperands();
				brackets += right.brackets;
				if (hasBits) {
					prev = union(prev, bits);
					bits = new ClassSet();
					hasBits = false;
				}
				if (prev == null) {
					if (right.set == null) {
						throw error("Bad class syntax");
					}
					prev = right.set;
				} else if (right.set == null) {
					// A trailing "&&" intersects the class with itself.
				} else {
					prev = prev.intersect(right.set);
				}
				continue;
			}
			if (peek() == ']' && (prev != null || hasBits)) {
				if (consumeClose) {
					read();
				}
				ClassSet result = prev == null ? bits : (hasBits ? prev.union(bits) : prev);
				if (negate) {
					result = result.complementAscii();
				}
				return new ClassParse(result, brackets);
			}
			if (!bracketed && peek() == ']' ) {
				ClassSet result = prev == null ? bits : (hasBits ? prev.union(bits) : prev);
				return new ClassParse(result, brackets);
			}
			if (parseClassElement(bits)) {
				hasBits = true;
			} else {
				// parseClassElement reports a property through the field below.
				prev = union(prev, pendingSet);
				pendingSet = null;
			}
		}
	}

	private ClassSet pendingSet;

	/** @return true when the element was a single character or a range, stored in {@code bits} */
	private boolean parseClassElement(ClassSet bits) {
		if (peek() == '\\' && startsWith("\\Q")) {
			i += 2;
			while (i < n && !startsWith("\\E")) {
				bits.add(rawRead());
			}
			if (startsWith("\\E")) {
				i += 2;
			}
			return true;
		}
		if (peek() == '\\') {
			read();
			Escape escape = parseEscape(true);
			if (escape.kind == Escape.Kind.SET) {
				pendingSet = escape.set;
				return false;
			}
			if (escape.kind != Escape.Kind.CHAR) {
				throw error("Illegal/unsupported escape sequence");
			}
			addCharOrRange(bits, escape.cp);
			return true;
		}
		addCharOrRange(bits, read());
		return true;
	}

	private void addCharOrRange(ClassSet bits, int start) {
		if (!atRangeOperator()) {
			bits.add(start);
			return;
		}
		read();
		int end;
		if (peek() == '\\') {
			read();
			Escape escape = parseEscape(true);
			if (escape.kind != Escape.Kind.CHAR) {
				throw error("Illegal character range");
			}
			end = escape.cp;
		} else {
			end = read();
		}
		if (end < start) {
			throw error("Illegal character range");
		}
		bits.addRange(start, end);
	}

	private ClassParse parseIntersectionOperands() {
		ClassSet right = null;
		int brackets = 0;
		while (peek() >= 0 && peek() != ']' && peek() != '&') {
			if (peek() == '[') {
				read();
				ClassParse nested = parseClass(true, true);
				right = union(right, nested.set);
				brackets += nested.brackets;
			} else {
				ClassParse implicit = parseClass(false, false);
				right = union(right, implicit.set);
				brackets += implicit.brackets;
			}
		}
		return new ClassParse(right, brackets);
	}

	private boolean atRangeOperator() {
		int saved = i;
		boolean savedExtended = extended;
		if (peek() != '-') {
			i = saved;
			extended = savedExtended;
			return false;
		}
		read();
		int end = peek();
		i = saved;
		extended = savedExtended;
		return end >= 0 && end != ']' && end != '[';
	}

	private boolean atIntersection() {
		int saved = i;
		boolean savedExtended = extended;
		skipExtended();
		boolean found = i + 1 < n && s.charAt(i) == '&' && s.charAt(i + 1) == '&';
		if (!found) {
			i = saved;
			extended = savedExtended;
		}
		return found;
	}

	private Expr escapeToExpr(Escape escape) {
		if (escape.kind == Escape.Kind.CHAR) {
			return Symbol.INSTANCE;
		}
		if (escape.kind == Escape.Kind.SET) {
			return new CharClass(escape.set.size(), 0);
		}
		if (escape.kind == Escape.Kind.ANCHOR) {
			return Anchor.INSTANCE;
		}
		return BackRef.INSTANCE;
	}

	private Escape parseEscape(boolean inClass) {
		if (i >= n) {
			throw error("Trailing backslash");
		}
		int cp = rawRead();
		switch (cp) {
			case 'd':
				return Escape.set(Posix.DIGIT);
			case 'D':
				return Escape.set(Posix.DIGIT.complementAscii());
			case 'w':
				return Escape.set(Posix.WORD);
			case 'W':
				return Escape.set(Posix.WORD.complementAscii());
			case 's':
				return Escape.set(Posix.SPACE);
			case 'S':
				return Escape.set(Posix.SPACE.complementAscii());
			case 'h':
				return Escape.set(Posix.HORIZONTAL);
			case 'H':
				return Escape.set(Posix.HORIZONTAL.complementAscii());
			case 'v':
				return Escape.set(Posix.VERTICAL);
			case 'V':
				return Escape.set(Posix.VERTICAL.complementAscii());
			case 'p':
				return Escape.set(readProperty(false));
			case 'P':
				return Escape.set(readProperty(true));
			case 't':
				return Escape.ch('\t');
			case 'n':
				return Escape.ch('\n');
			case 'r':
				return Escape.ch('\r');
			case 'f':
				return Escape.ch('\f');
			case 'a':
				return Escape.ch('\007');
			case 'e':
				return Escape.ch('\033');
			case 'x':
				return Escape.ch(readHexEscape());
			case 'u':
				return Escape.ch(readHex(4));
			case '0':
				return Escape.ch(readOctal());
			case 'c':
				if (i >= n) {
					throw error("Illegal/unsupported escape sequence");
				}
				return Escape.ch(rawRead() ^ 64);
			case 'A':
			case 'B':
			case 'G':
			case 'Z':
			case 'z':
				if (inClass) {
					throw error("Illegal/unsupported escape sequence");
				}
				return Escape.anchor();
			case 'b':
				if (inClass) {
					throw error("Illegal/unsupported escape sequence");
				}
				return Escape.anchor();
			case 'k':
				if (inClass) {
					throw error("Illegal/unsupported escape sequence");
				}
				parseNamedBackref();
				return Escape.backref();
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				if (inClass) {
					throw error("Illegal/unsupported escape sequence");
				}
				finishBackref(cp - '0');
				return Escape.backref();
			default:
				if (Character.isLetter(cp)) {
					throw error("Illegal/unsupported escape sequence");
				}
				return Escape.ch(cp);
		}
	}

	private void finishBackref(int ref) {
		while (i < n && isDigit(s.charAt(i))) {
			int next = ref * 10 + (s.charAt(i) - '0');
			if (groupTotal < next) {
				break;
			}
			ref = next;
			i++;
		}
	}

	private void parseNamedBackref() {
		expect('<');
		String name = readGroupName();
		expect('>');
		if (!names.contains(name)) {
			throw error("named capturing group <" + name + "> does not exist");
		}
	}

	private ClassSet readProperty(boolean complement) {
		if (rawPeek() != '{') {
			throw error("Unknown character family");
		}
		rawRead();
		StringBuilder name = new StringBuilder();
		while (i < n && rawPeek() != '}') {
			name.appendCodePoint(rawRead());
		}
		expect('}');
		if (name.length() == 0) {
			throw error("Empty character family");
		}
		ClassSet set = Posix.byName(name.toString());
		if (set == null) {
			throw error("Unknown character family {" + name + "}");
		}
		return complement ? set.complementAscii() : set;
	}

	private int readHexEscape() {
		if (rawPeek() == '{') {
			rawRead();
			int value = 0;
			int digits = 0;
			while (i < n && rawPeek() != '}') {
				int digit = Character.digit(rawPeek(), 16);
				if (digit < 0) {
					throw error("Illegal hexadecimal escape");
				}
				value = (value << 4) + digit;
				if (value > 0x10FFFF) {
					throw error("Illegal hexadecimal escape");
				}
				rawRead();
				digits++;
			}
			if (digits == 0) {
				throw error("Illegal hexadecimal escape");
			}
			expect('}');
			return value;
		}
		return readHex(2);
	}

	private int readHex(int digits) {
		int value = 0;
		for (int k = 0; k < digits; k++) {
			if (i >= n) {
				throw error("Illegal hexadecimal escape");
			}
			int digit = Character.digit(rawPeek(), 16);
			if (digit < 0) {
				throw error("Illegal hexadecimal escape");
			}
			value = (value << 4) + digit;
			rawRead();
		}
		return value;
	}

	private int readOctal() {
		int value = 0;
		int digits = 0;
		while (digits < 3 && i < n && s.charAt(i) >= '0' && s.charAt(i) <= '7') {
			value = value * 8 + (s.charAt(i) - '0');
			i++;
			digits++;
		}
		if (digits == 0) {
			throw error("Illegal octal escape sequence");
		}
		return value;
	}

	private void skipExtended() {
		if (!extended) {
			return;
		}
		while (i < n) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B') {
				i++;
				continue;
			}
			if (c == '#') {
				while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
					i++;
				}
				continue;
			}
			break;
		}
	}

	private int peek() {
		skipExtended();
		return rawPeek();
	}

	private int read() {
		int cp = peek();
		if (cp < 0) {
			throw error("Unexpected end");
		}
		i += Character.charCount(cp);
		return cp;
	}

	private int rawPeek() {
		if (i >= n) {
			return -1;
		}
		return s.codePointAt(i);
	}

	private int rawRead() {
		int cp = rawPeek();
		if (cp < 0) {
			throw error("Unexpected end");
		}
		i += Character.charCount(cp);
		return cp;
	}

	private boolean startsWith(String token) {
		return s.startsWith(token, i);
	}

	private void expect(char c) {
		if (rawPeek() != c) {
			throw error("Expected '" + c + "'");
		}
		i++;
	}

	private RegexSyntaxException error(String message) {
		return new RegexSyntaxException(message, Math.min(i, n));
	}

	private static boolean isDigit(int cp) {
		return cp >= '0' && cp <= '9';
	}

	private static boolean isNameStart(int cp) {
		return (cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z');
	}

	private static boolean isNamePart(int cp) {
		return isNameStart(cp) || isDigit(cp);
	}

	private static ClassSet union(ClassSet left, ClassSet right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return left.union(right);
	}

	public static final class Analysis {
		private final String expression;
		private final OperatorProfile operators;
		private final ExpressionComplexity complexity;

		Analysis(String expression, OperatorProfile operators, ExpressionComplexity complexity) {
			this.expression = expression;
			this.operators = operators;
			this.complexity = complexity;
		}

		public String getExpression() {
			return expression;
		}

		public OperatorProfile operators() {
			return operators;
		}

		public ExpressionComplexity complexity() {
			return complexity;
		}
	}

	private abstract static class Expr {
		abstract OperatorProfile operators();

		abstract ExpressionComplexity complexity();
	}

	private static final class Empty extends Expr {
		static final Empty INSTANCE = new Empty();

		OperatorProfile operators() {
			return OperatorProfile.ZERO;
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.ZERO;
		}
	}

	private static final class Anchor extends Expr {
		static final Anchor INSTANCE = new Anchor();

		OperatorProfile operators() {
			return OperatorProfile.ZERO;
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.ZERO;
		}
	}

	private static final class BackRef extends Expr {
		static final BackRef INSTANCE = new BackRef();

		OperatorProfile operators() {
			return OperatorProfile.ZERO;
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.symbols(1);
		}
	}

	private static final class Dot extends Expr {
		static final Dot INSTANCE = new Dot();

		OperatorProfile operators() {
			return OperatorProfile.dot();
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.symbols(1);
		}
	}

	private static final class Symbol extends Expr {
		static final Symbol INSTANCE = new Symbol();

		OperatorProfile operators() {
			return OperatorProfile.ZERO;
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.symbols(1);
		}
	}

	private static final class CharClass extends Expr {
		private final long cardinality;
		private final int brackets;

		CharClass(long cardinality, int brackets) {
			this.cardinality = cardinality;
			this.brackets = brackets;
		}

		OperatorProfile operators() {
			return OperatorProfile.characterClass(brackets);
		}

		ExpressionComplexity complexity() {
			return ExpressionComplexity.symbols(cardinality);
		}
	}

	private static final class Group extends Expr {
		private final Expr body;

		Group(Expr body) {
			this.body = body;
		}

		OperatorProfile operators() {
			return body.operators().add(OperatorProfile.group());
		}

		ExpressionComplexity complexity() {
			return body.complexity();
		}
	}

	private static final class Concat extends Expr {
		private final ArrayList<Expr> parts;

		Concat(ArrayList<Expr> parts) {
			this.parts = parts;
		}

		OperatorProfile operators() {
			OperatorProfile profile = OperatorProfile.ZERO;
			for (Expr part : parts) {
				profile = profile.add(part.operators());
			}
			return profile;
		}

		ExpressionComplexity complexity() {
			ExpressionComplexity value = ExpressionComplexity.ZERO;
			for (Expr part : parts) {
				value = value.concatenate(part.complexity());
			}
			return value;
		}
	}

	private static final class Union extends Expr {
		private final ArrayList<Expr> alts;

		Union(ArrayList<Expr> alts) {
			this.alts = alts;
		}

		OperatorProfile operators() {
			OperatorProfile profile = OperatorProfile.or(alts.size() - 1);
			for (Expr alt : alts) {
				profile = profile.add(alt.operators());
			}
			return profile;
		}

		ExpressionComplexity complexity() {
			ExpressionComplexity value = ExpressionComplexity.ZERO;
			for (Expr alt : alts) {
				value = value.alternate(alt.complexity());
			}
			return value;
		}
	}

	private static final class Repeat extends Expr {
		private final Expr body;
		private final Quantifier quantifier;

		Repeat(Expr body, Quantifier quantifier) {
			this.body = body;
			this.quantifier = quantifier;
		}

		OperatorProfile operators() {
			OperatorProfile profile = body.operators();
			if (quantifier.kind == Quantifier.Kind.STAR) {
				return profile.add(OperatorProfile.star());
			}
			if (quantifier.kind == Quantifier.Kind.PLUS) {
				return profile.add(OperatorProfile.plus());
			}
			if (quantifier.kind == Quantifier.Kind.OPTIONAL) {
				return profile.add(OperatorProfile.question());
			}
			return profile;
		}

		ExpressionComplexity complexity() {
			ExpressionComplexity value = body.complexity();
			if (quantifier.max < 0) {
				return value.unbounded();
			}
			return value;
		}
	}

	private static final class Quantifier {
		enum Kind { STAR, PLUS, OPTIONAL, BRACE }

		final Kind kind;
		final int max;

		private Quantifier(Kind kind, int max) {
			this.kind = kind;
			this.max = max;
		}

		static Quantifier star() {
			return new Quantifier(Kind.STAR, -1);
		}

		static Quantifier plus() {
			return new Quantifier(Kind.PLUS, -1);
		}

		static Quantifier optional() {
			return new Quantifier(Kind.OPTIONAL, 1);
		}

		static Quantifier brace(int max) {
			return new Quantifier(Kind.BRACE, max);
		}
	}

	private static final class ClassParse {
		final ClassSet set;
		final int brackets;

		ClassParse(ClassSet set, int brackets) {
			this.set = set;
			this.brackets = brackets;
		}
	}

	private static final class Escape {
		enum Kind { CHAR, SET, ANCHOR, BACKREF }

		final Kind kind;
		final int cp;
		final ClassSet set;

		private Escape(Kind kind, int cp, ClassSet set) {
			this.kind = kind;
			this.cp = cp;
			this.set = set;
		}

		static Escape ch(int cp) {
			return new Escape(Kind.CHAR, cp, null);
		}

		static Escape set(ClassSet set) {
			return new Escape(Kind.SET, -1, set);
		}

		static Escape anchor() {
			return new Escape(Kind.ANCHOR, -1, null);
		}

		static Escape backref() {
			return new Escape(Kind.BACKREF, -1, null);
		}
	}

	static final class ClassSet {
		private final BitSet ascii = new BitSet(ExpressionComplexity.COMPLEMENT_ALPHABET);
		private final HashSet<Integer> above = new HashSet<Integer>();

		void add(int cp) {
			if (cp < ExpressionComplexity.COMPLEMENT_ALPHABET) {
				ascii.set(cp);
			} else {
				above.add(Integer.valueOf(cp));
			}
		}

		void addRange(int from, int to) {
			for (int cp = from; cp <= to; cp++) {
				add(cp);
			}
		}

		ClassSet union(ClassSet other) {
			ClassSet result = new ClassSet();
			result.ascii.or(ascii);
			result.ascii.or(other.ascii);
			result.above.addAll(above);
			result.above.addAll(other.above);
			return result;
		}

		ClassSet intersect(ClassSet other) {
			ClassSet result = new ClassSet();
			result.ascii.or(ascii);
			result.ascii.and(other.ascii);
			result.above.addAll(above);
			result.above.retainAll(other.above);
			return result;
		}

		ClassSet complementAscii() {
			ClassSet result = new ClassSet();
			for (int cp = 0; cp < ExpressionComplexity.COMPLEMENT_ALPHABET; cp++) {
				if (!ascii.get(cp)) {
					result.ascii.set(cp);
				}
			}
			return result;
		}

		long size() {
			return ascii.cardinality() + above.size();
		}
	}

	static final class Posix {
		static final ClassSet DIGIT = range('0', '9');
		static final ClassSet LOWER = range('a', 'z');
		static final ClassSet UPPER = range('A', 'Z');
		static final ClassSet ASCII = range(0, ExpressionComplexity.COMPLEMENT_ALPHABET - 1);
		static final ClassSet ALPHA = LOWER.union(UPPER);
		static final ClassSet ALNUM = ALPHA.union(DIGIT);
		static final ClassSet WORD = ALNUM.union(single('_'));
		static final ClassSet SPACE = chars(' ', '\t', '\n', '\u000B', '\f', '\r');
		static final ClassSet BLANK = chars(' ', '\t');
		static final ClassSet HORIZONTAL = chars(' ', '\t', '\u00A0', '\u1680', '\u180E', '\u202F', '\u205F', '\u3000')
				.union(range(0x2000, 0x200A));
		static final ClassSet VERTICAL = chars('\n', '\u000B', '\f', '\r', '\u0085', '\u2028', '\u2029');
		static final ClassSet CNTRL = range(0, 31).union(single(127));
		static final ClassSet XDIGIT = DIGIT.union(range('a', 'f')).union(range('A', 'F'));
		static final ClassSet PUNCT = chars("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
		static final ClassSet GRAPH = ALNUM.union(PUNCT);
		static final ClassSet PRINT = GRAPH.union(single(' '));

		private static final Map<String, ClassSet> BY_NAME = new HashMap<String, ClassSet>();

		static {
			put("lower", LOWER);
			put("upper", UPPER);
			put("ascii", ASCII);
			put("alpha", ALPHA);
			put("digit", DIGIT);
			put("alnum", ALNUM);
			put("punct", PUNCT);
			put("graph", GRAPH);
			put("print", PRINT);
			put("blank", BLANK);
			put("cntrl", CNTRL);
			put("xdigit", XDIGIT);
			put("space", SPACE);
		}

		private static void put(String name, ClassSet set) {
			BY_NAME.put(name, set);
		}

		static ClassSet byName(String name) {
			return BY_NAME.get(name.toLowerCase(Locale.ROOT));
		}

		private static ClassSet range(int from, int to) {
			ClassSet set = new ClassSet();
			set.addRange(from, to);
			return set;
		}

		private static ClassSet single(int cp) {
			ClassSet set = new ClassSet();
			set.add(cp);
			return set;
		}

		private static ClassSet chars(String text) {
			ClassSet set = new ClassSet();
			for (int k = 0; k < text.length(); ) {
				int cp = text.codePointAt(k);
				set.add(cp);
				k += Character.charCount(cp);
			}
			return set;
		}

		private static ClassSet chars(char... values) {
			ClassSet set = new ClassSet();
			for (char value : values) {
				set.add(value);
			}
			return set;
		}
	}
}
