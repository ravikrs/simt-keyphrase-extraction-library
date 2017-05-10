package de.rwth.i9.cimt.ke.lib.algorithm.kpextraction.unsupervised.topicclustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.ke.lib.model.Keyword;
import de.rwth.i9.cimt.nlp.NLP;

public class TopicalPageRank {
	private static Logger log = LoggerFactory.getLogger(TopicalPageRank.class);
	private static final int CO_OCCURRENCE_WINDOW = 5;
	private static final int MAX_ITERATIONS_DEFAULT = 100;
	private static final double TOLERANCE_DEFAULT = 0.001;
	private static final double DAMPING_FACTOR_DEFAULT = 0.85d;

	public static List<Keyword> performTopicalPageRankKE(String textContent, NLP nlpImpl, String cimtHome) {

		Graph<String, DefaultEdge> textRankGraph = new SimpleGraph<>(DefaultEdge.class);
		List<String> tokenVertices = new ArrayList<>();
		List<Keyword> returnedKeyphrases = new ArrayList<>();
		List<Map<String, Double>> prScoreMapList = new ArrayList<>();

		List<String> sentencesList = Arrays.asList(nlpImpl.detectSentences(textContent));

		// stores sentences in sequence. <1, This is first sentence>, <2, This is second sentence>....
		Map<Integer, String> sentenceListMap = IntStream.range(0, sentencesList.size()).boxed()
				.collect(Collectors.toMap(Function.identity(), i -> sentencesList.get(i)));
		// stores sentences in sequence with tokens of each sentence. <1, [This, is, first, sentence]>, <2, [This, is, second, sentence]>....
		Map<Integer, List<String>> sentenceIndexTokenListMap = sentenceListMap.entrySet().stream().collect(
				Collectors.toMap(e -> e.getKey(), e -> (List<String>) Arrays.asList(nlpImpl.tokenize(e.getValue()))
						.stream().map(token -> token.trim().toLowerCase()).collect(Collectors.toList())));

		Map<String, List<Double>> tokensWithTopicIdProbMap = new HashMap<>();
		for (Map.Entry<Integer, String> entry : sentenceListMap.entrySet()) {
			Integer index = entry.getKey();
			String[] tokens = sentenceIndexTokenListMap.get(index).toArray(new String[0]);
			String[] posTags = nlpImpl.tagPartOfSpeech(tokens);
			for (int i = 0; i < tokens.length; i++) {
				if (isGoodPos(posTags[i])) {
					String token = tokens[i].trim().toLowerCase();
					if (!tokenVertices.contains(token)) {
						tokenVertices.add(token);
						textRankGraph.addVertex(token);
						tokensWithTopicIdProbMap.put(token, LDATopicModel.computeTopicProbability(cimtHome, token));
					}
				}
			}
		}

		for (Map.Entry<Integer, List<String>> entry : sentenceIndexTokenListMap.entrySet()) {
			List<String> tokenList = entry.getValue();
			int tokenIndex = 0;
			for (String token : tokenList) {
				tokenIndex++;
				token = token.trim().toLowerCase();
				if (tokenVertices.contains(token)) {
					int toIndex = tokenIndex + CO_OCCURRENCE_WINDOW;
					toIndex = toIndex > tokenList.size() ? tokenList.size() : toIndex;
					tokenIndex = tokenIndex > toIndex ? toIndex : tokenIndex;
					List<String> subtokenList = tokenList.subList(tokenIndex, toIndex);
					for (String secondToken : subtokenList) {
						secondToken = secondToken.trim().toLowerCase();
						if (tokenVertices.contains(secondToken)) {
							if (!token.equals(secondToken)) {
								textRankGraph.addEdge(token, secondToken);
							}
						}
					}
				}

			}
		}

		//build a map to be used by TPRPageRank
		List<Map<String, Double>> topicIdTokenProbMap = new ArrayList<>();

		for (int i = 0; i < LDATopicModel.numTopics; i++) {
			Map<String, Double> tokenProbMap = new HashMap<>();
			for (Entry<String, List<Double>> entry : tokensWithTopicIdProbMap.entrySet()) {
				tokenProbMap.put(entry.getKey(), entry.getValue().get(i));
			}
			topicIdTokenProbMap.add(tokenProbMap);
		}

		// compute 
		for (Integer i = 0; i < topicIdTokenProbMap.size(); i++) {
			TPRPageRank<String, DefaultEdge> pr = new TPRPageRank<String, DefaultEdge>(textRankGraph,
					DAMPING_FACTOR_DEFAULT, MAX_ITERATIONS_DEFAULT, TOLERANCE_DEFAULT, topicIdTokenProbMap.get(i));
			prScoreMapList.add(pr.getScores());
		}
		Map<String, List<Integer>> nounAdjSeq = getNounAdjSeqToken(textContent, nlpImpl);
		List<Map<String, Double>> nounAdjSeqScore = rankNounAdjSeqFromPRforEachTopic(prScoreMapList,
				nounAdjSeq.keySet());
		returnedKeyphrases = rankKeywords(textContent, cimtHome, nounAdjSeqScore, nounAdjSeq.keySet());
		Collections.sort(returnedKeyphrases, Keyword.KeywordComparatorDesc);
		return returnedKeyphrases;

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

	private static boolean isGoodPos(String pos) {
		if (pos.startsWith("NN") || pos.startsWith("JJ"))
			return true;
		return false;
	}

	private static List<Keyword> rankKeywords(String textContent, String cimtHome,
			List<Map<String, Double>> nounAdjSeqScores, Set<String> nounAdjSeq) {
		List<Keyword> keywordList = new ArrayList<>();
		List<Double> prob = LDATopicModel.computeTopicProbability(cimtHome, textContent);

		for (String nounAdjToken : nounAdjSeq) {
			double score = 0.0;
			for (int i = 0; i < nounAdjSeqScores.size(); i++) {
				Map<String, Double> nounAdjSeqScore = nounAdjSeqScores.get(i);
				if (nounAdjSeqScore.containsKey(nounAdjToken)) {
					score += (nounAdjSeqScore.get(nounAdjToken) * prob.get(i));
				}
			}
			keywordList.add(new Keyword(nounAdjToken, score));
		}
		return keywordList;
	}

	private static List<Map<String, Double>> rankNounAdjSeqFromPRforEachTopic(List<Map<String, Double>> prScoreMapList,
			Set<String> adjNounSeq) {
		List<Map<String, Double>> nounAdjSeqAggregatedScore = new ArrayList<>();

		for (Map<String, Double> prScore : prScoreMapList) {
			Map<String, Double> nounAdjSeqScorePerTopic = new HashMap<>();
			for (String adjNoun : adjNounSeq) {
				String[] tokens = adjNoun.split("\\s+");
				double score = 0;
				for (String token : tokens) {
					if (prScore.containsKey(token)) {
						score += prScore.get(token).doubleValue();
					}
				}
				nounAdjSeqScorePerTopic.put(adjNoun, score);
			}
			nounAdjSeqAggregatedScore.add(nounAdjSeqScorePerTopic);

		}

		return nounAdjSeqAggregatedScore;

	}
}
