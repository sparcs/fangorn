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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.index.Payload;

public class TreebankSentenceTokenizer extends Tokenizer {
	protected static final int BUFFER_SIZE = 512;
	private final char[] ioBuffer = new char[BUFFER_SIZE];
	private Tknzr tokenizer;
	private int read = -1;

	public TreebankSentenceTokenizer(Reader reader) {
		super(reader);
	}

	@Override
	public Token next(Token reusableToken) throws IOException {
		Token token = reusableToken;
		if (tokenizer() != null) {
			Token t = tokenizer().next(token);
			if (t != null) {
				return t;
			}
		}
		char[] sent = new char[] {};
		do {
			read = input.read(ioBuffer);
			if (read > 0)
				sent = add(sent, ioBuffer, read);
		} while (read != -1);
		if (sent.length == 0) {
			return null;
		}
		if (tokenizer() == null) {
			tokenizer = new Tknzr(sent);
		} else {
			tokenizer().reset(sent);
		}
		return tokenizer().next(token);

	}

	private char[] add(char[] sent, char[] newChunk, int newLength) {
		char oldData[] = sent;
		int oldLength = sent.length;
		int newCapacity = sent.length + newLength;
		sent = new char[newCapacity];
		System.arraycopy(oldData, 0, sent, 0, oldData.length);
		System.arraycopy(newChunk, 0, sent, oldLength, newLength);
		return sent;
	}

	Tknzr tokenizer() {
		return tokenizer;
	}

	@Override
	public void reset(Reader input) throws IOException {
		super.reset(input);
		if (tokenizer != null) {
			tokenizer.reset(new char[] {});
		}
		read = -1;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	public class Tknzr {
		protected char[] buffer;
		private List<Integer> cOpen = new ArrayList<Integer>();
		private List<Integer> cSpace = new ArrayList<Integer>();
		private List<Integer> cClose = new ArrayList<Integer>();
		private List<XY> parentNumberingList = new ArrayList<XY>();
		private boolean spacePending = false;
		private int lastReadPosition = 0;
		private Map<Integer, Integer> leftIndex = new HashMap<Integer, Integer>();
		private Stack<Integer> openPositions = new Stack<Integer>();
		private int lastWordBeginPos = 0;
		private boolean encounteredOpen = false;
		private int maxParentNumber = -1;

		public Tknzr(char[] buffer, int length) {
			this.buffer = new char[length];
			System.arraycopy(buffer, 0, this.buffer, 0, length);
		}

		public Tknzr(char[] sentence) {
			this(sentence, sentence.length);
		}

		public Token next(final Token reusableToken) throws IOException {
			assert reusableToken != null;
			reusableToken.clear();
			if (spacePending) {
				return setReusableTokenFromLocal(reusableToken,
						processSpace(lastWordBeginPos));
			}
			int i = lastReadPosition;
			boolean closeFound = false;
			while (i < buffer.length) {
				char c = buffer[i];
				if ('(' == c) {
					if (encounteredOpen) {
						openPositions.add(cOpen.size() - 1);
					}
					cOpen.add(i);
					encounteredOpen = true;
				} else if (Character.isWhitespace(c)) {
					cSpace.add(i);
					lastWordBeginPos = cOpen.get(cOpen.size() - 1);
					spacePending = true;
					encounteredOpen = false;
				} else if (')' == c) {
					cClose.add(i);
					closeFound = true;
					encounteredOpen = false;
					break;
				}
				i++;
			}
			lastReadPosition = i;
			if (closeFound) {
				lastReadPosition++;
				return setReusableTokenFromLocal(reusableToken, processClose());
			}
			return null;
		}

		private Token setReusableTokenFromLocal(final Token reusableToken,
				TreeToken local) {
			reusableToken.setTermBuffer(local.label);
			reusableToken.setPayload(local.getPayload());
			return reusableToken;
		}

		private TreeToken processClose() {
			if (spacePending) {
				int end = cClose.get(cClose.size() - 1);
				int start = cSpace.get(cSpace.size() - 1);
				TreeToken t = new TreeToken();
				char[] subset = new char[end - start - 1];
				System.arraycopy(buffer, start + 1, subset, 0, end - start - 1);
				t.label = String.valueOf(subset);
				t.h = cOpen.size() - cClose.size() + 1;
				t.r = cSpace.size();
				t.l = t.r - 1;
				t.p = parentNumber(t.h);
				return t;
			}
			int startPos = openPositions.pop();
			int start = cOpen.get(startPos);
			int end = cOpen.get(startPos + 1);
			TreeToken t = new TreeToken();
			char[] subset = new char[end - start - 1];
			System.arraycopy(buffer, start + 1, subset, 0, end - start - 1);
			t.label = new String(subset);
			t.h = cOpen.size() - cClose.size();
			t.r = cSpace.size();
			int j = startPos + 1;
			Integer left = leftIndex.get(cOpen.get(j));
			while (j < cOpen.size() && left == null) {
				j++;
				left = leftIndex.get(cOpen.get(j));
			}
			t.l = left;
			t.p = parentNumber(t.h);
			return t;
		}

		private int parentNumber(int h) {
			if (parentNumberingList.size() == 0) {
				return newNumber(h);
			}
			int i = parentNumberingList.size() - 1;
			while (i >= 0) {
				XY last = parentNumberingList.get(i);
				if (last.xEqual(h)) {
					final int parent = last.getY();
					parentNumberingList.add(new XY(h, parent));
					return parent;
				} else if (last.xGreater(h)) {
					i = i - 1;
				} else {
					return newNumber(h);
				}
			}
			return newNumber(h);
		}

		private int newNumber(int h) {
			maxParentNumber++;
			parentNumberingList.add(new XY(h, maxParentNumber));
			return maxParentNumber;
		}

		private TreeToken processSpace(int pos) {
			int end = cSpace.get(cSpace.size() - 1);
			int start = cOpen.get(cOpen.size() - 1);
			TreeToken t = new TreeToken();
			char[] subset = new char[end - start - 1];
			System.arraycopy(buffer, start + 1, subset, 0, end - start - 1);
			t.label = String.valueOf(subset);
			t.h = cOpen.size() - cClose.size();
			t.r = cSpace.size();
			t.l = t.r - 1;
			t.p = parentNumber(t.h);
			leftIndex.put(pos, t.l);
			spacePending = false;
			return t;
		}

		public void reset(char[] input) throws IOException {
			reset(input, input.length);
		}

		public void reset(char[] buffer, int length) throws IOException {
			this.buffer = new char[length];
			System.arraycopy(buffer, 0, this.buffer, 0, length);
			cOpen.clear();
			cSpace.clear();
			cClose.clear();
			parentNumberingList.clear();
			spacePending = false;
			lastReadPosition = 0;
			leftIndex.clear();
			openPositions.clear();
			lastWordBeginPos = 0;
			encounteredOpen = false;
			maxParentNumber = -1;
		}

	}

	static class XY {
		private final int x;
		private final int y;

		XY(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public boolean xEqual(int h) {
			return x == h;
		}

		public boolean xGreater(int otherX) {
			return x > otherX;
		}

		public int getY() {
			return y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof XY))
				return false;
			XY other = (XY) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

	}

	static class TreeToken {
		public int h;
		public int l;
		public int r;
		public String label;
		public int p;

		/**
		 * This is where the order of the elements in the current token decides
		 * the position within the position increment field in lucene token
		 * 
		 * @return
		 */
		public Payload getPayload() {
			// if (!(r < 256 && l < 256) && r < 1024 && l < 1024 && p < 512)
			// {
			// byte[] v = new byte[] { (byte) (r & 255), (byte) (l & 255),
			// (byte) (h & 255), (byte) (p & 255), (byte) (((r & 768) >>> 4)
			// |
			// ((l & 768) >>> 6) | ((p & 256) >>> 8)) };
			// }
			if (!(h < 256 && l < 256 && r < 256 && p < 256)) {
				throw new OverflowException(
						"Exceeded allocated space for payload l=" + l + " r="
								+ r + " p=" + p + " h=" + h);
			}

			byte[] v = new byte[] { (byte) (r & 255), (byte) (l & 255),
					(byte) (h & 255), (byte) (p & 255) };

			return new Payload(v);
		}

	}

}
