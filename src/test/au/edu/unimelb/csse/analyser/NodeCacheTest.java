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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class NodeCacheTest extends TestCase {
	public void testReusesNodesWhileIndexing() throws Exception {
		String[] sents = new String[]{"(A(B C)(D(E F)))", "(A(B(C D)))", "(A(B C)(D(E(F(G H)))))", "(A(B C))"};
		String[] jsonSents = new String[sents.length];
		String2NodesParser parser  = new String2NodesParser();
		assertEquals(0, NodeCache.cacheSize());
		int[] expectedCounts = new int[]{0, 2, 0, 5};
		//First sent: 6 nodes are used but they are not returned until the next sentence is read. 
		//Hence the cache still returns a size of 0
		//Second sent: 6 nodes are returned back but the new sentence contains 4 nodes
		//6 - 4 = 2
		//Third sent: 4 nodes are returned back but the new sentence contains 8 nodes
		//size shows 0 again
		//Fourth sent: 8 nodes are returned back but the new sentence contains 3 nodes
		//8 - 3 = 5

		for (int i = 0; i < sents.length; i++) {
			jsonSents[i] = parser.parse(sents[i]).asJSONString();
			assertEquals(expectedCounts[i], NodeCache.cacheSize());
		}
		Analyzer analyser = new NodeTreebankAnalyser(false);
		RAMDirectory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, analyser, true, IndexWriter.MaxFieldLength.UNLIMITED);
		
		Document d = new Document();
		d.add(new Field("sent", jsonSents[0], Field.Store.NO,
				Field.Index.ANALYZED_NO_NORMS,
				Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);
		//No change to Node cache
		assertEquals(5, NodeCache.cacheSize());
		
		d = new Document();
		d.add(new Field("sent", jsonSents[1], Field.Store.NO,
				Field.Index.ANALYZED_NO_NORMS,
				Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);
		//No change to Node cache
		assertEquals(5, NodeCache.cacheSize());

		d = new Document();
		d.add(new Field("sent", jsonSents[2], Field.Store.NO,
				Field.Index.ANALYZED_NO_NORMS,
				Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);
		//No change to Node cache
		assertEquals(5, NodeCache.cacheSize());

		d = new Document();
		d.add(new Field("sent", jsonSents[3], Field.Store.NO,
				Field.Index.ANALYZED_NO_NORMS,
				Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);
		//No change to Node cache
		assertEquals(5, NodeCache.cacheSize());

	}
}
