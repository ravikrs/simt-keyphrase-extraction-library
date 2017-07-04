package de.rwth.i9.simt.ke.lib.algorithm.kpextraction.unsupervised.topicclustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.simt.ke.lib.model.Keyword;
import de.rwth.i9.simt.nlp.NLP;
import de.rwth.i9.simt.nlp.util.StopWordsEn;
import smile.clustering.SpectralClustering;

public class KeyCluster {
	private static Logger log = LoggerFactory.getLogger(KeyCluster.class);
	private static final int CO_OCCURRENCE_WINDOW = 5;

	public static List<Keyword> performKeyClusterKE(String textContent, final NLP nlp) {
		List<Keyword> returnedKeywords = new ArrayList<Keyword>();
		// 1. Candidate term selection - filter out stop words and select all
		// single terms as candidates.
		// 2. Calculate term relatedness - eg co-occurrence,wikipedia based term
		// relatedness pmi
		// 3. Term Clustering - group terms similar as clusters. - hierarchical,
		// spectral, affinity propagation
		// 4. use exemplar terms to get keyphrases from the document - pos tags
		// noun groups with zero or more adjectives followed by one or more
		// nouns.

		List<String> candidateTokens = new ArrayList<>();
		Map<String, Integer> candidateTokenIndexMap = new HashMap<>();
		Map<Integer, String> idTokenMap = new HashMap<>();
		int iter = 0;

		// stores sentences in sequence. <1, This is first sentence>, <2, This is second sentence>....
		List<String> sentencesList = Arrays.asList(nlp.detectSentences(textContent));
		Map<Integer, String> sentenceListMap = IntStream.range(0, sentencesList.size()).boxed()
				.collect(Collectors.toMap(Function.identity(), i -> sentencesList.get(i)));
		// stores sentences in sequence with tokens of each sentence. <1, [This, is, first, sentence]>, <2, [This, is, second, sentence]>....
		Map<Integer, List<String>> sentenceIndexTokenListMap = sentenceListMap.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> (List<String>) Arrays.asList(nlp.tokenize(e.getValue()))
						.stream().map(token -> token.trim().toLowerCase()).collect(Collectors.toList())));

		// Step 2 Clustering based on Co-occurrence Term relatedness
		for (Map.Entry<Integer, String> entry : sentenceListMap.entrySet()) {
			Integer index = entry.getKey();
			String[] tokens = sentenceIndexTokenListMap.get(index).toArray(new String[0]);
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim().toLowerCase();
				if (!StopWordsEn.isStopWord(token) && !candidateTokens.contains(token)) {
					candidateTokens.add(token);
					candidateTokenIndexMap.put(token, iter);
					idTokenMap.put(iter, token);
					iter++;
				}
			}
		}
		double[][] data = new double[candidateTokens.size()][candidateTokens.size()];
		for (Map.Entry<Integer, String> entry : sentenceListMap.entrySet()) {
			Integer index = entry.getKey();
			String[] tokens = sentenceIndexTokenListMap.get(index).toArray(new String[0]);
			for (int i = 0; i < tokens.length; i++) {
				String firstToken = tokens[i].trim().toLowerCase();
				if (candidateTokens.contains(firstToken)) {
					int toIndex = i + CO_OCCURRENCE_WINDOW;
					toIndex = toIndex < tokens.length ? toIndex : tokens.length - 1;
					for (int j = i + 1; j <= toIndex; j++) {
						String secondToken = tokens[j].trim().toLowerCase();
						if (candidateTokens.contains(secondToken)) {
							int firstIndex = candidateTokenIndexMap.get(firstToken);
							int secondIndex = candidateTokenIndexMap.get(secondToken);
							data[firstIndex][secondIndex] += 1;
							data[secondIndex][firstIndex] += 1;
						}
					}
				}
			}
		}

		double sigma = 36.0;
		int k = (2 * candidateTokens.size()) / 3;

		SpectralClustering sc = new SpectralClustering(data, k, sigma);
		Map<Integer, List<String>> clusterIdTokensMap = new HashMap<>();
		Set<String> clusteredTokens = new HashSet<>();
		int[] clusters = sc.getClusterLabel();
		iter = 0;
		for (int c : clusters) {
			if (clusterIdTokensMap.containsKey(c)) {
				List<String> clusterdTokenList = clusterIdTokensMap.get(c);
				clusterdTokenList.add(idTokenMap.get(iter));
				clusterIdTokensMap.put(c, clusterdTokenList);
			} else {
				List<String> clusterdTokenList = new ArrayList<>();
				clusterdTokenList.add(idTokenMap.get(iter));
				clusterIdTokensMap.put(c, clusterdTokenList);
				clusteredTokens.add(idTokenMap.get(iter));
			}
			iter++;
		}
		//		for (Entry<Integer, List<String>> entry : clusterIdTokensMap.entrySet()) {
		//			System.out.println(entry.getKey() + " -> " + entry.getValue());
		//		}
		Map<String, List<Integer>> tokenOffsetPostionMap = getNounAdjSeqToken(textContent, nlp);
		List<String> nounAdjTokens = new ArrayList<>(tokenOffsetPostionMap.keySet());
		for (String nounAdjToken : nounAdjTokens) {
			String[] splittokens = nounAdjToken.split("\\s+");
			double score = 0.0;
			for (String splitToken : splittokens) {
				if (clusteredTokens.contains(splitToken)) {
					score += 1.0;
				}

			}
			if (score > 0) {
				score = (score * splittokens.length) / clusteredTokens.size();
				returnedKeywords.add(new Keyword(nounAdjToken, score));
			}

		}

		return returnedKeywords;

	}

	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
		return map.entrySet().stream().filter(entry -> Objects.equals(entry.getValue(), value)).map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	private static Map<String, List<Integer>> getNounAdjSeqToken(String textContent, NLP nlp) {
		Map<String, List<Integer>> tokenOffsetPostionMap = new HashMap<String, List<Integer>>();
		String token = "";
		int index = -1, phraseCount = 0, offsetPosition = -1;
		boolean nounPhraseEncountered = false;
		for (String sentence : nlp.detectSentences(textContent)) {
			String[] tokens = nlp.tokenize(sentence);
			String[] posTags = nlp.tagPartOfSpeech(tokens);
			index = -1;
			phraseCount = 0;
			for (String posTag : posTags) {
				index++;
				offsetPosition++;
				if (posTag.startsWith("JJ") && !nounPhraseEncountered) {
					token += tokens[index] + " ";
					phraseCount++;
					continue;
				} else if (posTag.startsWith("NN")) {
					token += tokens[index] + " ";
					phraseCount++;
					nounPhraseEncountered = true;
					continue;
				} else if (phraseCount >= 1) {
					token = token.toLowerCase().trim();
					if (tokenOffsetPostionMap.containsKey(token)) {
						tokenOffsetPostionMap.get(token).add(offsetPosition);
					} else {
						List<Integer> offsetPositionList = new ArrayList<Integer>();
						offsetPositionList.add(offsetPosition);
						tokenOffsetPostionMap.put(token, offsetPositionList);
					}
					phraseCount = 0;
					token = "";
				}
			}

		}
		return tokenOffsetPostionMap;
	}

}
