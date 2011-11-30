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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

public class SentenceTokenizerTest extends TestCase {
	public void testWithStringReader() {
		StringReader reader = new StringReader("((S1 (S (NP (NNP First)) (VP (VBD aired) (NP (NNP September) (CD 13) (, ,) (CD 2006))) (. .))))\n"+
"((S1 (FRAG (NP (NN Episode) (CD 11)) (: :) (FRAG (WHNP (WP What)) (NP (DT the) (NNP ELLE)) (. ?)) (. .))))\n" +
"((S1 (S (NP (DT The) (JJ remaining) (NNS designer(S1 (S (NP (DT The) (JJ remaining) (NNS designers)) (VP (AUX are) (VP (VBN asked) (S (VP (TO to) (VP (VB create) (NP (NP (DT an) (NN outfit)) (SBAR (WHNP (WDT that)) (S (VP (VBZ expresses) (S (VP (POS ') (NP (NP (PRP$ their) (JJ specific) (NN point)) (PP (IN of) (NP (NN view)))) (PP (IN as) (NP (NP (NP (DT a) (NN designer) (POS ')) (PP (IN in) (NP (RB just) (CD two) (NNS days)))) (CC and) (NP (QP ($ $) (CD 250) (CD USD)))))))))))))))) (. .))))\n"+
"((S1 (S (NP (PRP They)) (VP (MD must) (VP (AUX have) (NP (QP (IN at) (JJS least) (CD one)) (VBG flashing) (NNP amber) (NN light)) (PP (IN on) (NP (PRP them))))) (. .))))");
		
		SentenceTokenizer tokenizer = new SentenceTokenizer(new BufferedReader(reader));
		try {
			SentenceAndMetaData snm = tokenizer.next();
			assertNotNull(snm);
			assertEquals("(S1(S(NP(NNP First))(VP(VBD aired)(NP(NNP September)(CD 13)(, ,)(CD 2006)))(. .)))", snm.sentence());
		} catch (IOException e) {
			fail();
		}
		
		try {
			SentenceAndMetaData snm = tokenizer.next();
			assertNotNull(snm);
			assertEquals("(S1(FRAG(NP(NN Episode)(CD 11))(: :)(FRAG(WHNP(WP What))(NP(DT the)(NNP ELLE))(. ?))(. .)))", snm.sentence());
		} catch (IOException e) {
			fail();
		}
		//the third sentence is ignored
		try {
			SentenceAndMetaData snm = tokenizer.next();
			assertNotNull(snm);
			assertEquals("(S1(S(NP(PRP They))(VP(MD must)(VP(AUX have)(NP(QP(IN at)(JJS least)(CD one))(VBG flashing)(NNP amber)(NN light))(PP(IN on)(NP(PRP them)))))(. .)))", snm.sentence());
		} catch (IOException e) {
			fail();
		}
	}
}
