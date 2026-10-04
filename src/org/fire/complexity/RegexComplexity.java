package org.fire.complexity;

import org.fire.complexity.opusage.OperatorUsage;
import org.fire.complexity.opusage.RegularExpression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Operator occurrence and Ehrenfeucht-Zeiger complexity for regular expressions.
 *
 * <p>One expression per line, from the files named on the command line or from
 * standard input. {@code -e} passes a single expression. {@code -v} prints
 * each expression as well as the averages.
 */
public class RegexComplexity {
	public static void main(String[] args) {
		boolean verbose = false;
		ArrayList<Input> inputs = new ArrayList<Input>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-h".equals(arg) || "--help".equals(arg)) {
				help();
				return;
			}
			if ("-v".equals(arg) || "--verbose".equals(arg)) {
				verbose = true;
				continue;
			}
			if ("-e".equals(arg) || "--expression".equals(arg)) {
				if (i + 1 >= args.length) {
					System.err.println("Missing expression after " + arg);
					System.exit(1);
				}
				inputs.add(Input.expression(args[++i]));
				continue;
			}
			if ("--".equals(arg)) {
				for (int j = i + 1; j < args.length; j++) {
					inputs.add(Input.file(args[j]));
				}
				break;
			}
			if (arg.startsWith("-")) {
				System.err.println("Unknown option: " + arg);
				help();
				System.exit(1);
			}
			inputs.add(Input.file(arg));
		}

		ArrayList<RegularExpression> accepted = new ArrayList<RegularExpression>();
		int errors = 0;
		try {
			if (inputs.isEmpty()) {
				errors += read(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
						"stdin", accepted);
			} else {
				for (Input input : inputs) {
					if (input.expression != null) {
						if (!take("expression", 1, input.expression, accepted)) {
							errors++;
						}
					} else {
						BufferedReader reader = Files.newBufferedReader(Paths.get(input.name), StandardCharsets.UTF_8);
						try {
							errors += read(reader, input.name, accepted);
						} finally {
							reader.close();
						}
					}
				}
			}
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}

		if (accepted.isEmpty()) {
			System.out.println("No expressions.");
		} else if (accepted.size() == 1) {
			System.out.print(accepted.get(0).report());
		} else {
			OperatorUsage operators = new OperatorUsage();
			ComplexityUsage complexity = new ComplexityUsage();
			for (RegularExpression expression : accepted) {
				if (verbose) {
					System.out.print(expression.report());
					System.out.println();
				}
				operators.add(expression);
				complexity.add(expression);
			}
			System.out.print(operators);
			System.out.print(complexity);
		}
		if (errors > 0) {
			System.exit(1);
		}
	}

	private static int read(BufferedReader reader, String origin, ArrayList<RegularExpression> accepted)
			throws IOException {
		int errors = 0;
		int lineNo = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			lineNo++;
			if (lineNo == 1 && line.startsWith("\uFEFF")) {
				line = line.substring(1);
			}
			if (line.isEmpty()) {
				continue;
			}
			if (!take(origin, lineNo, line, accepted)) {
				errors++;
			}
		}
		return errors;
	}

	private static boolean take(String origin, int lineNo, String expression, ArrayList<RegularExpression> accepted) {
		try {
			accepted.add(new RegularExpression(expression));
			return true;
		} catch (RegexSyntaxException ex) {
			System.err.println(origin + ":" + lineNo + ": " + ex.getMessage());
			return false;
		}
	}

	private static void help() {
		System.out.println("Usage: RegexComplexity [-v] [-e expression] [file ...]");
		System.out.println("Reads one regular expression per line and reports operator occurrence");
		System.out.println("and Ehrenfeucht-Zeiger complexity: size (N), star height (H),");
		System.out.println("length (L) and width (W).");
		System.out.println("Complements are taken over ASCII (U+0000-U+007F).");
		System.out.println("  -e expression   measure this expression");
		System.out.println("  -v              print every expression as well as the averages");
		System.out.println("  -h              this help");
	}

	private static final class Input {
		final String name;
		final String expression;

		private Input(String name, String expression) {
			this.name = name;
			this.expression = expression;
		}

		static Input file(String name) {
			return new Input(name, null);
		}

		static Input expression(String expression) {
			return new Input("expression", expression);
		}
	}
}
