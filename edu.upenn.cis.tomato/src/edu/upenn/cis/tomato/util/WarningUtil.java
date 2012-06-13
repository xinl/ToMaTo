package edu.upenn.cis.tomato.util;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.upenn.cis.tomato.application.ToMaTo;

public class WarningUtil {

	public static final String WARNING_PROMPT = "[ToMaTo Policy Violation Warning]";

	public static void printNewLine() {
		System.out.println("");
	}

	public static void printFunctionInvocationWarning(String library,
			String function, String position) {
		System.out.println(WARNING_PROMPT + " Library [" + library
				+ "] invokes function [" + function + "] at " + position);
	}

	public static void printAliasAnalysisWarning(
			TreeMap<String, String> QueryVariableSet,
			TreeSet<String> AnswerVariableSet, boolean IsFilterNull,
			boolean IsFilterSystem) {
		printNewLine();
		TreeMap<String, TreeSet<String>> result = new TreeMap<String, TreeSet<String>>();

		Iterator<String> iter_avs = AnswerVariableSet.iterator();
		while (iter_avs.hasNext()) {
			String[] answer = iter_avs.next().split(" ");

			if (answer.length != 5) {
				ErrorUtil
						.printErrorMessage("Unexpected answer format from alias analysis.");
				continue;
			}

			if (IsFilterNull && answer[4].equalsIgnoreCase("null")) {
				continue;
			}

			if (IsFilterSystem
					&& (answer[2].startsWith(ToMaTo.systemPrologueName)
							|| answer[2].startsWith(ToMaTo.systemPreambleName) || answer[2]
								.endsWith(".js"))) {
				continue;
			}

			String queryKey = answer[0] + " " + answer[1];
			String queryVariableName = QueryVariableSet.get(queryKey);
			String[] queryFunctionNameArray = answer[0].split("/");
			String[] answerFunctionNameArray = answer[2].split("/");

			String resultKey = "Variable [" + queryVariableName
					+ "] @ Function ["
					+ queryFunctionNameArray[queryFunctionNameArray.length - 1]
					+ "]";
			String resultObject = "Variable ["
					+ answer[4]
					+ "] @ Function ["
					+ answerFunctionNameArray[answerFunctionNameArray.length - 1]
					+ "]";

			TreeSet<String> resultOjectset = result.get(resultKey);
			if (resultOjectset == null) {
				resultOjectset = new TreeSet<String>();
				result.put(resultKey, resultOjectset);
			}

			resultOjectset.add(resultObject);
		}

		Iterator<Entry<String, TreeSet<String>>> iter_r = result.entrySet()
				.iterator();
		while (iter_r.hasNext()) {
			Entry<String, TreeSet<String>> resultEntry = iter_r.next();
			Iterator<String> iter_re = resultEntry.getValue().iterator();
			System.out.println(WARNING_PROMPT + " " + resultEntry.getKey()
					+ " has alias: ");
			while (iter_re.hasNext()) {
				System.out.println(iter_re.next() + "; ");
			}
			printNewLine();
		}
	}
}
