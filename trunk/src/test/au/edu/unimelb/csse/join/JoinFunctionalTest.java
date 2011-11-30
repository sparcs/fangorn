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
package au.edu.unimelb.csse.join;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.analyser.FastStringAnalyser;
import au.edu.unimelb.csse.queryParser.QueryBuilder;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.DoNotUseJoinLogic;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class JoinFunctionalTest extends TestCase {
	/**
	 * This test is actually commented out.. to run the test.. match counting has to be enabled in JoinLogic
	 * @throws Exception
	 */
	public void testNumberOfCallsToMatch() throws Exception {
		String sent = "(NP" + "(NP" + "(DT The)" + "(NN year))" + "(NP"
				+ "(NP(CD 1956))" + "(PP" + "(IN in)"
				+ "(NP(JJ rugby)(NN union))" + ")" + ")" + "(. .)" + ")";
		Analyzer analyser = new FastStringAnalyser();
		RAMDirectory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, analyser, true,
				IndexWriter.MaxFieldLength.UNLIMITED);

		Document d = new Document();
		d
				.add(new Field("sent", sent, Field.Store.NO,
						Field.Index.ANALYZED_NO_NORMS,
						Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);

		writer.close();

		IndexSearcher searcher = new IndexSearcher(dir);
		assertNumberOfComparisons(searcher, "//NP//NP", TermJoinType.SIMPLE,
				false, 6);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.SIMPLE_WITH_FC, false, 1);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.EARLY_STOP, false, 2);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.EARLY_STOP_WITH_FC, false, 1);

		assertNumberOfComparisons(searcher, "//NP//NP", TermJoinType.SIMPLE,
				true, 6);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.SIMPLE_WITH_FC, true, 5);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.EARLY_STOP, true, 6);

		assertNumberOfComparisons(searcher, "//NP//NP",
				TermJoinType.EARLY_STOP_WITH_FC, true, 5);

		assertNumberOfComparisons(searcher, "//NP//NP//NP",
				TermJoinType.SIMPLE, false, 23);

		assertNumberOfComparisons(searcher, "//NP//NP//NP",
				TermJoinType.SIMPLE_WITH_FC, false, 10);

		assertNumberOfComparisons(searcher, "//NP//NP//NP",
				TermJoinType.EARLY_STOP, false, 10);

		assertNumberOfComparisons(searcher, "//NP//NP//NP",
				TermJoinType.EARLY_STOP_WITH_FC, false, 8);

	}

	public void testFilterjoin() throws Exception {
		String sent = "(NP" + "(NP" + "(DT The)" + "(NN year))" + "(NP"
				+ "(NP(CD 1956))" + "(PP" + "(IN in)"
				+ "(NP(JJ rugby)(NN union))" + ")" + ")" + "(. .)" + ")";
		Analyzer analyser = new FastStringAnalyser();
		RAMDirectory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, analyser, true,
				IndexWriter.MaxFieldLength.UNLIMITED);

		Document d = new Document();
		d
				.add(new Field("sent", sent, Field.Store.NO,
						Field.Index.ANALYZED_NO_NORMS,
						Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);

		writer.close();

		IndexSearcher searcher = new IndexSearcher(dir);

		boolean[] lookaheadOptions = new boolean[] { false, true };
		for (TermJoinType type : TermJoinType.values()) {
			for (boolean lookahead : lookaheadOptions) {
				QueryBuilder builder = new QueryBuilder("//PP[/IN AND /NP]");
				TreebankQuery query = builder.parse(type, lookahead);
				SimpleHitCollector hitCollector = new SimpleHitCollector(10);
				searcher.search(query, hitCollector);
				assertEquals(1, hitCollector.totalHits);
			}
		}
		
		QueryBuilder builder = new QueryBuilder("//PP[/IN AND /NP/JJ/rugby]");
		TreebankQuery query = builder.parse(TermJoinType.SIMPLE, true);
		SimpleHitCollector hitCollector = new SimpleHitCollector(10);
		searcher.search(query, hitCollector);
		assertEquals(1, hitCollector.totalHits);

	}

	private void assertNumberOfComparisons(IndexSearcher searcher,
			final String queryString, final TermJoinType joinType,
			final boolean useLookahead, final int numberOfComparisons)
			throws ParseException, IOException {
		QueryBuilder builder = new QueryBuilder(queryString);
		TreebankQuery query = builder.parse(joinType, useLookahead);
		int beforeTest = DoNotUseJoinLogic.getNumberOfComparisons();
		SimpleHitCollector hitCollector = new SimpleHitCollector(10);
		searcher.search(query, hitCollector);
		assertEquals(1, hitCollector.totalHits);
		int afterTest = DoNotUseJoinLogic.getNumberOfComparisons();
		// assertEquals(numberOfComparisons, afterTest - beforeTest);
	}
}
