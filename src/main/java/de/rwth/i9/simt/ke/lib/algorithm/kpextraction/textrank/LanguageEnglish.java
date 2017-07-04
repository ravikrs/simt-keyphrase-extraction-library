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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tartarus.snowball.ext.EnglishStemmer;

import de.rwth.i9.simt.nlp.opennlp.OpenNLPImplSpring;

/**
 * Implementation of English-specific tools for natural language processing.
 *
 * @author paco@sharethis.com
 */

@Service("languageEnglish")
public class LanguageEnglish extends LanguageModel {
	// logging
	private static final Logger logger = LoggerFactory.getLogger(LanguageEnglish.class);

	@Autowired
	OpenNLPImplSpring openNLPImplSpring;

	public static EnglishStemmer stemmer_en = null;

	/**
	 * Constructor. Not quite a Singleton pattern but close enough given the
	 * resources required to be loaded ONCE.
	 */

	public LanguageEnglish() {
		stemmer_en = new EnglishStemmer();
	}

	/**
	 * Tokenize the sentence text into an array of tokens.
	 */

	public String[] tokenizeSentence(final String text) {
		final String[] token_list = openNLPImplSpring.tokenize(text);

		for (int i = 0; i < token_list.length; i++) {
			token_list[i] = token_list[i].replace("\"", "").toLowerCase().trim();
		}

		return token_list;
	}

	/**
	 * Run a part-of-speech tagger on the sentence token list.
	 */

	public String[] tagTokens(final String[] token_list) {
		return openNLPImplSpring.tagPartOfSpeech(token_list);
	}

	/**
	 * Prepare a stable key for a graph node (stemmed, lemmatized) from a token.
	 */

	public String getNodeKey(final String text, final String pos) throws Exception {
		return pos.substring(0, 2) + stemToken(scrubToken(text)).toLowerCase();
	}

	/**
	 * Determine whether the given PoS tag is a noun.
	 */

	public boolean isNoun(final String pos) {
		return pos.startsWith("NN");
	}

	/**
	 * Determine whether the given PoS tag is an adjective.
	 */

	public boolean isAdjective(final String pos) {
		return pos.startsWith("JJ");
	}

	/**
	 * Perform stemming on the given token.
	 */

	public String stemToken(final String token) {
		stemmer_en.setCurrent(token);
		stemmer_en.stem();

		return stemmer_en.getCurrent();
	}

}
