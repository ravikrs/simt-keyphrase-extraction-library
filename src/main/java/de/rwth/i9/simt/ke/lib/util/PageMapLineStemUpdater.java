package de.rwth.i9.simt.ke.lib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import de.rwth.i9.simt.nlp.util.StemmerEn;

public class PageMapLineStemUpdater {

	public static void main(String[] args) {

		BufferedWriter out = null;
		BufferedReader br = null;
		int iter = 0;

		try {
			FileInputStream fis = new FileInputStream(
					new File("C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\PageMapLine.txt"));
			br = new BufferedReader(new InputStreamReader(fis));

			FileWriter fstream = new FileWriter(
					"C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\PageMapLine.sql", true);
			out = new BufferedWriter(fstream);
			String aLine = null;
			boolean isFirst = true;

			out.write("INSERT INTO `enwikidb`.`pagemapline` (`id`, `name`, `pageID`, `stem`) VALUES ");
			while ((aLine = br.readLine()) != null) {
				String[] tokens = aLine.split("\\\t");
				String name = tokens[1].trim().replace("\\'", "''");
				String stem = StemmerEn.stemToken(tokens[1].trim().replace("\\'", "'"));
				stem = stem.replace("'", "''");
				if (isFirst) {
					out.write("(" + tokens[0] + ",'" + name + "'," + tokens[2] + ",'" + stem + "') ");
					isFirst = false;
				} else {
					out.write(",(" + tokens[0] + ",'" + name + "'," + tokens[2] + ",'" + stem + "') ");
				}
				//out.write("\n");
				//				iter++;
				//				if (iter == 4) {
				//					throw new IOException();
				//				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.write(";");
				out.flush();
				br.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// close buffer writer

		}

	}

}
