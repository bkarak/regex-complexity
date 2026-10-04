package org.fire.complexity;

/**
 * Ehrenfeucht and Zeiger's four measures of a regular expression
 * (Journal of Computer and System Sciences, 1976).
 *
 * <p>Size is the number of alphabetical symbols written in the expression.
 * Star height is the nesting depth of unbounded repetition ({@code *},
 * {@code +}, {@code {n,}}). Length is the longest nonrepeating path:
 * concatenation adds, alternation takes the longer branch, and a quantifier
 * does not walk its body again. Width is the dual of length: alternation
 * adds, concatenation takes the wider branch.
 *
 * <p>Complements ({@code [^...]}, {@code \D}, {@code \W}, {@code \S},
 * {@code \P}) are taken over the ASCII alphabet, U+0000 through U+007F.
 * A code point written above that range still counts. The dot is one
 * symbol. A finite bound such as {@code {2,4}} is not unrolled, and case
 * flags do not expand letters.
 */
public final class ExpressionComplexity {
	/** Universe for complemented classes and shorthands. */
	public static final int COMPLEMENT_ALPHABET = 128;

	public static final ExpressionComplexity ZERO = new ExpressionComplexity(0, 0, 0, 0);

	public final long size;
	public final int starHeight;
	public final long length;
	public final long width;

	public ExpressionComplexity(long size, int starHeight, long length, long width) {
		this.size = size;
		this.starHeight = starHeight;
		this.length = length;
		this.width = width;
	}

	/** One position that unions {@code n} alphabetical symbols. Zero symbols is the empty measure. */
	public static ExpressionComplexity symbols(long n) {
		if (n <= 0) {
			return ZERO;
		}
		return new ExpressionComplexity(n, 0, 1, n);
	}

	public ExpressionComplexity concatenate(ExpressionComplexity other) {
		return new ExpressionComplexity(
				size + other.size,
				Math.max(starHeight, other.starHeight),
				length + other.length,
				Math.max(width, other.width));
	}

	public ExpressionComplexity alternate(ExpressionComplexity other) {
		return new ExpressionComplexity(
				size + other.size,
				Math.max(starHeight, other.starHeight),
				Math.max(length, other.length),
				width + other.width);
	}

	/** Unbounded repetition: star height grows, the path is not repeated. */
	public ExpressionComplexity unbounded() {
		return new ExpressionComplexity(size, starHeight + 1, length, width);
	}

	public String toString() {
		return "Size (N) = " + size + "\n"
				+ "Star height (H) = " + starHeight + "\n"
				+ "Length (L) = " + length + "\n"
				+ "Width (W) = " + width + "\n";
	}
}
