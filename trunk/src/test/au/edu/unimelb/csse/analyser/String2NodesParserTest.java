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
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.index.Payload;

import au.edu.unimelb.csse.ParseException;

public class String2NodesParserTest extends TestCase {
	public void testSimpleSentence() throws Exception {
		String2NodesParser parser = new String2NodesParser();
		Node r = parser.parse("(S(NP(AT a)(NN man))(VBP ran)(. .))");
		assertNode(r, "S", 3, 0, 4, 0, 6);

		Node np = r.children.get(0);
		assertNode(np, "NP", 2, 0, 2, 1, 5);

		Node at = np.children.get(0);
		assertNode(at, "AT", 1, 0, 1, 2, 2);

		Node a = at.children.get(0);
		assertNode(a, "a", 0, 0, 1, 3, 0);

		Node nn = np.children.get(1);
		assertNode(nn, "NN", 1, 1, 2, 2, 2);

		Node man = nn.children.get(0);
		assertNode(man, "man", 0, 1, 2, 3, 1);

		Node vbp = r.children.get(1);
		assertNode(vbp, "VBP", 1, 2, 3, 1, 5);

		Node ran = vbp.children.get(0);
		assertNode(ran, "ran", 0, 2, 3, 2, 3);

		Node dotPOS = r.children.get(2);
		assertNode(dotPOS, ".", 1, 3, 4, 1, 5);

		Node dot = dotPOS.children.get(0);
		assertNode(dot, ".", 0, 3, 4, 2, 4);
	}

	public void test10SentsFromWiki() throws ParseException, IOException {
		String2NodesParser parser = new String2NodesParser();
		//we wont have this case ever because the earlier step removes spaces..
		//the newer version of string2nodesparser handles spaces more elegantly
		// String sent1 =
		// "( NP ( NP ( NNP David ) (NNP Arthur) (NNP Wales)) (, ,) (NP (ADJP (JJ a.k.a.) (SBAR (S (NP (NP (NP (NNP David) (NNP Wales))) (CC or) (NP (NP (NNP David) (NNP Art) (NNP Wales)) (PRN (-LRB- -LRB-) (VP (VBN born) (NP (NP (CD 6) (NNP February) (CD 1964)) (, ,) (NP (NP (NNP Sydney))))) (-RRB- -RRB-)))) (VP (VBZ is) (NP (NP (DT an) (JJ Australian) (NN entrepreneur) (CC and) (NN artist))) (ADJP (JJS best) (VBN known) (PP (IN for) (S (VP (VBG creating) (NP (NP (JJ satirical) (NN cult) (NN figure))))))))))) (NNP Guru) (NNP Adrian)) (. .))";
		String sent1 = "(NP(NP(NNP David)(NNP Arthur)(NNP Wales))(, ,)(NP(ADJP(JJ a.k.a.)(SBAR(S(NP(NP(NP(NNP David)(NNP Wales)))(CC or)(NP(NP(NNP David)(NNP Art)(NNP Wales))(PRN(-LRB- -LRB-)(VP(VBN born)(NP(NP(CD 6)(NNP February)(CD 1964))(, ,)(NP(NP(NNP Sydney)))))(-RRB- -RRB-))))(VP(VBZ is)(NP(NP(DT an)(JJ Australian)(NN entrepreneur)(CC and)(NN artist)))(ADJP(JJS best)(VBN known)(PP(IN for)(S(VP(VBG creating)(NP(NP(JJ satirical)(NN cult)(NN figure)))))))))))(NNP Guru)(NNP Adrian))(. .))";
		Node parsed = parser.parse(sent1);
		assertNotNull(parsed);

		assertNode(parsed, "NP", 4, 0, 35, 0, 61);

		Node first = parsed.children.get(0);
		assertNode(first, "NP", 3, 0, 3, 1, 60);
		
		assertNode(first.children.get(0), "NNP", 1, 0, 1, 2, 3);
		assertNode(first.children.get(0).children.get(0), "David", 0, 0, 1, 3, 0);
		
		assertNode(first.children.get(1), "NNP", 1, 1, 2, 2, 3);
		assertNode(first.children.get(1).children.get(0), "Arthur", 0, 1, 2, 3, 1);

		assertNode(first.children.get(2), "NNP", 1, 2, 3, 2, 3);
		assertNode(first.children.get(2).children.get(0), "Wales", 0, 2, 3, 3, 2);

		assertNode(parsed.children.get(1), ",", 1, 3, 4, 1, 60);
		
		assertEquals(96, parsed.totalNumberOfNodes());
		
		Token token = new Token();
		for (int i = 0; i < 96; i++) {
			assertNotNull(parser.next(token));
		}
		
		assertNull(parser.next(token));
		
		String sent2 = "(S(PP(IN During)(NP(DT the)(CD 1980s)))(NP(PRP he))(VP(VBD was)(NP(NP(DT a)(JJ frequent)(NN contributor))(PP(TO to)(NP(JJ Australian)(NN radio)(NN station)(NNP Triple)(NNP Jay))))(, ,)(S(VP(VBG providing)(NP(NP(NP(NN commentary))(PP(IN on)(NP(NP(JJ pop-cultural)(NNS issues))(, ,)(PP(VBG including)(NP(NP(DT a)(JJ live)(NN report))(PP(IN from)(NP(NNP Berlin)))(SBAR(IN as)(S(NP(DT the)(NNP Berlin)(NNP Wall))(VP(VBD fell)))))))))(, ,)(CC and)(NP(NP(DT a)(JJ comic)(NN strip))(VP(VBG featuring)(NP(NNP Guru)(NNP Adrian))(PP(IN for)(NP(NP(NP(DT the)(NN station)(POS 's))(NN fanzine))(, ,)(NP(NNP Alan))))))))))(. .))";
		
		parsed = parser.parse(sent2);
		
		assertEquals(131, parsed.totalNumberOfNodes());
		
		for (int i = 0; i < 131; i++) {
			assertNotNull(parser.next(token));
		}

		assertNull(parser.next(token));
		
		
		String sent3 = "(S(NP(NNP Wales))(VP(VP(VBD moved)(PP(TO to)(NP(NNP New)(NNP York)))(S(VP(TO to)(VP(VB become)(NP(NP(DT a)(NN painter))(PP(IN in)(NP(CD 1989))))))))(CC and)(VP(VBD spent)(NP(DT the)(CD 90s)(NN showing))(PP(IN at)(NP(NP(JJ various)(NNP Manhattan))(CC and)(NP(NP(JJ Australian)(NNS galleries))(, ,)(PP(VBG including)(NP(NNP Roslyn)(NNP Oxley)(CD 9)(CC and)(NNP Sherman)(NNPS Galleries))))))))(. .))";
		
		parsed = parser.parse(sent3);
		
		assertEquals(84, parsed.totalNumberOfNodes());
		
		for (int i = 0; i < 84; i++) {
			assertNotNull(parser.next(token));
		}

		assertNull(parser.next(token));
		
		
		String sent4 = "NP(NP(CD Eight)(NNS campuses))(PP(IN outside)(NP(DT the)(NNP Guadalajara)(NNP Metropolitan)(NNP Area)))(PP(IN within)(NP(NP(DT the)(NNP State))(PP(IN of)(NP(NNP Jalisco)))))(. .)";
		
		parsed = parser.parse(sent4);
	}

	public void testReturnsNodesInOrder() throws ParseException, IOException {
		String2NodesParser tokenizer = new String2NodesParser();
		Node n = tokenizer.parse("(A(B(C D)(E F))(G H))");
		assertEquals("A", n.label());
		Token token = new Token();
		Token ret;
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("A", token.term());
		assertPayload(token, 3, 0, 0, 5);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("B", token.term());
		assertPayload(token, 2, 0, 1, 4);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("C", token.term());
		assertPayload(token, 1, 0, 2, 2);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("D", token.term());
		assertPayload(token, 1, 0, 3, 0);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("E", token.term());
		assertPayload(token, 2, 1, 2, 2);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("F", token.term());
		assertPayload(token, 2, 1, 3, 1);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("G", token.term());
		assertPayload(token, 3, 2, 1, 4);
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("H", token.term());
		assertPayload(token, 3, 2, 2, 3);
		ret = tokenizer.next(token);
		assertNull(ret);
		
		tokenizer.parse("(K(L M)(N O))");
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("K", token.term());
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("L", token.term());
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("M", token.term());
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("N", token.term());
		ret = tokenizer.next(token);
		assertNotNull(ret);
		assertEquals("O", token.term());
		ret = tokenizer.next(token);
		assertNull(ret);
	}

	private void assertPayload(Token token, int right, int left, int depth,
			int parent) {
		Payload payload = token.getPayload();
		assertEquals(right, payload.byteAt(0));
		assertEquals(left, payload.byteAt(1));
		assertEquals(depth, payload.byteAt(2));
		assertEquals(parent, payload.byteAt(3));
	}
	private void assertNode(Node node, final String name, final int noc,
			final int left, final int right, final int depth, final int parent) {
		assertNotNull(node);
		assertEquals(name, node.name);
		assertEquals(noc, node.children.size());
		assertEquals(depth, node.depth);
		assertEquals(left, node.left);
		assertEquals(right, node.right);
		assertEquals(parent, node.parentId);
	}
}
