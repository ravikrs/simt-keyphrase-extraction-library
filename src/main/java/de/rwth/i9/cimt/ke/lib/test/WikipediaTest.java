package de.rwth.i9.cimt.ke.lib.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;

public class WikipediaTest {
	private static Set<Long> genericWikipediaCategories;

	public static void main(String[] args) throws Exception {
		DatabaseConfiguration dbConfig = new DatabaseConfiguration();
		dbConfig.setHost("localhost");
		dbConfig.setDatabase("enwikidb");
		dbConfig.setUser("wikiuser");
		dbConfig.setPassword("wikiuser");
		dbConfig.setLanguage(de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language.valueOf("english"));
		loadGenericWikipediaCategories();
		//		Wikipedia wiki = null;
		//		try {
		//			wiki = new Wikipedia(dbConfig);
		//		} catch (WikiInitializationException e) {
		//			e.printStackTrace();
		//		}
		//		List<String> keywordTokens = Arrays.asList("open learning analytics ecosystem", "open learning analytics",
		//				"networked learning environments", "learning analytics", "effective learning experiences",
		//				"technology-enhanced learning", "present key conceptual", "networked environments", "last few years",
		//				"technical ideas", "tel landscape", "best support", "research field", "fast-paced change");
		//
		//		for (String token : keywordTokens) {
		//			if (wiki.existsPage(token)) {
		//				Page p = wiki.getPage(token);
		//				System.out.println("PAGE -> " + p.getTitle());
		//				for (Category category : p.getCategories()) {
		//					if (!genericWikipediaCategories.contains(new Long(category.getPageId()))) {
		//						System.out.println("Category : internal id -> " + category.__getId() + " wikipedia id -> "
		//								+ category.getPageId() + " title -> " + category.getTitle());
		//					}
		//
		//				}
		//			}
		//		}

	}

	private static void loadGenericWikipediaCategories() throws IOException {
		BufferedReader br = null;
		FileReader fr = null;
		BufferedWriter bw = null;
		FileWriter fw = null;
		genericWikipediaCategories = new HashSet<>();
		try {
			fr = new FileReader("src/main/resources/Raw.txt");
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				if (!sCurrentLine.isEmpty()) {
					long categoryPageId = Long.parseLong(sCurrentLine);
					genericWikipediaCategories.add(categoryPageId);
				}
			}
			fw = new FileWriter("src/main/resources/WikipediaSpecificCategories.txt");
			bw = new BufferedWriter(fw);
			for (long l : genericWikipediaCategories) {
				fw.append(String.valueOf(l) + "\n");

			}
		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();
				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}

	}

}
