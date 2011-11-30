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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.index.Payload;

import au.edu.unimelb.csse.bit.ByteOps;

public class JsonSentenceParser {
	Pattern namePattern = Pattern.compile("\\\"n\\\":\\\"[^\\s]*");
	Matcher nameMatcher = namePattern.matcher("");
	Pattern indexPattern = Pattern.compile("\\\"i\\\":\\\"[^\\s]*");
	Matcher indexMatcher = indexPattern.matcher("");
	private static final int BEFORE_CONST = 5;
	private static final int AFTER_CONST = 2;
	private byte[] buffer = new byte[4];
	private int[] intbuffer = new int[4];
	private boolean compressPayload = false;
	private String jsonSentence;
	private ByteOps byteOps = new ByteOps();

	public JsonSentenceParser(boolean compressPayload) {
		this.compressPayload = compressPayload;
	}

	public void parse(String jsonSentence) {
		this.jsonSentence = jsonSentence;
		nameMatcher.reset(jsonSentence);
		indexMatcher.reset(jsonSentence);
	}

	public Token next(Token token) {
		boolean nameFound = nameMatcher.find();
		boolean indexFound = indexMatcher.find();
		if (nameFound && indexFound) {
			final int nstart = nameMatcher.start();
			final int nend = nameMatcher.end();
			final int indexOfEscapedQuotes = jsonSentence.indexOf("\\\"", nstart
					+ BEFORE_CONST);
			if (indexOfEscapedQuotes != -1
					&& indexOfEscapedQuotes < nend - AFTER_CONST) {
				String str = jsonSentence.substring(nstart + BEFORE_CONST, nend
						- AFTER_CONST);
				str = str.replace("\\\"", "\"");
				token.setTermBuffer(str);
			} else {
				token.setTermBuffer(jsonSentence, nstart + BEFORE_CONST,
						nameMatcher.end() - AFTER_CONST - nstart - BEFORE_CONST);
			}
			String index = jsonSentence.substring(indexMatcher.start()
					+ BEFORE_CONST, indexMatcher.end() - AFTER_CONST);
			String[] split = index.split("_");
			for (int i = 0; i < 4; i++) {
				intbuffer[i] = Integer.parseInt(split[i]);
				if (intbuffer[i] > 255) {
					throw new OverflowException(
							"Exceeded payload size for element " + i + " = "
									+ intbuffer[i]);
				}
				buffer[i] = (byte) (intbuffer[i] & 255);
			}
			if (compressPayload) {
				byte[] bytes = new byte[8];
				try {
					token.setPayload(getVarDiffPayload(bytes));
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new OverflowException(
							"Exceeded payload size for element ");

				}
			} else {
				token.setPayload(new Payload(buffer.clone()));
			}

			// if (compressPayload) {
			// byte[] bytes = new byte[8];
			// try {
			// token.setPayload(getVarDiffPayload(bytes));
			// }catch (ArrayIndexOutOfBoundsException e) {
			// bytes = new byte[16];
			// try {
			// token.setPayload(getVarDiffPayload(bytes));
			// } catch(ArrayIndexOutOfBoundsException ee) {
			// throw new
			// OverflowException("Exceeded payload size for element ");
			// }
			// }
			//				
			// } else {
			// for (int i = 0; i < 4; i++) {
			// if(intbuffer[i] > 255) {
			// throw new OverflowException("Exceeded payload size for element "
			// + i + " = " + intbuffer[i]);
			// }
			// buffer[i] = (byte) (intbuffer[i] & 255);
			// }
			// token.setPayload(new Payload(buffer.clone()));
			// }
			return token;
		}
		return null;
	}

	private Payload getVarDiffPayload(byte[] bytes) {
		byte[] finalbytes = bytes;
		ByteOps.PosMid pm = byteOps.int4ToDiffVarBytes(intbuffer, bytes);
		if (pm.position < 8) {
			int numberOfBytes;
			if (pm.mid) {
				numberOfBytes = pm.position + 1;
			} else {
				numberOfBytes = pm.position;
			}
			finalbytes = new byte[numberOfBytes];
			System.arraycopy(bytes, 0, finalbytes, 0, numberOfBytes);
		}
		final Payload payload = new Payload(finalbytes);
		return payload;
	}

}
