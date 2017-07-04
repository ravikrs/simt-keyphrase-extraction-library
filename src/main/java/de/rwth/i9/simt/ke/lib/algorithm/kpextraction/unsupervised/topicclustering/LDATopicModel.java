package de.rwth.i9.simt.ke.lib.algorithm.kpextraction.unsupervised.topicclustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	private static SerialPipes pipes = null;
	public static final int numTopics = 50;

	public static List<Double> computeTopicProbability(String cimtHome, String documentContent) {
		if (model == null || pipes == null) {
			try {
				loadTopicModel(cimtHome);
			} catch (IOException e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}

		}
		List<Double> topicIdProbMap = new ArrayList<>();
		if (model != null && pipes != null) {
			// Create a new instance named "test instance" with empty target and source fields.
			InstanceList instances = new InstanceList(pipes);
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

		FileInputStream modelfis = null;
		ObjectInputStream modelois = null;
		FileOutputStream modelfos = null;
		ObjectOutputStream modeloos = null;
		FileInputStream pipesfis = null;
		ObjectInputStream pipesois = null;
		FileOutputStream pipesfos = null;
		ObjectOutputStream pipesoos = null;
		try {

			File pipesModel = new File(cimtHome + "/lda/model/ldapipes.ser");
			if (pipesModel.exists()) {
				pipesfis = new FileInputStream(pipesModel);
				pipesois = new ObjectInputStream(pipesfis);
				pipes = (SerialPipes) pipesois.readObject();
			} else {
				ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

				// Pipes: lowercase, tokenize, remove stopwords, map to features
				pipeList.add(new CharSequenceLowercase());
				pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
				pipeList.add(new TokenSequenceRemoveStopwords(new File(cimtHome + "/lda/stoplists/en.txt"), "UTF-8",
						false, false, false));
				pipeList.add(new TokenSequence2FeatureSequence());
				pipes = new SerialPipes(pipeList);

				pipesModel.createNewFile();
				pipesfos = new FileOutputStream(pipesModel.getPath());
				pipesoos = new ObjectOutputStream(pipesfos);
				pipesoos.writeObject(pipes);
			}

			File ldaModel = new File(cimtHome + "/lda/model/ldamodel.ser");
			if (ldaModel.exists()) {
				modelfis = new FileInputStream(ldaModel.getPath());
				modelois = new ObjectInputStream(modelfis);
				model = (ParallelTopicModel) modelois.readObject();

			} else {

				InstanceList instances = new InstanceList(pipes);
				Reader fileReader = new InputStreamReader(
						new FileInputStream(new File(cimtHome + "/lda/corpus/ldacorpus.txt")), "UTF-8");
				instances.addThruPipe(
						new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, label, name fields
				model = new ParallelTopicModel(numTopics, 1.0, 0.01);
				model.addInstances(instances);

				// Use two parallel samplers, which each look at one half the corpus and combine
				//  statistics after every iteration.
				model.setNumThreads(2);

				// Run the model for 50 iterations and stop (this is for testing only, 
				//  for real applications, use 1000 to 2000 iterations)
				model.setNumIterations(1000);
				model.estimate();

				ldaModel.createNewFile();
				modelfos = new FileOutputStream(ldaModel.getPath());
				modeloos = new ObjectOutputStream(modelfos);
				modeloos.writeObject(model);
			}

		} catch (IOException ex) {
			log.error("Could not read model from file: " + ex);
		} catch (ClassNotFoundException ex) {
			log.error("Could not load the model: " + ex);
		} finally {
			if (modelois != null) {
				modelois.close();
			}
			if (modelfis != null) {
				modelfis.close();
			}
			if (modeloos != null) {
				modeloos.close();
			}
			if (modelfos != null) {
				modelfos.close();
			}
			if (pipesois != null) {
				pipesois.close();
			}
			if (pipesfis != null) {
				pipesfis.close();
			}
			if (pipesoos != null) {
				pipesoos.close();
			}
			if (pipesfos != null) {
				pipesfos.close();
			}

		}

	}

}
