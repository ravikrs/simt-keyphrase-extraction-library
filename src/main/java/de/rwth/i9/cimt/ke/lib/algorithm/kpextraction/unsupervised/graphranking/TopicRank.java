package de.rwth.i9.cimt.ke.lib.algorithm.kpextraction.unsupervised.graphranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;

import de.rwth.i9.cimt.ke.lib.algorithm.clustering.hac.HACClusteringAlgorithm;
import de.rwth.i9.cimt.ke.lib.model.Keyword;
import de.rwth.i9.cimt.nlp.NLP;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;

public class TopicRank {
	private static final Logger log = LoggerFactory.getLogger(TopicRank.class);
	private static final double STEM_OVERLAP_THRESHOLD = 0.25;
	private static SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);

	/**
	 * performs TopicRank Keyphrase Algorithm for the input textContent.
	 * 
	 * @param textContent
	 *            input text
	 * @param nlpImpl
	 *            nlp implementation used for sentence detection
	 */
	public static List<Keyword> performTopicRankKE(String textContent, final NLP nlpImpl) {
		SimpleWeightedGraph<Cluster, DefaultWeightedEdge> topicRankGraph = new SimpleWeightedGraph<>(
				DefaultWeightedEdge.class);
		List<Cluster> tokenVertices = new ArrayList<>();
		Map<Cluster, String> clusterStringMap = new HashMap<>();
		List<Keyword> returnedKeyphrases = new ArrayList<>();

		Map<String, List<Integer>> tokenOffsetPostionMap = getNounAdjSeqToken(textContent, nlpImpl);
		List<String> nounAdjTokens = new ArrayList<>(tokenOffsetPostionMap.keySet());

		List<Cluster> clusters = performClustering(nounAdjTokens);
		for (Cluster cluster : clusters) {
			tokenVertices.add(cluster);
			topicRankGraph.addVertex(cluster);
			clusterStringMap.put(cluster, getClusterName(cluster));
		}

		for (int i = 0; i < clusters.size(); i++) {
			for (int j = i + 1; j < clusters.size(); j++) {
				DefaultWeightedEdge we = topicRankGraph.addEdge(clusters.get(i), clusters.get(j));
				double edgeWeight = calculateEdgeWeight(clusters.get(i), clusters.get(j), tokenOffsetPostionMap);
				topicRankGraph.setEdgeWeight(we, edgeWeight);
			}
		}

		PageRank<Cluster, DefaultWeightedEdge> pr = new PageRank<>(topicRankGraph);
		Map<Cluster, Double> score = pr.getScores();
		for (Entry<Cluster, Double> entry : score.entrySet()) {
			String[] candidates = clusterStringMap.get(entry.getKey()).split(",");
			String firstOccurredCandidate = "";
			int minOffset = Integer.MAX_VALUE;
			for (String candidate : candidates) {
				List<Integer> offsetPos = tokenOffsetPostionMap.get(candidate);
				Collections.sort(offsetPos);
				if (minOffset > offsetPos.get(0)) {
					firstOccurredCandidate = candidate;
					minOffset = offsetPos.get(0);
				}
			}
			returnedKeyphrases.add(new Keyword(firstOccurredCandidate, entry.getValue()));
		}
		Collections.sort(returnedKeyphrases, Keyword.KeywordComparatorDesc);

		return returnedKeyphrases;

	}

	private static double calculateEdgeWeight(Cluster node1, Cluster node2,
			Map<String, List<Integer>> tokenOffsetPostionMap) {
		String[] candidates1 = getClusterName(node1).split(",");
		String[] candidates2 = getClusterName(node2).split(",");
		double edgeWeight = 0.0, dist = 0.0;
		for (String candidate1 : candidates1) {
			for (String candidate2 : candidates2) {
				if (tokenOffsetPostionMap.containsKey(candidate1) && tokenOffsetPostionMap.containsKey(candidate2)) {
					List<Integer> offsetPosition1 = tokenOffsetPostionMap.get(candidate1);
					List<Integer> offsetPosition2 = tokenOffsetPostionMap.get(candidate2);
					for (int offsetPositionValue1 : offsetPosition1) {
						for (int offsetPositionValue2 : offsetPosition2) {
							dist += 1.0 / (Math.abs(offsetPositionValue1 - offsetPositionValue2));
						}
					}
					edgeWeight += dist;
				}
			}
		}
		return edgeWeight;

	}

	private static Map<String, List<Integer>> getNounAdjSeqToken(String textContent, NLP nlp) {
		Map<String, List<Integer>> tokenOffsetPostionMap = new HashMap<String, List<Integer>>();
		String token = "";
		int index = -1, phraseCount = 0, offsetPosition = -1;
		for (String sentence : nlp.detectSentences(textContent)) {
			String[] tokens = nlp.tokenize(sentence);
			String[] posTags = nlp.tagPartOfSpeech(tokens);
			index = -1;
			phraseCount = 0;
			for (String posTag : posTags) {
				index++;
				offsetPosition++;
				if (posTag.startsWith("NN") || posTag.startsWith("JJ")) {
					token += tokens[index] + " ";
					phraseCount++;
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

	private static double stemOverlapScore(String token1, String token2) {
		int overlapCount1 = 0;
		int overlapCount2 = 0;

		String[] subTokens1 = token1.split("\\s+");
		String[] subTokens2 = token2.split("\\s+");
		for (String subToken1 : subTokens1) {
			for (String subToken2 : subTokens2) {
				if (stemmer.stem(subToken1).equals(stemmer.stem(subToken2))) {
					overlapCount1++;
				}

			}
		}
		for (String subToken2 : subTokens2) {
			for (String subToken1 : subTokens1) {
				if (stemmer.stem(subToken1).equals(stemmer.stem(subToken2))) {
					overlapCount2++;
				}

			}
		}
		double overlapScore1 = (double) overlapCount1 / subTokens1.length;
		double overlapScore2 = (double) overlapCount2 / subTokens2.length;
		if (overlapCount1 > 0 && overlapScore1 >= STEM_OVERLAP_THRESHOLD && overlapScore2 >= STEM_OVERLAP_THRESHOLD) {
			return 1 - ((overlapScore1 + overlapScore2) / 2);
		} else {
			return Double.MAX_VALUE;
		}
	}

	private static List<Cluster> performClustering(List<String> nounAdjTokens) {

		HACClusteringAlgorithm alg = new HACClusteringAlgorithm();
		int tokensLength = nounAdjTokens.size();
		double distances[][] = new double[tokensLength][tokensLength];

		for (int i = 0; i < tokensLength; i++) {
			for (int j = i + 1; j < tokensLength; j++) {
				double overlapScore = stemOverlapScore(nounAdjTokens.get(i), nounAdjTokens.get(j));
				distances[i][j] = distances[j][i] = overlapScore;
			}
		}

		String[] clusterNames = nounAdjTokens.toArray(new String[nounAdjTokens.size()]);

		List<Cluster> clusters = alg.performFlatClustering(distances, clusterNames, new AverageLinkageStrategy(), 0.75);
		return clusters;
	}

	private static String getClusterName(Cluster node) {
		StringBuilder sb = new StringBuilder();
		if (node.isLeaf()) {
			return node.getName().trim();
		}
		for (Cluster child : node.getChildren()) {
			sb.append(getClusterName(child)).append(",");
		}
		if (sb.toString().endsWith(","))
			return sb.toString().substring(0, sb.length() - 1).trim();
		else
			return sb.toString().trim();
	}

}
