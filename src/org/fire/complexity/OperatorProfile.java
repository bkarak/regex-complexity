package org.fire.complexity;

/**
 * How many times each operator of the FIRE/J occurrence table is written.
 * A lazy or possessive marker is part of the quantifier it modifies.
 * {@code ?} inside {@code (?:} and {@code (?=} is not the optional quantifier.
 */
public final class OperatorProfile {
	public static final OperatorProfile ZERO = new OperatorProfile(0, 0, 0, 0, 0, 0, 0);

	public final int star;
	public final int plus;
	public final int dot;
	public final int question;
	public final int or;
	public final int characterClass;
	public final int group;

	public OperatorProfile(int star, int plus, int dot, int question, int or, int characterClass, int group) {
		this.star = star;
		this.plus = plus;
		this.dot = dot;
		this.question = question;
		this.or = or;
		this.characterClass = characterClass;
		this.group = group;
	}

	public OperatorProfile add(OperatorProfile other) {
		return new OperatorProfile(
				star + other.star,
				plus + other.plus,
				dot + other.dot,
				question + other.question,
				or + other.or,
				characterClass + other.characterClass,
				group + other.group);
	}

	public static OperatorProfile star() {
		return new OperatorProfile(1, 0, 0, 0, 0, 0, 0);
	}

	public static OperatorProfile plus() {
		return new OperatorProfile(0, 1, 0, 0, 0, 0, 0);
	}

	public static OperatorProfile dot() {
		return new OperatorProfile(0, 0, 1, 0, 0, 0, 0);
	}

	public static OperatorProfile question() {
		return new OperatorProfile(0, 0, 0, 1, 0, 0, 0);
	}

	public static OperatorProfile or(int count) {
		return new OperatorProfile(0, 0, 0, 0, count, 0, 0);
	}

	public static OperatorProfile characterClass(int count) {
		return new OperatorProfile(0, 0, 0, 0, 0, count, 0);
	}

	public static OperatorProfile group() {
		return new OperatorProfile(0, 0, 0, 0, 0, 0, 1);
	}

	public String toString() {
		return "Star (*) = " + star + "\n"
				+ "Plus (+) = " + plus + "\n"
				+ "Dot (.) = " + dot + "\n"
				+ "Question (?) = " + question + "\n"
				+ "Or (|) = " + or + "\n"
				+ "Character Classes ([]) = " + characterClass + "\n"
				+ "Groups ( () ) = " + group + "\n";
	}
}
