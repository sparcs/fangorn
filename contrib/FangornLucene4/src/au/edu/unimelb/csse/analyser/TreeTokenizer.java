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
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.PayloadFormatException;

public class TreeTokenizer extends Tokenizer {

	private String sentence;
	private TreeLexer lexer = new TreeLexer();
	private int numTokens; // total number of tokens in the sentence
	private int tokenPos; // current position of the returned token
	private int[] payloads = new int[512];
	private BytesRef[] payloadBytesRefs = null;
	private int[] textPositions = new int[256];
	private int[] tailUpdateStack = new int[128]; // helps assign right and
													// parent ids

	private LRDP nodePositionAware;
	private int positionLength;

	private static final int OPEN_B_TOK = 1;
	private static final int CLOSE_B_TOK = 2;
	private static final int SPACE_TOK = 4;
	private static final int TEXT_TOK = 8;

	private static final int START_STATE = 0;
	private static final int OPEN_STATE = 1;
	private static final int CLOSE_STATE = 2;
	private static final int SPACE_STATE = 3;
	private static final int TEXT_STATE = 4;
	private static final int ERROR_STATE = 5;

	// token attributes; position attribute is added in the constructor
	private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
	private final PayloadAttribute payloadAttribute = addAttribute(PayloadAttribute.class);

	private class State {
		int expectedToken;
		int effPrevState;

		State() {
			expectedToken = OPEN_B_TOK;
			effPrevState = START_STATE;
		}
	}

	public TreeTokenizer(Reader input, LRDP nodePositionAware) {
		super(input);
		addAttribute(PositionIncrementAttribute.class); // adds default
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
	}

	@Override
	public void reset() throws IOException {
		BufferedReader br = new BufferedReader(input);
		String line = br.readLine();
		StringBuilder sb = null;
		String newline;
		while ((newline = br.readLine()) != null) {
			if (sb == null) {
				sb = new StringBuilder();
				sb.append(line);
			}
			sb.append(newline);
		}
		try {
			resetState(sb == null ? line : sb.toString());
		} catch (ParseException e) { // hack!
			br.close();
			throw new IOException(e.getMessage());
		}
		br.close();
	}

	public void resetState(String sentence) throws ParseException {
		this.sentence = sentence;
		numTokens = 0;
		tokenPos = 0;
		parse(lexer.tokenMarkerPos(sentence));
	}

	private void parse(int[] positions) throws ParseException {
		State prev = new State();
		int state = START_STATE;
		int prevTextPos = -1;
		int lastRight = 0;
		int lastParent = 0;
		int unmatchedOpenNum = 0;
		int maxTokStorePos = -1;
		int stackSize = 0;
		for (int i = 0; i < positions.length; i = i + 2) {
			if (state == ERROR_STATE) {
				throw new ParseException("Cannot parse sentence " + sentence);
			}
			char c = sentence.charAt(positions[i]);
			int tokenType = getTokenType(c);
			switch (state) {
			case START_STATE:
				if (tokenType == OPEN_B_TOK) {
					state = OPEN_STATE;
				} else if (tokenType == SPACE_TOK) {
					state = SPACE_STATE;
				} else {
					state = ERROR_STATE;
				}
				break;
			case OPEN_STATE:
				unmatchedOpenNum++;
				if (tokenType == TEXT_TOK) {
					state = TEXT_STATE;
					prevTextPos = i;
				} else if (tokenType == SPACE_TOK) {
					state = SPACE_STATE;
				} else {
					state = ERROR_STATE;
				}
				prev.effPrevState = OPEN_STATE;
				prev.expectedToken = TEXT_TOK;
				break;
			case CLOSE_STATE:
				if (prev.effPrevState == TEXT_STATE) {
					if (tokenType == SPACE_TOK) {
						state = SPACE_STATE;
						prev.expectedToken = OPEN_B_TOK | CLOSE_B_TOK;
					} else if (tokenType == OPEN_B_TOK) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B_TOK;
					} else if (tokenType == CLOSE_B_TOK) {
						state = CLOSE_STATE;
						prev.expectedToken = CLOSE_B_TOK;
					} else {
						state = ERROR_STATE;
					}
					prev.effPrevState = CLOSE_STATE;
				} else if (prev.effPrevState == CLOSE_STATE) {
					// if the prev state was a close state we'll have to update
					// the stack
					lastParent++;
					stackSize = setTailPayloadNonTml(lastRight, lastParent,
							unmatchedOpenNum, stackSize);

					if (tokenType == SPACE_TOK) {
						state = SPACE_STATE;
						prev.expectedToken = OPEN_B_TOK | CLOSE_B_TOK;
					} else if (tokenType == OPEN_B_TOK) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B_TOK;
					} else if (tokenType == CLOSE_B_TOK) {
						state = CLOSE_STATE;
						prev.expectedToken = CLOSE_B_TOK;
					} else {
						state = ERROR_STATE;
					}
					prev.effPrevState = CLOSE_STATE;
				} else {
					state = ERROR_STATE;
				}
				unmatchedOpenNum--;
				break;
			case SPACE_STATE:
				if (tokenType == TEXT_TOK) {
					state = TEXT_STATE;
					prevTextPos = i;
					if ((prev.expectedToken & SPACE_TOK) == SPACE_TOK) {
						prev.effPrevState = SPACE_STATE;
						prev.expectedToken = TEXT_TOK;
					} else if (!(prev.effPrevState == OPEN_STATE)) {
						state = ERROR_STATE;
					}
				} else if (tokenType == OPEN_B_TOK) {
					state = OPEN_STATE;
					if ((prev.expectedToken & OPEN_B_TOK) == OPEN_B_TOK) {
						prev.expectedToken = OPEN_B_TOK;
					} else {
						state = ERROR_STATE;
					}
				} else if (tokenType == CLOSE_B_TOK) {
					state = CLOSE_STATE;
					if ((prev.expectedToken & CLOSE_B_TOK) == CLOSE_B_TOK) {
						prev.expectedToken = CLOSE_B_TOK;
					} else {
						state = ERROR_STATE;
					}
				} else {
					state = ERROR_STATE;
				}
				break;
			case TEXT_STATE:
				maxTokStorePos++;
				if (prev.effPrevState == OPEN_STATE) {
					setPosMarksForTokenText(positions, prevTextPos,
							maxTokStorePos);
					setPartPayload(lastRight, unmatchedOpenNum - 1,
							maxTokStorePos, stackSize++);
					if (tokenType == SPACE_TOK) {
						state = SPACE_STATE;
						prev.expectedToken = SPACE_TOK | OPEN_B_TOK;
					} else if (tokenType == OPEN_B_TOK) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B_TOK;
					} else {
						state = ERROR_STATE;
					}
				} else if (prev.effPrevState == SPACE_STATE) { // leaf text
					// token
					lastParent++;
					// here depth = numberOfUnmatchedOpens and not
					// numberOfUnmatchedOpens - 1
					setPosMarksForTokenText(positions, prevTextPos,
							maxTokStorePos);
					setPartPayload(lastRight, unmatchedOpenNum, maxTokStorePos,
							stackSize++);
					lastRight++;
					setPartTailPayloadLeafPreTml(lastRight, lastParent,
							maxTokStorePos);
					stackSize--; // pop leaf from stack
					nodePositionAware.setRight(payloads,
							tailUpdateStack[stackSize - 1] * positionLength,
							lastRight); // necessary?
					prev.expectedToken = CLOSE_B_TOK;
					if (tokenType == SPACE_TOK) {
						state = SPACE_STATE;
					} else if (tokenType == CLOSE_B_TOK) {
						state = CLOSE_STATE;
					} else {
						state = ERROR_STATE;
					}
				} else {
					state = ERROR_STATE;
				}
				prev.effPrevState = TEXT_STATE;
				break;
			}
		}
		if (state == CLOSE_STATE) {
			if (prev.effPrevState == CLOSE_STATE) {
				// update the stack if looping in close state
				lastParent++;
				stackSize = setTailPayloadNonTml(lastRight, lastParent,
						unmatchedOpenNum, stackSize);
			}
			unmatchedOpenNum--;
		} else if (!(state == SPACE_STATE)) {
			throw new ParseException("Cannot parse sentence " + sentence);
		}

		if (stackSize != 1) {
			throw new ParseException("Cannot parse sentence " + sentence
					+ ". Brackets might be missing.");
		}
		prev = null;
		numTokens = maxTokStorePos + 1;
		try {
			payloadBytesRefs = nodePositionAware.encode(
					payloads, numTokens);
		} catch (PayloadFormatException e) {
			throw new ParseException("Cannot parse sentence " + sentence
					+ "." + e.getMessage());
		}
	}

	private int setTailPayloadNonTml(int right, int parent, int relevantDepth,
			int stackSize) {
		while (stackSize > 0
				&& nodePositionAware.depth(payloads,
						tailUpdateStack[stackSize - 1] * positionLength) == relevantDepth) {
			nodePositionAware.setParent(payloads,
					tailUpdateStack[stackSize - 1] * positionLength, parent);
			stackSize--;
		}
		if (stackSize > 0) {
			nodePositionAware.setRight(payloads, tailUpdateStack[stackSize - 1]
					* positionLength, right);
		}
		return stackSize;
	}

	private void setPartTailPayloadLeafPreTml(int right, int parent, int tokPos) {
		setTailPayloadLeaf(right, parent, tokPos);
		// only right payload for pre-terminal
		nodePositionAware.setRight(payloads, (tokPos - 1) * positionLength,
				right);
	}

	private void setTailPayloadLeaf(int right, int parent, int maxTokStorePos) {
		nodePositionAware.setRight(payloads, maxTokStorePos * positionLength,
				right);
		nodePositionAware.setParent(payloads, maxTokStorePos * positionLength,
				parent);
	}

	private void setPartPayload(int left, int depth, int tokPos, int stackSize) {
		pushPositionOntoNodeStack(tokPos, stackSize);
		setLeftAndDepthPayload(left, depth, tokPos);
	}

	private void setLeftAndDepthPayload(int left, int depth, int tokPos) {
		try {
			// ensure length sufficient
			payloads[tokPos * positionLength + positionLength - 1] = 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldPayloads = payloads;
			payloads = new int[oldPayloads.length + 128];
			System.arraycopy(oldPayloads, 0, payloads, 0, oldPayloads.length);
		}
		nodePositionAware.setLeft(payloads, tokPos * positionLength, left);
		nodePositionAware.setDepth(payloads, tokPos * positionLength, depth);
	}

	private void pushPositionOntoNodeStack(int tokPos, int stackSize) {
		try {
			tailUpdateStack[stackSize] = tokPos;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldStack = tailUpdateStack;
			tailUpdateStack = new int[oldStack.length + 128];
			System.arraycopy(oldStack, 0, tailUpdateStack, 0, oldStack.length);
			// note that the increment would have been performed before the
			// exception is thrown
			tailUpdateStack[stackSize] = tokPos;
		}
	}

	private void setPosMarksForTokenText(int[] positions, int i,
			int maxTokStorePos) {
		try {
			textPositions[2 * maxTokStorePos] = positions[i];
			textPositions[2 * maxTokStorePos + 1] = positions[i + 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldTextPos = textPositions;
			textPositions = new int[oldTextPos.length + 256];
			System.arraycopy(oldTextPos, 0, textPositions, 0, oldTextPos.length);
			textPositions[2 * maxTokStorePos] = positions[i];
			textPositions[2 * maxTokStorePos + 1] = positions[i + 1];
		}
	}

	/**
	 * returns token type for tokens returned by the lexer
	 * 
	 * @param c
	 * @return
	 */
	private int getTokenType(char c) {
		switch (c) {
		case '(':
			return OPEN_B_TOK;
		case ')':
			return CLOSE_B_TOK;
		case ' ':
			return SPACE_TOK;
		}
		return TEXT_TOK;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (tokenPos < numTokens) {
			clearAttributes();
			charTermAttribute.append(sentence, textPositions[2 * tokenPos],
					textPositions[2 * tokenPos + 1]);
			payloadAttribute.setPayload(payloadBytesRefs[tokenPos]);
			tokenPos++;
			return true;
		}
		return false;
	}
}
