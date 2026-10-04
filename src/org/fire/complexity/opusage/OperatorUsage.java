package org.fire.complexity.opusage;

import java.util.ArrayList;

public class OperatorUsage {
	private ArrayList<RegularExpression> regexList;
	
	public OperatorUsage() {
		regexList = new ArrayList<RegularExpression>();
	}
	
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
		StringBuilder strbld = new StringBuilder();
		int starCount = 0;
		int plusCount = 0;
		int dotCount = 0;
		int questionCount = 0;
		int orCount = 0;
		int charClassCount = 0;
		int groupCount = 0;
		
		for (RegularExpression re : regexList) {
			starCount += re.starCount;
			plusCount += re.plusCount;
			dotCount += re.dotCount;
			questionCount += re.questionCount;
			orCount += re.orCount;
			charClassCount += re.charClassCount;
			groupCount += re.groupCount;
		}
		
		strbld.append("Average Operator Occurrences:\n");
		strbld.append("Star (*) = ").append(((double)starCount / (double)regexList.size())).append("\n");
		strbld.append("Plus (+) = ").append(((double)plusCount / (double)regexList.size())).append("\n");
		strbld.append("Dot (.) = ").append(((double)dotCount / (double)regexList.size())).append("\n");
		strbld.append("Question (?) = ").append(((double)questionCount / (double)regexList.size())).append("\n");
		strbld.append("Or (|) = ").append(((double)orCount / (double)regexList.size())).append("\n");
		strbld.append("Character Classes ([]) = ").append(((double)charClassCount / (double)regexList.size())).append("\n");
		strbld.append("Groups ( () ) = ").append(((double)groupCount / (double)regexList.size())).append("\n");
		
		return strbld.toString();
	}
}
