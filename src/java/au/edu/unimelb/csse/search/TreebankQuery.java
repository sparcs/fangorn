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
package au.edu.unimelb.csse.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import au.edu.unimelb.csse.search.join.TermJoinType;

public class TreebankQuery extends Query {
	private static final long serialVersionUID = -1267167816581277525L;
	private final TreeExpr exp;
	
	public TreebankQuery(TreeExpr exp) {
		this(exp, TermJoinType.EARLY_STOP_WITH_FC);
	}

	public TreebankQuery(TreeExpr exp, TermJoinType joinType) {
		this(exp, joinType, false);
	}
	
	public TreebankQuery(TreeExpr exp, TermJoinType joinType, boolean useLookahead) {
		this.exp = exp;
		exp.setJoinType(joinType);
		exp.setStopAtFirstMatchForLastTerm();
		if (useLookahead) {
			exp.setLookaheadOptimization();
		}
	}


	@Override
	public String toString(String field) {
		StringBuilder builder  = new StringBuilder();
		return exp.toString(builder).toString();
	}

	@Override
	protected Weight createWeight(Searcher searcher) throws IOException {
		return new TreebankWeight(searcher);
	}

	@Override
	public void setBoost(float b) {
		throw new RuntimeException(
				"Query weights are not permitted in this version");
	}
	
	public TreeExpr getTreeExpr() {
		return exp;
	}
	
	@Override
	public Query rewrite(IndexReader reader) throws IOException {
//		Optimizing should take care of using the correct scorer
//		if (exp.isSingleDescTermWOFilter()) {//optimizing single term queries
//			return new TermQuery(exp.firstTerm());
//		}
		return this;
	}

	private class TreebankWeight implements Weight {
		private static final long serialVersionUID = 1939725042087552162L;
		private Similarity similarity;

		public TreebankWeight(Searcher searcher) throws IOException {
			this.similarity = getSimilarity(searcher);
		}

		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			// TODO Auto-generated method stub
			return new Explanation();
		}

		public Query getQuery() {
			return TreebankQuery.this;
		}

		public float getValue() {
			return 1;
		}

		public void normalize(float norm) {

		}

		public Scorer scorer(final IndexReader reader) throws IOException {
			if (exp.size() == 0) // optimize zero-term case
				return null;
			exp.init(reader);

			return new Scorer(similarity) {
				private byte[] payloadBuffer = new byte[NodeDataBuffer.BUFFER_SIZE * 4];
				private int[] positionsBuffer = new int[NodeDataBuffer.BUFFER_SIZE];
				private final NodeDataBuffer buffer = new NodeDataBuffer();
				private int doc = -1;

				@Override
				public Explanation explain(int doc) throws IOException {
					return new Explanation();
				}

				@Override
				public float score() throws IOException {
					return 1;
				}

				@Override
				public int doc() {
					return doc;
				}

				@Override
				public boolean next() throws IOException {
					boolean found = false;
					while(!found || (found & !exp.structureMatch(buffer, payloadBuffer, positionsBuffer))) {
						found = exp.nextDoc();
						if (!found) return false;
					}
					doc = exp.doc();
					return true;
				}

				@Override
				public boolean skipTo(int target) throws IOException {
					return exp.skipTo(target);
				}


			};
		}

		public float sumOfSquaredWeights() throws IOException {
			return 1;
		}

	}
}
