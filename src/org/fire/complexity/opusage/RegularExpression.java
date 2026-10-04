package org.fire.complexity.opusage;

import org.fire.complexity.ExpressionComplexity;
import org.fire.complexity.OperatorProfile;
import org.fire.complexity.syntax.RegexParser;

/**
 * One regular expression, with its operator counts and its complexity.
 */
public class RegularExpression {
	private final String regex;
	private ExpressionComplexity complexity = ExpressionComplexity.ZERO;

	int starCount;
	int plusCount;
	int dotCount;
	int questionCount;
	int orCount;
	int charClassCount;
	int groupCount;

	public RegularExpression(String regex) {
		this.regex = regex;
		measure();
	}

	private void measure() {
		RegexParser.Analysis analysis = RegexParser.parse(regex);
		complexity = analysis.complexity();
		OperatorProfile operators = analysis.operators();
		starCount = operators.star;
		plusCount = operators.plus;
		dotCount = operators.dot;
		questionCount = operators.question;
		orCount = operators.or;
		charClassCount = operators.characterClass;
		groupCount = operators.group;
	}

	public String getRegex() {
		return regex;
	}

	public ExpressionComplexity getComplexity() {
		return complexity;
	}

	public OperatorProfile getOperators() {
		return new OperatorProfile(starCount, plusCount, dotCount, questionCount, orCount, charClassCount, groupCount);
	}

	public String report() {
		return regex + "\n" + complexity + getOperators();
	}
}
