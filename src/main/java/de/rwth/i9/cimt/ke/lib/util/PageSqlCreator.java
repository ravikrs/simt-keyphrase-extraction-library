package de.rwth.i9.cimt.ke.lib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class PageSqlCreator {

	public static void main(String[] args) {
		BufferedWriter out = null;
		BufferedReader br = null;
		try {
			FileInputStream fis = new FileInputStream(
					new File("C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\Page.txt"));
			br = new BufferedReader(new InputStreamReader(fis));

			FileWriter fstream = new FileWriter(
					"C:\\rks\\Thesis\\Softwares\\Wikipedia dump\\dummy\\dump\\output\\Page.sql", true);
			out = new BufferedWriter(fstream);
			String aLine = null;
			boolean isFirst = true;
			while ((aLine = br.readLine()) != null) {
				String[] tokens = aLine.split("\\\t");
				if (tokens.length >= 3) {
					if (isFirst) {
						out.write("(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "') ");
						isFirst = false;
					} else {
						out.write(",(" + tokens[0] + "," + tokens[1] + ",'" + tokens[2] + "') ");
					}
				}

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
