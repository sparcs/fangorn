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

import junit.framework.TestCase;

public class FastStringParserTest extends TestCase {
	FastStringParser parser = new FastStringParser();
	Token token = new Token();
	
	public void testUnreal2TermSentence() throws Exception {
		parser.reset("(AA BB)");
		
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
	}

	public void testUnreal2TermSentenceWithSpaces() throws Exception {
		parser.reset("( AA BB)");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
		
		parser.reset("(AA  BB)");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
		
		parser.reset("(AA BB )");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
		
		parser.reset("(AA BB) ");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);

		parser.reset(" ( AA   BB ) ");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
	}

	private void assertNextToken(final String string, final int left,
			final int right, final int depth, final int parent) {
		token = parser.next(token);
		assertNotNull(token);
		assertEquals(string, token.term());
		assertPayload(left,right,depth,parent);
	}
	
	public void test1ChildRootTree() throws Exception {
		parser.reset("(AA(BB CC))");

		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 2);
		assertNextToken("CC", 0, 1, 2, 1);
	}

	public void test1ChildRootTreeWithSpaces() throws Exception {
		parser.reset(" ( AA (  BB   CC ) )  ");

		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 2);
		assertNextToken("CC", 0, 1, 2, 1);
	}

	public void test2ChildrenRootTree() throws Exception {
		parser.reset("(AA(BB CC)(D EEE))");

		assertNextToken("AA", 0, 2, 0, 0);
		assertNextToken("BB", 0, 1, 1, 3);
		assertNextToken("CC", 0, 1, 2, 1);
		assertNextToken("D", 1, 2, 1, 3);
		assertNextToken("EEE", 1, 2, 2, 2);
	}
	
	public void test3ChildrenRootTree() throws Exception {
		parser.reset("(AA(BB CC)(D EEE)(FFF G))");

		assertNextToken("AA", 0, 3, 0, 0);
		assertNextToken("BB", 0, 1, 1, 4);
		assertNextToken("CC", 0, 1, 2, 1);
		assertNextToken("D", 1, 2, 1, 4);
		assertNextToken("EEE", 1, 2, 2, 2);
		assertNextToken("FFF", 2, 3, 1, 4);
		assertNextToken("G", 2, 3, 2, 3);
	}
	
	public void testNestedTree() throws Exception {
		parser.reset("(A(B(C D)(E F))(G(H(I J))(K L)))");

		assertNextToken("A", 0, 4, 0, 0);
		assertNextToken("B", 0, 2, 1, 8);
		assertNextToken("C", 0, 1, 2, 3);
		assertNextToken("D", 0, 1, 3, 1);
		assertNextToken("E", 1, 2, 2, 3);
		assertNextToken("F", 1, 2, 3, 2);
		assertNextToken("G", 2, 4, 1, 8);
		assertNextToken("H", 2, 3, 2, 7);
		assertNextToken("I", 2, 3, 3, 5);
		assertNextToken("J", 2, 3, 4, 4);
		assertNextToken("K", 3, 4, 2, 7);
		assertNextToken("L", 3, 4, 3, 6);
	}

	public void testNestedTreeWithSpace() throws Exception {
		parser.reset(" (  A ( B ( C D ) ( E F ) )  ( G ( H ( I  J ) ) ( K  L ) ) ) ");

		assertNextToken("A", 0, 4, 0, 0);
		assertNextToken("B", 0, 2, 1, 8);
		assertNextToken("C", 0, 1, 2, 3);
		assertNextToken("D", 0, 1, 3, 1);
		assertNextToken("E", 1, 2, 2, 3);
		assertNextToken("F", 1, 2, 3, 2);
		assertNextToken("G", 2, 4, 1, 8);
		assertNextToken("H", 2, 3, 2, 7);
		assertNextToken("I", 2, 3, 3, 5);
		assertNextToken("J", 2, 3, 4, 4);
		assertNextToken("K", 3, 4, 2, 7);
		assertNextToken("L", 3, 4, 3, 6);
	}
	
	public void testIncorrectSentencesAreIdentified() throws Exception {
		assertThrowsError("(A)");
		assertThrowsError("(A )");
		assertThrowsError("(A B");
		assertThrowsError("(A(B(C D))");
		assertThrowsError("(A B C)");
		assertThrowsError("(A (B C D))");
		assertThrowsError("(A (B C)))");
		assertThrowsError("A");
	}

	private void assertThrowsError(final String s) {
		try {
			parser.reset(s);
			fail();
		} catch(ParseException e) {
			
		}
	}

	private void assertPayload(int left, int right, int depth, int parent) {
		final Payload payload = token.getPayload();
		assertEquals(left, payload.byteAt(1));
		assertEquals(right, payload.byteAt(0));
		assertEquals(depth, payload.byteAt(2));
		assertEquals(parent, payload.byteAt(3));
	}
}
