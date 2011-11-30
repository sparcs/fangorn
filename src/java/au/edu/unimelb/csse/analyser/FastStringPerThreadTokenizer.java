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

import au.edu.unimelb.csse.ParseException;

public class FastStringPerThreadTokenizer extends Tokenizer {
	
	private final FastStringParser actualParser;
	private final char[] ioBuffer = new char[512];
	char[] sent = new char[512];

	public FastStringPerThreadTokenizer(Reader reader) {
		super(reader);
		actualParser = new FastStringParser();
	}
	
	@Override
	public void reset(Reader input) throws IOException {
		super.reset(input);
	}
	
	@Override
	public Token next(Token reusableToken) throws IOException {
		Token t = actualParser.next(reusableToken);
		if (t != null) return t;
		int readSoFar = 0;
		int read;
		do {
			read = input.read(ioBuffer);
			if (read > 0) {
				while (readSoFar + read > sent.length) {
					char[] oldSent = sent;
					sent = new char[sent.length + 512];
					System.arraycopy(oldSent, 0, sent, 0, readSoFar);
				}
				System.arraycopy(ioBuffer, 0, sent, readSoFar, read);
				readSoFar += read;
			}
		} while (read != -1);
		if (readSoFar == 0) {
			return null;
		}
		try {
			actualParser.reset(new String(sent, 0, readSoFar));
		} catch (ParseException e) {
			return null;
		}
		return actualParser.next(reusableToken);
	}
	
}
