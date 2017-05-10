package de.rwth.i9.cimt.ke.lib.algorithm.kpextraction.unsupervised.topicclustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.ke.lib.model.Keyword;

public class CommunityCluster {
	private static Logger log = LoggerFactory.getLogger(CommunityCluster.class);

	public static List<Keyword> performCommunityClusterKE(String textContent) {
		List<Keyword> returnedKeywords = new ArrayList<Keyword>();
		// 1. candidate generation(N-Grams)
		// 2. WSD (Wikipedia based wsd)
		// 3. Build Semantic graph
		// 4. Discovering community structure of the semantic graph
		// 5. Selecting valuable communities
		Set<String> Ngrams = generateNgramsCandidate(textContent, 0, 5);

		return returnedKeywords;

	}

	private static Set<String> generateNgramsCandidate(String text, int minNgram, int maxNgram) {
		Set<String> Ngrams = new HashSet<String>();
		String[] words = text.split(" ");
		for (int i = minNgram; i <= maxNgram; i++) {
			Ngrams.addAll(ngrams(i, words));
		}
		return Ngrams;
	}

	public static List<String> ngrams(int n, String[] words) {
		List<String> ngrams = new ArrayList<String>();
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i + n));
		return ngrams;
	}

	public static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
	}
}
