/*
Copyright (c) 2009, ShareThis, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

 * Neither the name of the ShareThis, Inc., nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.rwth.i9.simt.ke.lib.algorithm.kpextraction.textrank;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.cimt.nlp.NLP;
import de.rwth.i9.simt.ke.lib.model.Keyword;
import net.sf.extjwnl.data.POS;

/**
 * Java implementation of the TextRank algorithm by Rada Mihalcea, et al.
 * http://lit.csci.unt.edu/index.php/Graph-based_NLP
 *
 * @author paco@sharethis.com
 */

public class TextRankWordnet implements Callable<Collection<MetricVector>> {
	// logging

	private static final Logger logger = LoggerFactory.getLogger(TextRankWordnet.class);

	/**
	 * Public definitions.
	 */

	public static final String NLP_RESOURCES = "nlp.resources";
	public static final double MIN_NORMALIZED_RANK = 0.05D;
	public static final int MAX_NGRAM_LENGTH = 5;
	public static final long MAX_WORDNET_TEXT = 2000L;
	public static final long MAX_WORDNET_GRAPH = 600L;

	/**
	 * Protected members.
	 */

	protected String text = null;
	protected boolean use_wordnet = false;

	protected Graph graph = null;
	protected Graph ngram_subgraph = null;
	protected Map<NGram, MetricVector> metric_space = null;

	protected long start_time = 0L;
	protected long elapsed_time = 0L;
	NLP openNLPImpl = null;

	LanguageEnglish lang;
	String wordNetResPath;

	/**
	 * Constructor.
	 */

	public TextRankWordnet(final NLP openNLPImpl, final LanguageEnglish lang, final String wordNetResPath,
			final boolean use_wordnet) throws Exception {
		this.openNLPImpl = openNLPImpl;
		this.lang = lang;
		this.wordNetResPath = wordNetResPath;
		this.use_wordnet = use_wordnet;
	}

	/**
	 * Prepare to call algorithm with a new text to analyze.
	 */

	public void prepCall(final String text) throws Exception {
		graph = new Graph();
		ngram_subgraph = null;
		metric_space = new HashMap<NGram, MetricVector>();
		this.text = text;
		if (use_wordnet) {
			WordNet.buildDictionary(wordNetResPath);
		}
	}

	/**
	 * Run the TextRank algorithm on the given semi-structured text (e.g.,
	 * results of parsed HTML from crawled web content) to build a graph of
	 * weighted key phrases.
	 */
	@Override
	public Collection<MetricVector> call() throws Exception {
		//////////////////////////////////////////////////
		// PASS 1: construct a graph from PoS tags

		initTime();

		// scan sentences to construct a graph of relevent morphemes

		final ArrayList<Sentence> s_list = new ArrayList<Sentence>();

		for (String sent_text : openNLPImpl.detectSentences(text)) {
			final Sentence s = new Sentence(sent_text.trim());
			s.mapTokens(lang, graph);
			s_list.add(s);

			if (logger.isDebugEnabled()) {
				logger.debug("s: " + s.text);
				logger.debug(s.md5_hash);
			}
		}

		markTime("construct_graph");

		//////////////////////////////////////////////////
		// PASS 2: run TextRank to determine keywords

		initTime();

		final int max_results = (int) Math.round((double) graph.size() * Graph.KEYWORD_REDUCTION_FACTOR);

		graph.runTextRank();
		graph.sortResults(max_results);

		ngram_subgraph = NGram.collectNGrams(lang, s_list, graph.getRankThreshold());

		markTime("basic_textrank");

		if (logger.isInfoEnabled()) {
			logger.info("TEXT_BYTES:\t" + text.length());
			logger.info("GRAPH_SIZE:\t" + graph.size());
		}

		//////////////////////////////////////////////////
		// PASS 3: lemmatize selected keywords and phrases

		initTime();

		Graph synset_subgraph = new Graph();

		// filter for edge cases

		if (use_wordnet && (text.length() < MAX_WORDNET_TEXT) && (graph.size() < MAX_WORDNET_GRAPH)) {
			// test the lexical value of nouns and adjectives in WordNet

			for (Node n : graph.values()) {
				final KeyWord kw = (KeyWord) n.value;

				if (lang.isNoun(kw.pos)) {
					SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.NOUN);
				} else if (lang.isAdjective(kw.pos)) {
					SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.ADJECTIVE);
				}
			}

			// test the collocations in WordNet

			for (Node n : ngram_subgraph.values()) {
				final NGram gram = (NGram) n.value;

				if (gram.nodes.size() > 1) {
					SynsetLink.addKeyWord(synset_subgraph, n, gram.getCollocation(), POS.NOUN);
				}
			}

			synset_subgraph = SynsetLink.pruneGraph(synset_subgraph, graph);
		}

		// augment the graph with n-grams added as nodes

		for (Node n : ngram_subgraph.values()) {
			final NGram gram = (NGram) n.value;

			if (gram.length < MAX_NGRAM_LENGTH) {
				graph.put(n.key, n);

				for (Node keyword_node : gram.nodes) {
					n.connect(keyword_node);
				}
			}
		}

		markTime("augment_graph");

		//////////////////////////////////////////////////
		// PASS 4: re-run TextRank on the augmented graph

		initTime();

		graph.runTextRank();
		// graph.sortResults(graph.size() / 2);

		// collect stats for metrics

		final int ngram_max_count = NGram.calcStats(ngram_subgraph);

		if (use_wordnet) {
			SynsetLink.calcStats(synset_subgraph);
		}

		markTime("ngram_textrank");

		if (logger.isInfoEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("RANK: " + ngram_subgraph.dist_stats);

				for (Node n : new TreeSet<Node>(ngram_subgraph.values())) {
					final NGram gram = (NGram) n.value;
					logger.debug(gram.getCount() + " " + n.rank + " "
							+ gram.text /* + " @ " + gram.renderContexts() */);
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("RANK: " + synset_subgraph.dist_stats);

				for (Node n : new TreeSet<Node>(synset_subgraph.values())) {
					final SynsetLink s = (SynsetLink) n.value;
					logger.info("emit: " + s.synset + " " + n.rank + " " + s.relation);
				}
			}
		}

		//////////////////////////////////////////////////
		// PASS 5: construct a metric space for overall ranking

		initTime();

		final double link_min = ngram_subgraph.dist_stats.getMin();
		final double link_coeff = ngram_subgraph.dist_stats.getMax() - ngram_subgraph.dist_stats.getMin();

		final double count_min = 1;
		final double count_coeff = (double) ngram_max_count - 1;

		final double synset_min = synset_subgraph.dist_stats.getMin();
		final double synset_coeff = synset_subgraph.dist_stats.getMax() - synset_subgraph.dist_stats.getMin();

		for (Node n : ngram_subgraph.values()) {
			final NGram gram = (NGram) n.value;

			if (gram.length < MAX_NGRAM_LENGTH) {
				final double link_rank = (n.rank - link_min) / link_coeff;
				final double count_rank = (gram.getCount() - count_min) / count_coeff;
				final double synset_rank = use_wordnet ? n.maxNeighbor(synset_min, synset_coeff) : 0.0D;

				final MetricVector mv = new MetricVector(gram, link_rank, count_rank, synset_rank);
				metric_space.put(gram, mv);
			}
		}

		markTime("normalize_ranks");

		// return results

		return metric_space.values();
	}

	//////////////////////////////////////////////////////////////////////
	// access and utility methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Re-initialize the timer.
	 */

	public void initTime() {
		start_time = System.currentTimeMillis();
	}

	/**
	 * Report the elapsed time with a label.
	 */

	public void markTime(final String label) {
		elapsed_time = System.currentTimeMillis() - start_time;

		if (logger.isInfoEnabled()) {
			logger.info("ELAPSED_TIME:\t" + elapsed_time + "\t" + label);
		}
	}

	/**
	 * Accessor for the graph.
	 */

	public Graph getGraph() {
		return graph;
	}

	/**
	 * Serialize the graph to a file which can be rendered.
	 */

	public void serializeGraph(final String graph_file) throws Exception {
		for (Node n : graph.values()) {
			n.marked = false;
		}

		final TreeSet<String> entries = new TreeSet<String>();

		for (Node n : ngram_subgraph.values()) {
			final NGram gram = (NGram) n.value;
			final MetricVector mv = metric_space.get(gram);

			if (mv != null) {
				final StringBuilder sb = new StringBuilder();

				sb.append("rank").append('\t');
				sb.append(n.getId()).append('\t');
				sb.append(mv.render());
				entries.add(sb.toString());

				n.serializeGraph(entries);
			}
		}

		final OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(graph_file), "UTF-8");

		try {
			for (String entry : entries) {
				fw.write(entry, 0, entry.length());
				fw.write('\n');
			}
		} finally {
			fw.close();
		}
	}

	/**
	 * Serialize resulting graph to a string.
	 */

	@Override
	public String toString() {
		final TreeSet<MetricVector> key_phrase_list = new TreeSet<MetricVector>(metric_space.values());
		final StringBuilder sb = new StringBuilder();

		for (MetricVector mv : key_phrase_list) {
			if (mv.metric >= MIN_NORMALIZED_RANK) {
				sb.append(mv.render()).append("\t").append(mv.value.text).append("\n");
			}
		}

		return sb.toString();
	}

	//////////////////////////////////////////////////////////////////////
	// command line interface
	//////////////////////////////////////////////////////////////////////

	/**
	 * Main entry point.
	 */

	public static List<Keyword> extractKeywordTextRankWordnet(String text, NLP openNLPImpl, LanguageEnglish lang,
			String wordnetResPath, boolean useWordnet) {
		/**
		 * / final String res_path = new
		 * File(System.getProperty(NLP_RESOURCES)).getPath(); /*
		 */
		List<Keyword> keywords = new ArrayList<Keyword>();

		// set up logging for debugging and instrumentation

		// PropertyConfigurator.configure(cli.log4j_conf);

		// load the sample text from a file

		// filter out overly large files

		// main entry point for the algorithm
		TextRankWordnet tr = null;
		try {
			tr = new TextRankWordnet(openNLPImpl, lang, wordnetResPath, useWordnet);
			tr.prepCall(text);
		} catch (Exception e1) {
			logger.error(ExceptionUtils.getFullStackTrace(e1));
		}

		// wrap the call in a timed task

		final FutureTask<Collection<MetricVector>> task = new FutureTask<Collection<MetricVector>>(tr);
		Collection<MetricVector> answer = null;

		final Thread thread = new Thread(task);
		thread.run();

		try {
			// answer = task.get(); // run until complete
			answer = task.get(15000L, TimeUnit.MILLISECONDS); // timeout in N ms
		} catch (ExecutionException e) {
			logger.error("exec exception", e);
		} catch (InterruptedException e) {
			logger.error("interrupt", e);
		} catch (TimeoutException e) {
			logger.error("timeout", e);

			// Unfortunately, with graph size > 700, even read-only
			// access to WordNet on disk will block and cause the
			// thread to be uninterruptable. None of the following
			// remedies work...

			// thread.interrupt();
			// task.cancel(true);
			return keywords;
		}

		logger.info("\n" + tr);

		for (MetricVector mv : answer) {
			Keyword keyword = new Keyword(mv.value.text, mv.metric);
			keywords.add(keyword);
		}
		return keywords;
	}
}
