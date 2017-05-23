package de.rwth.i9.cimt.ke.lib.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rwth.i9.cimt.ke.lib.adt.Pair;

/**
 * Provides an immutable pair of a String and an Double
 */
public class WordCount extends Pair<String, Double> {

	public WordCount(String token, Double score) {
		super(token, score);
	}

	public static List<WordCount> parseIntoList(String setStr) {
		// in form as e.g. [(Mark, 2), (communications, 4), (developments, 2), (vision,, 2)]

		List<WordCount> set = new ArrayList<>();
		//TODO not capturing any numbers, special signs like %
		String regex = "\\(([^,]+), ([^,]+)\\)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(setStr);
		while (m.find()) {
			String word = m.group(1);
			double score = Double.parseDouble(m.group(2));
			set.add(new WordCount(word, score));
		}
		return set;
	}

	public static String formatIntoString(List<WordCount> wordCounts) {
		if (wordCounts == null || wordCounts.isEmpty())
			return "";
		String res = "[";
		for (WordCount wc : wordCounts) {
			res += "(" + wc.getX() + ", " + wc.getY() + "),";
		}
		return res.substring(0, res.length() - 1) + "]";
	}

}
