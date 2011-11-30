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

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.complete.Result;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class TreeExpr implements FilterChunkElement {
	private TreeTerm[] terms = new TreeTerm[] {};
	private int doc;
	// a temp variable made into a field to stop it from instantiating a new
	// variable each time
	private boolean found;

	public void addTerm(TreeTerm treeTerm) {
		TreeTerm[] newTerms = new TreeTerm[terms.length + 1];
		System.arraycopy(terms, 0, newTerms, 0, terms.length);
		newTerms[terms.length] = treeTerm;
		terms = newTerms;
	}

	public void init(IndexReader reader) throws IOException {
		for (int i = 0; i < terms.length; i++) {
			terms[i].init(reader);
		}
		doc = -1;
	}

	@Override
	public int doc() {
		return doc;
	}

	@Override
	public boolean nextDoc() throws IOException {
		int max = -1;
		int numberOfMaxTerms = 0;
		for (int i = 0; i < terms.length; i++) {
			found = terms[i].nextDoc();
			if (!found) {
				return false;
			}
			int d = terms[i].doc();
			if (d > max) {
				max = d;
				numberOfMaxTerms = 1;
			} else if (d == max) {
				numberOfMaxTerms++;
			}
		}
		while (numberOfMaxTerms < terms.length) {
			numberOfMaxTerms = 0;
			for (int i = 0; i < terms.length; i++) {
				if (terms[i].doc() < max) {
					found = terms[i].skipTo(max);
					if (!found) {
						return false;
					}
				}
				int d = terms[i].doc();
				if (d > max) {
					max = d;
					numberOfMaxTerms = 1;
				} else if (d == max) {
					numberOfMaxTerms++;
				}
			}
		}
		doc = max;
		return true;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		int max = target;
		int numberOfMaxTerms;
		do {
			numberOfMaxTerms = 0;
			for (int i = 0; i < terms.length; i++) {
				if (terms[i].doc() < max) {
					found = terms[i].skipTo(target);
					if (!found) {
						return false;
					}
				}
				int d = terms[i].doc();
				if (d > max) {
					max = d;
					numberOfMaxTerms = 1;
				} else if (d == max) {
					numberOfMaxTerms++;
				}
			}
		} while (numberOfMaxTerms < terms.length);
		doc = max;
		return true;
	}

	/**
	 * In this class structureMatch is invoked by the TreeQuery. nextDoc or
	 * skipTo should have been called before this method is invoked.
	 * 
	 * @param data
	 * @param payloadBuffer
	 * @return
	 * @throws IOException
	 */
	public boolean structureMatch(NodeDataBuffer data, byte[] payloadBuffer,
			int[] positionsBuffer) throws IOException {
		if (terms.length == 0)
			return false;
		TreeTerm first = terms[0];
		boolean matched = first.rootStructureMatch(data, payloadBuffer,
				positionsBuffer);
		if (!matched) {
			return false;
		}
		for (int i = 1; i < terms.length; i++) {
			matched = terms[i].structureMatch(data, payloadBuffer,
					positionsBuffer);
			if (!matched) {
				return false;
			}
		}
		return true;
	}

	// @Override
	// public boolean structureMatch(NodeDataBuffer data, byte[] payloadBuffer,
	// int[] positionsBuffer)
	// throws IOException {
	// for (int i = 0; i < terms.length; i++) {
	// boolean matched = terms[i].structureMatch(data, payloadBuffer,
	// positionsBuffer);
	// if (!matched) {
	// return false;
	// }
	// }
	// return true;
	// }

	public int size() {
		return terms.length;
	}

	public boolean isSingleDescTermWOFilter() {
		return terms.length == 1 && !terms[0].hasFilter();
	}

	public boolean reverseStructureMatch(NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionsBuffer) throws IOException {
		boolean satisfied = terms[terms.length - 1].reverseStructureMatch(data,
				payloadBuffer, positionsBuffer);
		if (!satisfied) {
			return false;
		}
		for (int i = terms.length - 2; i >= 0; i--) {
			satisfied = terms[i].conjugateAxisStructureMatch(data,
					payloadBuffer, positionsBuffer, terms[i + 1]);
			if (!satisfied) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TreeAxis axis() {
		return terms[0].axis();
	}

	/**
	 * Finds all hits in a particular document
	 * 
	 * @return
	 * @throws IOException
	 */
	public Result allStructureMatch(Result result, NodeDataBuffer data,
			byte[] payloads, int[] positions) throws IOException {
		if (terms.length == 0)
			return result;
		TreeTerm first = terms[0];
		if (data.size > 0) {
			// enters here when finding all matches in filter expressions
			result = terms[0].allStructureMatch(result, data, payloads,
					positions);
		} else {
			result = first
					.rootStructureMatch(result, data, payloads, positions);
		}
		for (int i = 1; i < terms.length; i++) {
			result = terms[i].allStructureMatch(result, data, payloads,
					positions);
		}
		return result;
	}

	public TreeTerm getTerm(int pos) {
		return terms[pos];
	}

	public void setJoinType(TermJoinType joinType) {
		for (int i = 0; i < terms.length; i++) {
			terms[i].setJoinType(joinType);
		}
	}

	public void setStopAtFirstMatchForLastTerm() {
		terms[size() - 1].setStopAtFirstMatch();
	}

	public void setLookaheadOptimization() {
		for (int i = 0; i < terms.length; i++)
			terms[i].setLookaheadOptimization(i + 1 < terms.length ? terms[i + 1].axis() : null);
	}
	
	@Override
	public void setLookaheadOptimization(TreeAxis axis) {
		for (int i = terms.length - 1; i > 0; i--) {
			terms[i].setLookaheadOptimization(terms[i].axis().conjugate());
		}
		terms[0].setLookaheadOptimization(axis);
	}
	
	public StringBuilder toString(StringBuilder builder) {
		for (TreeTerm term : terms) {
			builder = term.toString(builder);
		}
		return builder;
	}
	
	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}
}
