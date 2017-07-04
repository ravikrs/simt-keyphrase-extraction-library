package de.rwth.i9.simt.ke.lib.algorithm.kpextraction.unsupervised.graphranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.nlp.NLP;
import de.rwth.i9.simt.ke.lib.model.Keyword;

public class TextRank {
	private static Logger log = LoggerFactory.getLogger(TextRank.class);
	private static final int CO_OCCURRENCE_WINDOW = 5;

	/**
	 * performs TextRank Keypharse Algorithm for the input textContent.
	 * 
	 * @param textContent
	 *            input text
	 * @param nlpImpl
	 *            nlp implementation used for sentence detection
	 */
	public static List<Keyword> performTextRankKE(String textContent, final NLP nlpImpl) {
		Graph<String, DefaultEdge> textRankGraph = new SimpleGraph<>(DefaultEdge.class);
		List<String> tokenVertices = new ArrayList<>();
		List<Keyword> returnedKeyphrases = new ArrayList<>();
		List<String> sentencesList = Arrays.asList(nlpImpl.detectSentences(textContent));

		// stores sentences in sequence. <1, This is first sentence>, <2, This is second sentence>....
		Map<Integer, String> sentenceListMap = IntStream.range(0, sentencesList.size()).boxed()
				.collect(Collectors.toMap(Function.identity(), i -> sentencesList.get(i)));
		// stores sentences in sequence with tokens of each sentence. <1, [This, is, first, sentence]>, <2, [This, is, second, sentence]>....
		Map<Integer, List<String>> sentenceIndexTokenListMap = sentenceListMap.entrySet().stream().collect(
				Collectors.toMap(e -> e.getKey(), e -> (List<String>) Arrays.asList(nlpImpl.tokenize(e.getValue()))
						.stream().map(token -> token.trim().toLowerCase()).collect(Collectors.toList())));

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

		PageRank<String, DefaultEdge> pr = new PageRank<String, DefaultEdge>(textRankGraph);
		Map<String, Double> prScoreMap = pr.getScores();
		List<String> returnedKeywords = prScoreMap.entrySet().stream().map(x -> x.getKey())
				.collect(Collectors.toList());

		Set<String> nonRetainedkeywords = new HashSet<String>();
		Set<String> tokenAdded = new HashSet<>();
		for (String sentence : sentencesList) {
			String keyphrase = "";
			double keyphraseScore = 0.0;
			int phraseCount = 0;
			for (String token : nlpImpl.tokenize(sentence)) {
				String trimmedtoken = token.trim().toLowerCase();
				if (returnedKeywords.contains(trimmedtoken)) {
					keyphrase += trimmedtoken + " ";
					keyphraseScore += prScoreMap.get(trimmedtoken).doubleValue();
					phraseCount++;
					continue;
				} else if (phraseCount > 1) {
					if (tokenAdded.contains(keyphrase.trim())) {
						continue;
					}
					tokenAdded.add(keyphrase.trim());
					returnedKeyphrases.add(new Keyword(keyphrase.trim(), keyphraseScore));
					nonRetainedkeywords.addAll(Arrays.asList(keyphrase.trim().split("\\s+")));
				}
				keyphrase = "";
				keyphraseScore = 0.0;
				phraseCount = 0;
			}
		}
		returnedKeywords.removeAll(nonRetainedkeywords);
		for (String keywordString : returnedKeywords) {
			returnedKeyphrases.add(new Keyword(keywordString, prScoreMap.get(keywordString).doubleValue()));
		}
		Collections.sort(returnedKeyphrases, Keyword.KeywordComparatorDesc);
		return returnedKeyphrases;

	}

	private static boolean isGoodPos(String pos) {
		if (pos.startsWith("NN") || pos.startsWith("JJ"))
			return true;
		return false;
	}
}
