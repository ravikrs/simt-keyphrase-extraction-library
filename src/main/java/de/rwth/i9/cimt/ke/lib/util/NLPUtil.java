package de.rwth.i9.cimt.ke.lib.util;

import java.util.HashSet;
import java.util.Set;

public class NLPUtil {
	public static Set<String> generateNgramsCandidate(String text, int minNgram, int maxNgram) {
		Set<String> Ngrams = new HashSet<String>();
		String[] words = text.split(" ");
		for (int i = minNgram; i <= maxNgram; i++) {
			Ngrams.addAll(ngrams(i, words));
		}
		return Ngrams;
	}

	private static Set<String> ngrams(int n, String[] words) {
		Set<String> ngrams = new HashSet<String>();
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i + n));
		return ngrams;
	}

	private static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
	}
}
