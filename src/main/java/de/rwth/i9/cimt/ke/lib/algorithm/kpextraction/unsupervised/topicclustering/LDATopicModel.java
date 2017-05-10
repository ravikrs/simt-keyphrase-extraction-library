package de.rwth.i9.cimt.ke.lib.algorithm.kpextraction.unsupervised.topicclustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class LDATopicModel {
	private static final Logger log = LoggerFactory.getLogger(LDATopicModel.class);
	private static ParallelTopicModel model = null;
	private static ArrayList<Pipe> pipeList = null;
	public static final int numTopics = 50;

	public static List<Double> computeTopicProbability(String cimtHome, String documentContent) {
		if (model == null) {
			try {
				loadTopicModel(cimtHome);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(ExceptionUtils.getStackTrace(e));
			}

		}
		List<Double> topicIdProbMap = new ArrayList<>();
		if (model != null) {
			// Create a new instance named "test instance" with empty target and source fields.
			InstanceList instances = new InstanceList(new SerialPipes(pipeList));
			TopicInferencer inferencer = model.getInferencer();
			instances.addThruPipe(new Instance(documentContent, null, "test instance", null));
			double[] testProbabilities = inferencer.getSampledDistribution(instances.get(0), 10, 1, 5);
			for (double prob : testProbabilities) {
				topicIdProbMap.add(prob);
			}
		}
		return topicIdProbMap;

	}

	private static void loadTopicModel(String cimtHome) throws IOException {
		// Begin by importing documents from text to feature sequences
		pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
		pipeList.add(new TokenSequenceRemoveStopwords(new File(cimtHome + "/lda/stoplists/en.txt"), "UTF-8", false,
				false, false));
		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		Reader fileReader = new InputStreamReader(
				new FileInputStream(new File(cimtHome + "/lda/corpus/palm_corpus_ta.txt")), "UTF-8");
		instances
				.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, label, name fields

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is 
		model = new ParallelTopicModel(numTopics, 1.0, 0.01);
		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(2);

		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(1000);
		model.estimate();

	}

}
