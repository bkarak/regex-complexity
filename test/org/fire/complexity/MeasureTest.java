package org.fire.complexity;

import org.fire.complexity.opusage.OperatorUsage;
import org.fire.complexity.opusage.RegularExpression;
import org.fire.complexity.syntax.RegexParser;

import java.util.regex.Pattern;

/**
 * Checks operator counts and Ehrenfeucht-Zeiger measures, and checks
 * character-class size against the ASCII characters Java's matcher accepts.
 */
public class MeasureTest {
	private static int failed;

	public static void main(String[] args) {
		check("a*", 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
		check("a+", 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1);
		check("a?", 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1);
		check("a??", 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1);
		check("a*?", 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
		check("ab|c", 0, 0, 0, 0, 1, 0, 0, 3, 0, 2, 2);
		check("a||b", 0, 0, 0, 0, 2, 0, 0, 2, 0, 1, 2);
		check("(a|b)*", 1, 0, 0, 0, 1, 0, 1, 2, 1, 1, 2);
		check("(a*)*", 2, 0, 0, 0, 0, 0, 1, 1, 2, 1, 1);
		check("(a*)(b*)", 2, 0, 0, 0, 0, 0, 2, 2, 1, 2, 1);
		check("(?:a)+", 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1);
		check("(?=a)*", 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1);
		check("a{2,4}", 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1);
		check("a{2,}", 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
		check("a{0,}", 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
		check("a.b", 0, 0, 1, 0, 0, 0, 0, 3, 0, 3, 1);
		check("[.]", 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1);
		check(".", 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1);
		check("\\d", 0, 0, 0, 0, 0, 0, 0, 10, 0, 1, 10);
		check("\\d+", 0, 1, 0, 0, 0, 0, 0, 10, 1, 1, 10);
		check("[a-z]", 0, 0, 0, 0, 0, 1, 0, 26, 0, 1, 26);
		check("[]a*]", 0, 0, 0, 0, 0, 1, 0, 3, 0, 1, 3);
		check("[^]]", 0, 0, 0, 0, 0, 1, 0, 127, 0, 1, 127);
		check("[[a]]", 0, 0, 0, 0, 0, 2, 0, 1, 0, 1, 1);
		check("[a-z&&[^aeiou]]", 0, 0, 0, 0, 0, 2, 0, 21, 0, 1, 21);
		check("a\\*", 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 1);
		check("a\\\\*", 1, 0, 0, 0, 0, 0, 0, 2, 1, 2, 1);
		check("(ab|c)*d", 1, 0, 0, 0, 1, 0, 1, 4, 1, 3, 2);
		check("(?i)a", 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1);
		check("(?x)a b", 0, 0, 0, 0, 0, 0, 1, 2, 0, 2, 1);
		check("(?x)a *", 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1);
		check("(a)\\141", 0, 0, 0, 0, 0, 0, 1, 4, 0, 4, 1);
		check("\\Qa+\\E", 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 1);
		check("\\Qab\\E+", 0, 1, 0, 0, 0, 0, 0, 2, 1, 2, 1);
		check("^a$", 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1);
		check("", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		check("[a&&b]", 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0);
		// Inline flags end with the group that holds them.
		check("((?x)a) b", 0, 0, 0, 0, 0, 0, 2, 3, 0, 3, 1);
		check("(?x:a) b", 0, 0, 0, 0, 0, 0, 1, 3, 0, 3, 1);
		check("[\\Qa-]\\E]", 0, 0, 0, 0, 0, 1, 0, 3, 0, 1, 3);

		expectFail("*");
		expectFail("a{2,1}");
		expectFail("a**");
		expectFail("[a--c]");
		expectFail("[&&]");
		expectFail("\\0");

		oracle("[a-c]");
		oracle("[^a]");
		oracle("\\d");
		oracle("\\D");
		oracle("\\w");
		oracle("\\W");
		oracle("\\s");
		oracle("\\S");
		oracle("[a-z&&[^aeiou]]");
		oracle("[]a*]");
		oracle("[^]]");
		oracle("[[a]]");
		oracle("[a-c-e]");
		oracle("[abc&&def]");
		oracle("[a-d&&[c-f]]");
		oracle("[\\d]");
		oracle("\\p{Digit}");
		oracle("\\p{Lower}");
		oracle("\\p{Punct}");
		oracle("\\p{Space}");
		oracle("[a-]");
		oracle("[-a]");
		oracle("[]]");
		oracle("[&&a]");
		oracle("[a&&]");
		oracle("[a&&&]");
		oracle("[a&&&&b]");
		oracle("(?x)[ a ]");
		oracle("[a-z&&[^aeiou]&&[^b]]");
		oracle("[--a]");
		oracle("\\p{ASCII}");
		oracle("\\H");
		oracle("\\V");
		oracle("[\\Qa-c\\E]");

		// The dot is one symbol, whatever Java's matcher accepts.
		RegularExpression dot = new RegularExpression(".");
		eq("dot size", 1, dot.getComplexity().size);

		OperatorUsage empty = new OperatorUsage();
		if (empty.toString().contains("NaN") || !empty.toString().contains("No expressions")) {
			fail("empty operator report: " + empty);
		}
		ComplexityUsage emptyComplexity = new ComplexityUsage();
		if (emptyComplexity.toString().contains("NaN") || !emptyComplexity.toString().contains("No expressions")) {
			fail("empty complexity report: " + emptyComplexity);
		}

		OperatorUsage usage = new OperatorUsage();
		usage.add("a*");
		usage.add("(b|c)+");
		if (!usage.toString().contains("Star (*) = 0.5") || !usage.toString().contains("Plus (+) = 0.5")) {
			fail("averages: " + usage);
		}
		ComplexityUsage complexity = new ComplexityUsage();
		complexity.add("a*");
		complexity.add("ab|c");
		// N = (1+3)/2 = 2, H = (1+0)/2 = 0.5, L = (1+2)/2 = 1.5, W = (1+2)/2 = 1.5
		String report = complexity.toString();
		if (!report.contains("Size (N) = 2.0") || !report.contains("Star height (H) = 0.5")
				|| !report.contains("Length (L) = 1.5") || !report.contains("Width (W) = 1.5")) {
			fail("complexity averages: " + report);
		}

		if (failed > 0) {
			System.err.println(failed + " failed");
			System.exit(1);
		}
		System.out.println("ok");
	}

	/** star, plus, dot, question, or, class, group, N, H, L, W */
	private static void check(String regex, int star, int plus, int dot, int question, int or, int classes, int group,
			long size, int height, long length, long width) {
		RegularExpression expression = new RegularExpression(regex);
		OperatorProfile operators = expression.getOperators();
		ExpressionComplexity complexity = expression.getComplexity();
		eq(regex + " star", star, operators.star);
		eq(regex + " plus", plus, operators.plus);
		eq(regex + " dot", dot, operators.dot);
		eq(regex + " question", question, operators.question);
		eq(regex + " or", or, operators.or);
		eq(regex + " class", classes, operators.characterClass);
		eq(regex + " group", group, operators.group);
		eq(regex + " N", size, complexity.size);
		eq(regex + " H", height, complexity.starHeight);
		eq(regex + " L", length, complexity.length);
		eq(regex + " W", width, complexity.width);
	}

	private static void oracle(String regex) {
		int ascii = 0;
		Pattern pattern = Pattern.compile(regex);
		for (int c = 0; c < 128; c++) {
			if (pattern.matcher(String.valueOf((char) c)).matches()) {
				ascii++;
			}
		}
		ExpressionComplexity complexity = RegexParser.parse(regex).complexity();
		eq(regex + " ascii N", ascii, complexity.size);
		if (ascii == 0) {
			eq(regex + " L", 0, complexity.length);
			eq(regex + " W", 0, complexity.width);
		} else {
			eq(regex + " L", 1, complexity.length);
			eq(regex + " W", ascii, complexity.width);
		}
	}

	private static void expectFail(String regex) {
		try {
			RegexParser.parse(regex);
			fail("accepted " + regex);
		} catch (RegexSyntaxException expected) {
			// rejected
		}
	}

	private static void eq(String label, long expected, long actual) {
		if (expected != actual) {
			fail(label + " expected " + expected + " but was " + actual);
		}
	}

	private static void fail(String message) {
		failed++;
		System.err.println(message);
	}
}
