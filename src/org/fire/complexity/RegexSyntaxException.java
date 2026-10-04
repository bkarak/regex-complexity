package org.fire.complexity;

/**
 * The expression is not a regular expression this calculator accepts.
 */
public class RegexSyntaxException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	private final int index;

	public RegexSyntaxException(String message, int index) {
		super(message + " at index " + index);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}
