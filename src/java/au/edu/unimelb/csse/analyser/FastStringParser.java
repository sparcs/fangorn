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
package au.edu.unimelb.csse.analyser;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.index.Payload;

import au.edu.unimelb.csse.ParseException;

public class FastStringParser {
	private String sentence;
	private FastStringLexer tokenizer = new FastStringLexer();
	private int currentPos;
	private int tokenPos;
	private int[] payloads = new int[128];
	private int[] textPositions = new int[256];
	private int[] stack = new int[128];
	private int stackSize;

	private static final int OPEN_B = 1;
	private static final int CLOSE_B = 2;
	private static final int SPACE = 4;
	private static final int TEXT = 8;

	private static final int START_STATE = 0;
	private static final int OPEN_STATE = 1;
	private static final int CLOSE_STATE = 2;
	private static final int SPACE_STATE = 3;
	private static final int TEXT_STATE = 4;
	private static final int ERROR_STATE = 5;

	private class State {
		int expectedToken;
		int effPrevState;

		State() {
			expectedToken = OPEN_B;
			effPrevState = START_STATE;
		}
	}
	
	public void reset(String sentence) throws ParseException {
		this.sentence = sentence;
		currentPos = -1;
		stackSize = 0;
		tokenPos = 0;
		parse(tokenizer.tokens(sentence));
	}

	private void parse(int[] positions) throws ParseException {
		State prev = new State();
		int state = START_STATE;
		int lastRight = 0;
		int prevTextPos = -1;
		int lastParent = 0;
		int numberOfUnmatchedOpens = 0;
		for (int i = 0; i < positions.length; i = i + 2) {
			if (state == ERROR_STATE) {
				throw new ParseException("Cannot parse sentence " + sentence);
			}
			char c = sentence.charAt(positions[i]);
			int nextTokenType = getTokenType(c);
			switch (state) {
			case START_STATE:
				if (nextTokenType == OPEN_B) {
					state = OPEN_STATE;
				} else if (nextTokenType == SPACE) {
					state = SPACE_STATE;
				} else {
					state = ERROR_STATE;
				}
				break;
			case OPEN_STATE:
				numberOfUnmatchedOpens++;
				if (nextTokenType == TEXT) {
					state = TEXT_STATE;
					prevTextPos = i;
				} else if (nextTokenType == SPACE) {
					state = SPACE_STATE;
				} else {
					state = ERROR_STATE;
				}
				prev.effPrevState = OPEN_STATE;
				prev.expectedToken = TEXT;
				break;
			case CLOSE_STATE:
				if (prev.effPrevState == TEXT_STATE) {
					if (nextTokenType == SPACE) {
						state = SPACE_STATE;
						prev.expectedToken = OPEN_B | CLOSE_B;
					} else if (nextTokenType == OPEN_B) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B;
					} else if (nextTokenType == CLOSE_B) {
						state = CLOSE_STATE;
						prev.expectedToken = CLOSE_B;
					} else {
						state = ERROR_STATE;
					}
					prev.effPrevState = CLOSE_STATE;
				} else if (prev.effPrevState == CLOSE_STATE) {
					// if the prev state was a close state we'll have to
					// update the stack
					int depthOfInterest = numberOfUnmatchedOpens;
					lastParent++;
					while (stackSize > 0
							&& depth(payloads[stack[stackSize - 1]]) == depthOfInterest) {
						payloads[stack[stackSize - 1]] |= (lastParent & 255);
						stackSize--;
					}
					if (stackSize > 0) {
						payloads[stack[stackSize - 1]] |= ((lastRight & 255) << 16);
					}

					if (nextTokenType == SPACE) {
						state = SPACE_STATE;
						prev.expectedToken = OPEN_B | CLOSE_B;
					} else if (nextTokenType == OPEN_B) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B;
					} else if (nextTokenType == CLOSE_B) {
						state = CLOSE_STATE;
						prev.expectedToken = CLOSE_B;
					} else {
						state = ERROR_STATE;
					}
					prev.effPrevState = CLOSE_STATE;
				} else {
					state = ERROR_STATE;
				}
				numberOfUnmatchedOpens--;
				break;
			case SPACE_STATE:
				if (nextTokenType == TEXT) {
					state = TEXT_STATE;
					prevTextPos = i;
					if ((prev.expectedToken & SPACE) == SPACE) {
						prev.effPrevState = SPACE_STATE;
						prev.expectedToken = TEXT;
					} else if (!(prev.effPrevState == OPEN_STATE)) {
						state = ERROR_STATE;
					}
				} else if (nextTokenType == OPEN_B) {
					state = OPEN_STATE;
					if ((prev.expectedToken & OPEN_B) == OPEN_B) {
						prev.expectedToken = OPEN_B;
					} else {
						state = ERROR_STATE;
					}
				} else if (nextTokenType == CLOSE_B) {
					state = CLOSE_STATE;
					if ((prev.expectedToken & CLOSE_B) == CLOSE_B) {
						prev.expectedToken = CLOSE_B;
					} else {
						state = ERROR_STATE;
					}
				} else {
					state = ERROR_STATE;
				}
				break;
			case TEXT_STATE:
				currentPos++;
				if (prev.effPrevState == OPEN_STATE) {
					noteText(positions, prevTextPos, lastRight,
							numberOfUnmatchedOpens - 1);
					if (nextTokenType == SPACE) {
						state = SPACE_STATE;
						prev.expectedToken = SPACE | OPEN_B;
					} else if (nextTokenType == OPEN_B) {
						state = OPEN_STATE;
						prev.expectedToken = OPEN_B;
					} else {
						state = ERROR_STATE;
					}
				} else if (prev.effPrevState == SPACE_STATE) {
					lastParent++;
					// here depth is = numberOfUnmatchedOpens and not
					// numberOfUnmatchedOpens - 1
					noteText(positions, prevTextPos, lastRight, lastRight + 1,
							lastParent, numberOfUnmatchedOpens);
					lastRight++;
					payloads[stack[stackSize - 1]] |= (lastRight << 16);
					prev.expectedToken = CLOSE_B;
					if (nextTokenType == SPACE) {
						state = SPACE_STATE;
					} else if (nextTokenType == CLOSE_B) {
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
				// if its looping in the close state.. then we'll have to
				// update the stack
				int depthOfInterest = numberOfUnmatchedOpens;
				lastParent++;
				while (stackSize > 0
						&& depth(payloads[stack[stackSize - 1]]) == depthOfInterest) {
					payloads[stack[stackSize - 1]] |= (lastParent & 255);
					stackSize--;
				}
				if (stackSize > 0) {
					payloads[stack[stackSize - 1]] |= ((lastRight & 255) << 16);
				}
			}
			numberOfUnmatchedOpens--;
		} else if (!(state == SPACE_STATE)) {
			throw new ParseException("Cannot parse sentence " + sentence);
		}

		if (stackSize != 1)
			throw new ParseException("Cannot parse sentence " + sentence
					+ ". Brackets might be missing.");
		prev = null;
	}

	private int depth(int payload) {
		return (payload >>> 8) & 255;
	}

	private void noteText(int[] positions, int i, int left, int right,
			int parent, int depth) {
		noteText(positions, i, left, depth);
		payloads[currentPos] |= ((right & 255) << 16);
		payloads[currentPos - 1] |= ((right & 255) << 16);
		payloads[currentPos] |= (parent & 255);
		stackSize--;
	}

	private void noteText(int[] positions, int i, int left, int depth) {
		try {
			textPositions[2 * currentPos] = positions[i];
			textPositions[2 * currentPos + 1] = positions[i + 1];
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldTextPos = textPositions;
			textPositions = new int[oldTextPos.length + 256];
			System
					.arraycopy(oldTextPos, 0, textPositions, 0,
							oldTextPos.length);
			textPositions[2 * currentPos] = positions[i];
			textPositions[2 * currentPos + 1] = positions[i + 1];
		}
		try {
			stack[stackSize++] = currentPos;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldStack = stack;
			stack = new int[oldStack.length + 128];
			System.arraycopy(oldStack, 0, stack, 0, oldStack.length);
			// note that the increment would have been performed before the
			// exception is thrown
			stack[stackSize] = currentPos;
		}
		int payload = 0;
		payload |= (left & 255);
		payload = payload << 24;
		payload |= ((depth & 255) << 8);
		try {
			payloads[currentPos] = payload;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] oldPayloads = payloads;
			payloads = new int[oldPayloads.length + 128];
			System.arraycopy(oldPayloads, 0, payloads, 0, oldPayloads.length);
			payloads[currentPos] = payload;
		}
	}

	private int getTokenType(char c) {
		switch (c) {
		case '(':
			return OPEN_B;
		case ')':
			return CLOSE_B;
		case ' ':
			return SPACE;
		}
		return TEXT;
	}

	public Token next(Token token) {
		if (currentPos == 0) return null;
		if (tokenPos <= currentPos) {
			token.setTermBuffer(sentence, textPositions[2 * tokenPos],
					textPositions[2 * tokenPos + 1]
							- textPositions[2 * tokenPos]);
			Payload p = new Payload();
			byte[] b = new byte[4];
			b[0] = (byte) ((payloads[tokenPos] >>> 16) & 255);
			b[1] = (byte) ((payloads[tokenPos] >>> 24) & 255);
			b[2] = (byte) ((payloads[tokenPos] >>> 8) & 255);
			b[3] = (byte) (payloads[tokenPos] & 255);
			p.setData(b);
			token.setPayload(p);
			tokenPos++;
			return token;
		}
		return null;
	}
}
