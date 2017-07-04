package de.rwth.i9.simt.ke.lib.constants;

public enum SenseInventory {
	WORDNET, WIKIPEDIA, WIKTIONARY;
	public static SenseInventory fromString(String value) {
		if ("WORDNET".equalsIgnoreCase(value))
			return WORDNET;
		if ("WIKIPEDIA".equalsIgnoreCase(value))
			return WIKIPEDIA;
		if ("WIKTIONARY".equalsIgnoreCase(value))
			return WIKTIONARY;
		return WORDNET;
	}

}
