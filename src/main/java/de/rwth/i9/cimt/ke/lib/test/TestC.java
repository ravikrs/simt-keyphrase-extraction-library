package de.rwth.i9.cimt.ke.lib.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.ukp.wikipedia.api.Page;

public class TestC {
	private static Set<Long> genericWikipediaCategories;

	public static void main(String[] args) throws Exception {
	}

	private static void loadGenericWikipediaCategories() throws IOException {
		BufferedReader br = null;
		FileReader fr = null;
		BufferedWriter bw = null;
		FileWriter fw = null;
		genericWikipediaCategories = new HashSet<>();
		try {
			fr = new FileReader("src/main/resources/WikipediaSpecificCategories.txt");
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				if (!sCurrentLine.isEmpty() && !sCurrentLine.startsWith("#")) {
					long categoryPageId = Long.parseLong(sCurrentLine);
					genericWikipediaCategories.add(categoryPageId);
				}
			}
		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}

	}

	private static Map<String, Integer> getPageLinks(Page page) {
		Map<String, Integer> pageLinks = new HashMap<>();
		String pageText = page.getText();

		String linkRegex = "\\[\\[\\s*(.+?)\\s*]]";
		Pattern pattern = Pattern.compile(linkRegex);
		Matcher matcher = pattern.matcher(pageText);

		while (matcher.find()) {
			String link = matcher.group(1);
			if (pageLinks.containsKey(link)) {
				pageLinks.put(link, Integer.valueOf(((Integer) pageLinks.get(link)).intValue() + 1));
			} else {
				pageLinks.put(link, Integer.valueOf(1));
			}
		}
		return pageLinks;
	}

}
