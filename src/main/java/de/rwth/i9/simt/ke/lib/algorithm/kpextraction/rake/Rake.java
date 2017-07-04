package de.rwth.i9.simt.ke.lib.algorithm.kpextraction.rake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.nlp.NLP;
import de.rwth.i9.simt.ke.lib.model.Keyword;
import edu.ehu.galan.rake.RakeAlgorithm;
import edu.ehu.galan.rake.model.Document;
import edu.ehu.galan.rake.model.Term;
import edu.ehu.galan.rake.model.Token;

public class Rake {
	private static final Logger log = LoggerFactory.getLogger(Rake.class);

	public static List<Keyword> extractKeyword(String text, NLP openNlpImpl) {
		List<LinkedList<Token>> tokenizedSentenceList = new ArrayList<LinkedList<Token>>();
		List<String> sentenceList = new ArrayList<String>();
		LinkedList<Token> tokenList = null;
		List<Keyword> keywords = new ArrayList<Keyword>();

		for (String sentence : openNlpImpl.detectSentences(text)) {
			sentenceList.add(sentence);
			String[] tokens = openNlpImpl.tokenize(sentence);
			String[] posTags = openNlpImpl.tagPartOfSpeech(tokens);
			tokenList = new LinkedList<Token>();
			for (int i = 0; i < tokens.length; i++) {
				tokenList.add(new Token(tokens[i], posTags[i]));
			}

		}

		Document doc = new Document("", "");
		doc.setSentenceList(sentenceList);
		doc.List(tokenizedSentenceList);
		RakeAlgorithm ex = new RakeAlgorithm();
		InputStream isStopwords = Rake.class.getClassLoader().getResourceAsStream("/stopLists/RakeStopWordsEN");
		InputStream isPunctuations = Rake.class.getClassLoader()
				.getResourceAsStream("/stopLists/RakePunctDefaultStopListEN");

		// ex.loadStopWordsList("src/main/resources/stopLists/SmartStopListEn");
		// ex.loadPunctStopWord("src/main/resources/stopLists/RakePunctDefaultStopList");

		ex.loadStopWordsList(getStringListFromInputStream(isStopwords));
		ex.loadPunctStopWord(getStringListFromInputStream(isPunctuations));
		ex.init(doc, "");
		ex.runAlgorithm();
		List<Term> terms = doc.getTermList();
		for (Term term : terms) {
			Keyword keyword = new Keyword(term.getTerm(), term.getScore());
			keywords.add(keyword);
		}
		log.info("RAKE Algorithm");
		return keywords;
	}

	private static List<String> getStringListFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		List<String> list = new ArrayList<>();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				list.add(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

}
