package au.edu.unimelb.csse.analyser;
/*******************************************************************************
 * Copyright 2011 The fangorn project
 * 
 *        Author: Sumukh Ghodke
 * 
 *        Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 ******************************************************************************/


import java.io.BufferedReader;
import java.io.IOException;

public class SentenceTokenizer {
	protected static final int SENT_BUFFER_SIZE = 100;
	private int charIndex = -1;
	private String line = null;
	private BufferedReader input;
	private int lineNumber;
	private int startLineNumber;

	private static final int SENTENCE_OPEN = 1;
	private static final int OPEN = 2;
	private static final int CLOSE = 4;
	private static final int SPACE = 8;
	private static final int TEXT = 16;
	private static final int SENTENCE_END = 32;
	private static final int END = 64;

	public SentenceTokenizer(BufferedReader reader) {
		input = reader;
		lineNumber = 0;
	}

	public SentenceAndMetaData next() throws IOException {
		if (charIndex == -1 || (line != null && charIndex == line.length())) {
			do {
				line = input.readLine();
				if (line == null) {
					return null;
				}
				lineNumber++;
			} while (line.trim().length() == 0 || line.trim().startsWith("*"));
			charIndex = 0;
		}
		int bracketCount = 0;
		int length = 0;
		startLineNumber = lineNumber;
		char[] sent = new char[SENT_BUFFER_SIZE];
		int prevToken = -1;
		int prevChar = -1;
		int expect = SENTENCE_OPEN;
		while (charIndex < line.length()) {
			char c = line.charAt(charIndex++);
			if (c == '(') {
				if (expect != SENTENCE_OPEN && (expect & OPEN) != OPEN) {
					return someErrorHereTryNextLine();
				}
				if (expect == SENTENCE_OPEN) {
					expect = OPEN;
					prevToken = SENTENCE_OPEN;
				} else {
					expect = TEXT;
					prevToken = OPEN;
				}
				// retain the order of these statements
				if (bracketCount > 0) {
					if (length > 0 && Character.isWhitespace(sent[length - 1])) {
						sent[length - 1] = c;
					} else {
						sent = add(sent, length++, c);
					}
				}
				prevChar = OPEN;
				bracketCount++;
			} else if (c == ')') {
				// retain the order of these statements
				if (expect != SENTENCE_END && (expect & CLOSE) != CLOSE) {
					return someErrorHereTryNextLine();
				}
				prevToken = CLOSE;
				bracketCount--;
				if (expect == SENTENCE_END) {
					if (bracketCount == 0)
						expect = END;
					else {
						// dont think this else part is reachable
						return someErrorHereTryNextLine();
					}
				} else if ((expect & CLOSE) == CLOSE && bracketCount == 1) {
					expect = SENTENCE_END;
				} else {
					expect = OPEN | CLOSE;
				}
				if (bracketCount > 0) {
					if (length > 0 && Character.isWhitespace(sent[length - 1])) {
						sent[length - 1] = c;
					} else {
						sent = add(sent, length++, c);
					}
				}
				prevChar = CLOSE;
			} else if (Character.isWhitespace(c) && bracketCount > 0) {
				if ((expect & SPACE) == SPACE) {
					expect = TEXT | OPEN;
					prevToken = SPACE;
				}
				if (length > 0 && !Character.isWhitespace(sent[length - 1])
						&& !('(' == sent[length - 1])
						&& !(')' == sent[length - 1])) {
					sent = add(sent, length++, c);
				}
				prevChar = SPACE;
			} else if (!(0 == (int) c) && bracketCount > 0) {
				if (prevChar == TEXT) {
					sent = add(sent, length++, c);
				} else if ((expect & TEXT) != TEXT) {
					return someErrorHereTryNextLine();
				} else {
					if (prevToken == OPEN) {
						expect = OPEN | SPACE;
					} else if (prevToken == SPACE) {
						expect = CLOSE;
					} else {
						return someErrorHereTryNextLine();
					}
					sent = add(sent, length++, c);
				}
				prevChar = TEXT;
			}
			if (bracketCount == 0 && expect != END) {
				return someErrorHereTryNextLine();
			}
			if (bracketCount == 0 && length > 0) {
				char[] sentcopy = new char[length];
				System.arraycopy(sent, 0, sentcopy, 0, length);
				final String s = String.valueOf(sentcopy);
				final int start = startLineNumber;
				final int nol = lineNumber - startLineNumber + 1;
				// sentence found
				return new SentenceAndMetaData() {

					public String sentence() {
						return s;
					}

					public int lineOffset() {
						return start;
					}

					public int numberOfLines() {
						return nol;
					}

				};
			}
			if (charIndex == line.length()) {
				do {
					line = input.readLine();
					if (line == null) {
						return null;
					}
					lineNumber++;
				} while (line.trim().length() == 0
						&& !line.trim().startsWith("*"));
				charIndex = 0;
			}
		}
		return null;
	}

	private SentenceAndMetaData someErrorHereTryNextLine() throws IOException {
		// Move over to the next line
		charIndex = line.length();
		return next();
	}

	private char[] add(char[] sent, int length, char c) {
		if (length == sent.length) {
			char oldData[] = sent;
			int newCapacity = sent.length + SENT_BUFFER_SIZE;
			sent = new char[newCapacity];
			System.arraycopy(oldData, 0, sent, 0, oldData.length);
		}
		sent[length] = c;
		return sent;
	}

	public void reset(BufferedReader input) {
		if (this.input != null) {
			try {
				this.input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.input = input;
		charIndex = -1;
		line = null;
		lineNumber = 0;
		startLineNumber = 0;
	}

	public void close() throws IOException {
		input.close();
	}

}
