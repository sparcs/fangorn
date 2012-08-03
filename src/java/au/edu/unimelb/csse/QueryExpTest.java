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
package au.edu.unimelb.csse;

import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.queryParser.QueryBuilder;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreeTerm;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class QueryExpTest {
	private String indexPath;
	private String query;
	private TermJoinType joinType;
	private boolean useLookahead;
	private int times;

	public QueryExpTest(String indexPath, String query, String termJoinType, String useLookahead) {
		this(indexPath, query, termJoinType, useLookahead, 5);
	}

	public QueryExpTest(String indexPath, String query, String termJoinType, String useLookahead, int times) {
		this.indexPath = indexPath;
		this.query = query;
		this.joinType = TermJoinType.valueOf(termJoinType);
		this.useLookahead = Boolean.parseBoolean(useLookahead);
		this.times = times;
	}

	public static void main(String[] args) throws CorruptIndexException,
			IOException, ParseException {
		if (args.length != 4 && args.length != 5) {
			System.out.println("Usage: java " + QueryExpTest.class.getName()
					+ " <index_path> <query> <join_type> <useLookahead> [<times>]");
			System.exit(1);
		}
		QueryExpTest test = null;
		if (args.length == 4) {
		test = new QueryExpTest(args[0], args[1], args[2], args[3]);
		} else {
			test = new QueryExpTest(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
		}
		test.run();
	}

	private void run() throws CorruptIndexException, IOException, ParseException {
		IndexSearcher searcher = new IndexSearcher(indexPath);
		TreeTerm starterTerm = new TreeTerm(0, TreeAxis.DESCENDANT, new Term("sent", "the"));
		TreeExpr starterExpr = new TreeExpr();
		starterExpr.addTerm(starterTerm);
		SimpleHitCollector collector = new SimpleHitCollector(1);
		
		searcher.search(new TreebankQuery(starterExpr), collector);
		collector.reset();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		searcher.search(new TreebankQuery(starterExpr), collector);

		for (int i = 0; i < times; i++) {
			collector.reset();
			QueryBuilder builder = new QueryBuilder(query);
			final TreebankQuery q = builder.parse(joinType, useLookahead);
			long start = System.nanoTime();
			searcher.search(q, collector);
			long end = System.nanoTime();
			System.out.println((end - start) + "\t" + collector.totalHits);
		}
	}
}
