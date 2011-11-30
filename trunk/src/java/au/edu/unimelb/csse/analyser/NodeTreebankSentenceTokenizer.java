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

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;

public class NodeTreebankSentenceTokenizer extends Tokenizer {
	protected static final int BUFFER_SIZE = 512;
	private final char[] ioBuffer = new char[BUFFER_SIZE];
	private JsonSentenceParser elementTokenizer;
	private int read = -1;
	private static final String NONE = "";
	private boolean compressPayload = false;

	public NodeTreebankSentenceTokenizer(Reader reader, boolean compressPayload) {
		super(reader);
		this.compressPayload = compressPayload;
	}

	@Override
	public Token next(Token reusableToken) throws IOException {
		Token token = reusableToken;
		if (elementTokenizer() != null) {
			Token t = elementTokenizer().next(token);
			if (t != null) {
				return t;
			}
		}
		char[] sent = new char[] {};
		do {
			read = input.read(ioBuffer);
			if (read > 0) sent = add(sent, ioBuffer, read);
		} while (read != -1);
		if (sent.length == 0) {
			return null;
		}
		if (elementTokenizer() == null) {
			elementTokenizer = new JsonSentenceParser(compressPayload);
		} 
		elementTokenizer().parse(String.valueOf(sent));
		return elementTokenizer().next(token);

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

	JsonSentenceParser elementTokenizer() {
		return elementTokenizer;
	}

	@Override
	public void reset(Reader input) throws IOException {
		super.reset(input);
		if (elementTokenizer != null) {
			elementTokenizer.parse(NONE);
		}
		read = -1;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}
}
