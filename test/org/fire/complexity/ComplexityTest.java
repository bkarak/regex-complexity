package org.fire.complexity;

import org.fire.complexity.opusage.RegularExpression;

/**
 * The four Ehrenfeucht-Zeiger measures, checked on their own.
 * Operator counts live in {@link MeasureTest}.
 */
public class ComplexityTest {
	private static int failed;

	public static void main(String[] args) {
		algebra();
		symbols();
		unionAndConcatenation();
		starHeight();
		quantifiersDoNotCopy();
		classes();
		averages();

		if (failed > 0) {
			System.err.println(failed + " failed");
			System.exit(1);
		}
		System.out.println("ok");
	}

	/** The recurrences, without going through the parser. */
	private static void algebra() {
		ExpressionComplexity a = ExpressionComplexity.symbols(1);
		ExpressionComplexity b = ExpressionComplexity.symbols(1);
		same("symbol", a, 1, 0, 1, 1);
		same("zero symbols", ExpressionComplexity.symbols(0), 0, 0, 0, 0);
		same("concat", a.concatenate(b), 2, 0, 2, 1);
		same("union", a.alternate(b), 2, 0, 1, 2);
		same("star", a.unbounded(), 1, 1, 1, 1);
		same("nested star", a.unbounded().unbounded(), 1, 2, 1, 1);
		same("concat identity", ExpressionComplexity.ZERO.concatenate(a), 1, 0, 1, 1);
		same("union identity", ExpressionComplexity.ZERO.alternate(a), 1, 0, 1, 1);

		ExpressionComplexity wide = ExpressionComplexity.symbols(26);
		same("star keeps width", wide.unbounded(), 26, 1, 1, 26);

		ExpressionComplexity sideBySide = a.unbounded().concatenate(b.unbounded());
		eq("side-by-side height", 1, sideBySide.starHeight);
		ExpressionComplexity deeper = a.unbounded().alternate(b.unbounded().unbounded());
		eq("union takes the deeper star", 2, deeper.starHeight);
	}

	private static void symbols() {
		measure("", 0, 0, 0, 0);
		measure("a", 1, 0, 1, 1);
		measure("ab", 2, 0, 2, 1);
		measure("abc", 3, 0, 3, 1);
		measure("()", 0, 0, 0, 0);
		measure("^a$", 1, 0, 1, 1);
		measure("\\ba\\b", 1, 0, 1, 1);
		measure(".", 1, 0, 1, 1);
		measure("\\.", 1, 0, 1, 1);
		measure("\\u00E9", 1, 0, 1, 1);
		measure("(a)\\1", 2, 0, 2, 1);
	}

	private static void unionAndConcatenation() {
		measure("a|b", 2, 0, 1, 2);
		measure("a|b|c", 3, 0, 1, 3);
		measure("ab|c", 3, 0, 2, 2);
		measure("a|", 1, 0, 1, 1);
		measure("|", 0, 0, 0, 0);
		measure("(a|b)(c|d|e)", 5, 0, 2, 3);
		measure("(ab|cde)(f|gh)", 8, 0, 5, 2);
		measure("(a|bb)*", 3, 1, 2, 2);
	}

	private static void starHeight() {
		measure("a*", 1, 1, 1, 1);
		measure("(a*)*", 1, 2, 1, 1);
		measure("((a*)*)*", 1, 3, 1, 1);
		measure("(a*)(b*)", 2, 1, 2, 1);
		measure("(a*b*)*", 2, 2, 2, 1);
		measure("a+", 1, 1, 1, 1);
		measure("(a+)+", 1, 2, 1, 1);
		measure("a?", 1, 0, 1, 1);
		measure("a{2,}", 1, 1, 1, 1);
		measure("(a{2,})*", 1, 2, 1, 1);
		measure("()*", 0, 1, 0, 0);
		// A star changes height only. Size, length and width stay put.
		ExpressionComplexity plain = of("ab");
		ExpressionComplexity starred = of("(ab)*");
		eq("star keeps size", plain.size, starred.size);
		eq("star keeps length", plain.length, starred.length);
		eq("star keeps width", plain.width, starred.width);
		eq("star adds one", plain.starHeight + 1, starred.starHeight);
	}

	private static void quantifiersDoNotCopy() {
		measure("a{10}", 1, 0, 1, 1);
		measure("a{0}", 1, 0, 1, 1);
		measure("(ab){3}", 2, 0, 2, 1);
		measure("(a|b){4}", 2, 0, 1, 2);
		measure("[a-z]{2,4}", 26, 0, 1, 26);
		measure("a{3,}", 1, 1, 1, 1);
	}

	private static void classes() {
		measure("[abc]", 3, 0, 1, 3);
		measure("a|b|c", 3, 0, 1, 3);
		measure("[a-z]", 26, 0, 1, 26);
		measure("[a-z]+", 26, 1, 1, 26);
		measure("[ab]c", 3, 0, 2, 2);
		measure("\\d", 10, 0, 1, 10);
		measure("\\w", 63, 0, 1, 63);
		measure("\\s", 6, 0, 1, 6);
		// \h and \v name code points above ASCII as well, as Java's do.
		measure("\\h", 19, 0, 1, 19);
		measure("\\v", 7, 0, 1, 7);
		measure("\\H", 126, 0, 1, 126);
		measure("\\D", 118, 0, 1, 118);
		measure("[^a]", 127, 0, 1, 127);
		measure("[\\u00E9]", 1, 0, 1, 1);
		measure("[^\\u00E9]", 128, 0, 1, 128);
		// Case flags do not multiply the letters that were written.
		measure("(?i)[a-z]", 26, 0, 1, 26);
		measure("(?i)a", 1, 0, 1, 1);
	}

	private static void averages() {
		ComplexityUsage empty = new ComplexityUsage();
		if (empty.toString().contains("NaN") || !empty.toString().contains("No expressions")) {
			fail("empty report: " + empty);
		}

		ComplexityUsage usage = new ComplexityUsage();
		usage.add("aa");
		usage.add("b|c");
		String report = usage.toString();
		// N = (2+2)/2, H = 0, L = (2+1)/2, W = (1+2)/2
		if (!report.contains("Size (N) = 2.0")
				|| !report.contains("Star height (H) = 0.0")
				|| !report.contains("Length (L) = 1.5")
				|| !report.contains("Width (W) = 1.5")) {
			fail("averages: " + report);
		}
		eq("corpus size", 2, usage.size());
	}

	private static void measure(String regex, long size, int height, long length, long width) {
		same(regex, of(regex), size, height, length, width);
	}

	private static ExpressionComplexity of(String regex) {
		return new RegularExpression(regex).getComplexity();
	}

	private static void same(String label, ExpressionComplexity value, long size, int height, long length, long width) {
		eq(label + " N", size, value.size);
		eq(label + " H", height, value.starHeight);
		eq(label + " L", length, value.length);
		eq(label + " W", width, value.width);
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
