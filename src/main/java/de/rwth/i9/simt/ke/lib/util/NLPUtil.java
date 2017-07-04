package de.rwth.i9.simt.ke.lib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.nlp.NLP;
import de.rwth.i9.cimt.nlp.util.StopWordsEn;

public class NLPUtil {

	private static Logger log = LoggerFactory.getLogger(NLPUtil.class);

	public static Set<String> generateNgramsCandidate(String text, int minNgram, int maxNgram) {
		Set<String> Ngrams = new HashSet<>();

		if (minNgram <= 0) {
			minNgram = 1;
		}
		if (minNgram > maxNgram || text.isEmpty()) {
			return Ngrams;
		}
		String[] words = text.split("\\s+");
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

	public static Map<String, List<Integer>> getNounAdjSeqToken(String textContent, NLP nlp) {
		Map<String, List<Integer>> tokenOffsetPostionMap = new HashMap<>();
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

	public static Map<String, List<Integer>> getNounSeqToken(String textContent, NLP nlp) {
		Map<String, List<Integer>> tokenOffsetPostionMap = new HashMap<>();
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
				if (posTag.startsWith("NN")) {
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

	public static Map<String, Integer> splitTextByStopWords(String textContent, NLP nlp) {
		Map<String, Integer> tokenCandidatesFreq = new HashMap<>();
		String tokenCandidate = "";
		boolean tokenAdded = false;
		List<String> candidateSubTokens = new ArrayList<>();
		for (String sentence : nlp.detectSentences(textContent)) {
			String[] tokens = nlp.tokenize(sentence);
			for (int iter = 0; iter < tokens.length; iter++) {
				if (!StopWordsEn.isStopWord(tokens[iter])) {
					candidateSubTokens.add(tokens[iter].toLowerCase().trim());
					tokenAdded = false;
				} else {
					tokenCandidate = String.join(" ", candidateSubTokens);
					if (!tokenCandidate.isEmpty()) {
						if (!tokenCandidatesFreq.containsKey(tokenCandidate)) {
							tokenCandidatesFreq.put(tokenCandidate, 1);
						} else {
							tokenCandidatesFreq.put(tokenCandidate, tokenCandidatesFreq.get(tokenCandidate) + 1);
						}
					}
					tokenAdded = true;
					candidateSubTokens = new ArrayList<>();
				}

			}
		}
		//add last candidate token
		if (!tokenAdded) {
			tokenCandidate = String.join(" ", candidateSubTokens);
			if (!tokenCandidate.isEmpty()) {
				if (!tokenCandidatesFreq.containsKey(tokenCandidate)) {
					tokenCandidatesFreq.put(tokenCandidate, 1);
				} else {
					tokenCandidatesFreq.put(tokenCandidate, tokenCandidatesFreq.get(tokenCandidate) + 1);
				}
			}
		}

		return tokenCandidatesFreq;
	}

}
