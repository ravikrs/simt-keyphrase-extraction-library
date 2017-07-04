package de.rwth.i9.simt.ke.lib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

public class PageSqlCreator {
	private static Set<Long> genericWikipediaCategories;

	public static void main(String[] args) throws IOException {
		BufferedWriter out = null;
		BufferedReader br = null;
		int iter = 0;

		try {
			FileInputStream fis = new FileInputStream(
					new File("C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\Page.txt"));
			br = new BufferedReader(new InputStreamReader(fis));

			FileWriter fstream = new FileWriter(
					"C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\Page.sql", true);
			out = new BufferedWriter(fstream);
			String aLine = null;
			boolean isFirst = true;

			out.write("INSERT INTO `enwikidb`.`page` (`id`,`pageId`,`name`,`text`,`isDisambiguation`) VALUES ");
			while ((aLine = br.readLine()) != null) {
				String[] tokens = aLine.split("\\\t");

				if (tokens.length == 4) {
					if (isFirst) {
						out.write("(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "','" + tokens[2] + "',0) ");
						isFirst = false;
					} else {
						out.write(",(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "','" + tokens[2] + "',0) ");
					}

				} else if (tokens.length == 5) {
					if (isFirst) {
						out.write("(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "','" + tokens[2] + "',1) ");
						isFirst = false;
					} else {
						out.write(",(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "','" + tokens[2] + "',1) ");
					}

				}
				//				iter++;
				//				if (iter == 1) {
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
