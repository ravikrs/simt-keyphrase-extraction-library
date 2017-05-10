package de.rwth.i9.cimt.ke.lib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class LdaPalmCorpusCreator {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String CorpusFilePath = "C:\\Users\\singh\\Desktop\\mallet-2.0.8\\palm\\palm.txt";
		String CorpusDirPath = "C:\\Users\\singh\\Desktop\\mallet-2.0.8\\palm\\output";

		BufferedWriter out = null;
		BufferedReader br = null;
		try {
			System.out.print("START");
			FileInputStream fis = new FileInputStream(new File(CorpusFilePath));
			String CorpusContent = IOUtils.toString(fis);
			String[] tokens = CorpusContent.split("ravikrsingh20");
			for (int i = 0; i < tokens.length; i++) {
				try {
					File file = new File(CorpusDirPath + File.separator + i + ".txt");
					file.createNewFile();
					FileWriter fstream = new FileWriter(file);
					out = new BufferedWriter(fstream);
					out.write(tokens[i]);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						out.flush();
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
			System.out.print("DONE");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
