package org.fire.complexity;

import org.fire.complexity.opusage.RegularExpression;

import java.util.ArrayList;

/**
 * Average Ehrenfeucht-Zeiger complexity over a list of expressions.
 */
public class ComplexityUsage {
	private final ArrayList<RegularExpression> regexList = new ArrayList<RegularExpression>();

	public void add(String regex) {
		add(new RegularExpression(regex));
	}

	public void add(RegularExpression expression) {
		regexList.add(expression);
	}

	public int size() {
		return regexList.size();
	}

	public String toString() {
		if (regexList.isEmpty()) {
			return "No expressions.\n";
		}
		long size = 0;
		long starHeight = 0;
		long length = 0;
		long width = 0;
		for (RegularExpression expression : regexList) {
			ExpressionComplexity complexity = expression.getComplexity();
			size += complexity.size;
			starHeight += complexity.starHeight;
			length += complexity.length;
			width += complexity.width;
		}
		double n = regexList.size();
		StringBuilder report = new StringBuilder();
		report.append("Average complexity:\n");
		report.append("Size (N) = ").append(size / n).append("\n");
		report.append("Star height (H) = ").append(starHeight / n).append("\n");
		report.append("Length (L) = ").append(length / n).append("\n");
		report.append("Width (W) = ").append(width / n).append("\n");
		report.append("Complements are taken over ASCII (U+0000-U+007F).\n");
		return report.toString();
	}
}
