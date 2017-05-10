package de.rwth.i9.cimt.ke.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Textbody {
	private String text;
	private String algorithmName;
	private String numKeywords;

	public String getNumKeywords() {
		return numKeywords;
	}

	public void setNumKeywords(String numKeywords) {
		this.numKeywords = numKeywords;
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	public void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
