package de.rwth.i9.cimt.ke.lib.model;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Keyword implements Comparable<Keyword> {
	private String token;
	private double weight;

	public Keyword(String keyword, double score) {
		this.token = keyword;
		this.weight = score;
	}

	@JsonProperty("text")
	public String getToken() {
		return token;
	}

	@JsonProperty("weight")
	public double getWeight() {
		return weight;
	}

	public static Comparator<Keyword> KeywordComparatorAsc = new Comparator<Keyword>() {
		@Override
		public int compare(Keyword keyword1, Keyword keyword2) {
			return keyword1.compareTo(keyword2);
		}

	};
	public static Comparator<Keyword> KeywordComparatorDesc = new Comparator<Keyword>() {
		@Override
		public int compare(Keyword keyword1, Keyword keyword2) {
			return keyword2.compareTo(keyword1);
		}

	};

	@Override
	public int compareTo(Keyword o) {
		if (this.weight < o.getWeight())
			return -1;
		else if (this.weight > o.getWeight())
			return 1;
		else
			return this.token.compareTo(o.getToken());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Keyword) {
			Keyword that = (Keyword) obj;
			if ((this.weight == that.getWeight()) && this.getToken().equals(that.getToken()))
				return true;
		}
		return false;
	}
}
