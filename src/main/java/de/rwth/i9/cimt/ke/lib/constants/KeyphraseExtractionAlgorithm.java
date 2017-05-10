package de.rwth.i9.cimt.ke.lib.constants;

public enum KeyphraseExtractionAlgorithm {
	JATE_TTF, JATE_ATTF, JATE_TFIDF, JATE_RIDF, JATE_CVALUE, JATE_CHISQUARE, JATE_RAKE, JATE_WEIRDNESS, JATE_GLOSSEX, JATE_TERMEX, RAKE, TEXT_RANK, TEXT_RANK_WORDNET, TOPIC_RANK, TOPICAL_PAGE_RANK, KEY_CLUSTER, DEFAULT;

	public static KeyphraseExtractionAlgorithm fromString(String value) {
		if ("JATE_TTF".equalsIgnoreCase(value))
			return JATE_TTF;

		if ("JATE_ATTF".equalsIgnoreCase(value))
			return JATE_ATTF;

		if ("JATE_TFIDF".equalsIgnoreCase(value))
			return JATE_TFIDF;

		if ("JATE_RIDF".equalsIgnoreCase(value))
			return JATE_RIDF;

		if ("JATE_CVALUE".equalsIgnoreCase(value))
			return JATE_CVALUE;

		if ("JATE_CHISQUARE".equalsIgnoreCase(value))
			return JATE_CHISQUARE;

		if ("JATE_RAKE".equalsIgnoreCase(value))
			return JATE_RAKE;

		if ("JATE_WEIRDNESS".equalsIgnoreCase(value))
			return JATE_WEIRDNESS;

		if ("JATE_GLOSSEX".equalsIgnoreCase(value))
			return JATE_GLOSSEX;

		if ("JATE_TERMEX".equalsIgnoreCase(value))
			return JATE_TERMEX;

		if ("RAKE".equalsIgnoreCase(value))
			return RAKE;

		if ("TEXT_RANK".equalsIgnoreCase(value))
			return TEXT_RANK;

		if ("TEXT_RANK_WORDNET".equalsIgnoreCase(value))
			return TEXT_RANK_WORDNET;

		if ("TOPIC_RANK".equalsIgnoreCase(value))
			return TOPIC_RANK;

		if ("TOPICAL_PAGE_RANK".equalsIgnoreCase(value))
			return TOPICAL_PAGE_RANK;

		if ("KEY_CLUSTER".equalsIgnoreCase(value))
			return KEY_CLUSTER;

		return DEFAULT;
	}

}
