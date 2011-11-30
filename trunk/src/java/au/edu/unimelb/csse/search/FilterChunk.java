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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.complete.Result;
import au.edu.unimelb.csse.search.complete.Result.ByteArrayWrapper;
import au.edu.unimelb.csse.search.complete.Result.Match;
import au.edu.unimelb.csse.search.complete.Result.MatchStatus;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class FilterChunk {

	private FilterChunkElement[] elements = new FilterChunkElement[] {};
	private FilterChunkElement[] notElements = new FilterChunkElement[] {};
	private int doc;
	// a temp variable made into a field to stop it from instantiating a new
	// variable each time
	private boolean found;

	public boolean skipTo(int target) throws IOException {
		int max = target;
		int numberOfMatches = 0;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].doc() < target) {
				found = elements[i].skipTo(target);
				if (!found) {
					return false;
				}
				int d = elements[i].doc();
				if (d > max) {
					max = d;
					numberOfMatches = 1;
				} else if (d == max) {
					numberOfMatches++;
				}
			}
		}
		if (numberOfMatches < elements.length) {
			do {
				numberOfMatches = 0;
				for (int i = 0; i < elements.length; i++) {
					if (elements[i].doc() < max) {
						found = elements[i].skipTo(max);
						if (!found) {
							return false;
						}
					}
					int d = elements[i].doc();
					if (d > max) {
						max = d;
						numberOfMatches = 1;
					} else if (d == max) {
						numberOfMatches++;
					}
				}
			} while (numberOfMatches < elements.length);
		}
		doc = max;
		for (int i = 0; i < notElements.length; i++) {
			if (notElements[i].doc() < max)
				notElements[i].skipTo(max);
		}
		return true;
	}

	public int doc() {
		return doc;
	}

	public boolean nextDoc() throws IOException {
		int max = -1;
		int numberOfMatches = 0;
		boolean allPresent = true;
		if (elements.length == 0) {
			if (doc == -1 && notElements.length > 0) {
				for (int i = 0; i < notElements.length; i++) {
					elements[i].nextDoc();
				}
			}
			return true;
		}
		for (int i = 0; i < elements.length; i++) {
			found = elements[i].nextDoc();
			if (found) {
				int d = elements[i].doc();
				if (d > max) {
					max = d;
					numberOfMatches = 1;
				} else if (d == max) {
					numberOfMatches++;
				}
			}
			allPresent &= found;
		}
		if (!allPresent) {
			return false;
		}
		if (numberOfMatches < elements.length) {
			do {
				numberOfMatches = 0;
				for (int i = 0; i < elements.length; i++) {
					if (elements[i].doc() < max) {
						found = elements[i].skipTo(max);
						if (!found) {
							return false;
						}
					}
					int d = elements[i].doc();
					if (d > max) {
						max = d;
						numberOfMatches = 1;
					} else if (d == max) {
						numberOfMatches++;
					}
				}
			} while (numberOfMatches < elements.length);
		}
		doc = max;
		for (int i = 0; i < notElements.length; i++) {
			if (notElements[i].doc() < max)
				notElements[i].skipTo(max);
		}
		return true;
	}

	public void init(IndexReader reader) throws IOException {
		for (int i = 0; i < elements.length; i++) {
			elements[i].init(reader);
		}
		for (int i = 0; i < notElements.length; i++) {
			notElements[i].init(reader);
		}
		doc = -1;
	}

	void addElement(FilterChunkElement element, boolean isNot) {
		if (isNot) {
			FilterChunkElement[] newElements = new FilterChunkElement[notElements.length + 1];
			System
					.arraycopy(notElements, 0, newElements, 0,
							notElements.length);
			newElements[notElements.length] = element;
			notElements = newElements;
		} else {
			FilterChunkElement[] newElements = new FilterChunkElement[elements.length + 1];
			System.arraycopy(elements, 0, newElements, 0, elements.length);
			newElements[elements.length] = element;
			elements = newElements;
		}
	}

	public boolean isOnlyNots() {
		return elements.length == 0 && notElements.length > 0;
	}

	public boolean structureMatch(NodeDataBuffer filterBuffer,
			NodeDataBuffer data, BitSet matched, byte[] payloads,
			int[] positions) throws IOException {
		boolean satisfied;
		for (int i = 0; i < notElements.length; i++) {
			// only continue if the notElement is at the correct doc
			if (notElements[i].doc() == doc) {
				// this part remains the same even for not elements because the
				// structure match within the NOT element should not be negated
				satisfied = notElements[i].reverseStructureMatch(filterBuffer,
						payloads, positions);
				// this check is to perform a comparison between the first
				// element and the parent to which this filter is attached
				if (satisfied) {
					satisfied = notElements[i].axis().filterJoiner()
							.matchFilterHead(data, filterBuffer, matched, true);
					if (!satisfied) {
						return false;
					}
				}
			}
		}
		for (int i = 0; i < elements.length; i++) {
			satisfied = elements[i].reverseStructureMatch(filterBuffer,
					payloads, positions);
			if (!satisfied) {
				return false;
			}
			// this check is to perform a comparison between the first element
			// and the parent to which this filter is attached
			satisfied = elements[i].axis().filterJoiner().matchFilterHead(data,
					filterBuffer, matched, false);
			if (!satisfied) {
				return false;
			}
		}
		final int firstMatch = matched.nextSetBit(0);
		return firstMatch != -1 && firstMatch < data.size();
	}

	/**
	 * returns a resultstatus that contains a set of matches of all
	 * chunkelements
	 */
	public MatchStatus allStructureMatches(NodeDataBuffer filterBuffer,
			NodeDataBuffer data, BitSet matched, byte[] payloads,
			int[] positions) throws IOException {
		MatchStatus rs = new MatchStatus();
		boolean satisfied;
		for (int i = 0; i < notElements.length; i++) {
			if (notElements[i].doc() == doc) {
				satisfied = notElements[i].reverseStructureMatch(filterBuffer,
						payloads, positions);
				if (satisfied) {
					satisfied = notElements[i].axis()
							.filterJoiner().matchFilterHead(data, filterBuffer,
									matched, true);
					if (!satisfied) {
						rs.successful = false;
						return rs;
					}
				}
			}
		}
		Map<ByteArrayWrapper, List<Match>> results = new HashMap<ByteArrayWrapper, List<Match>>();
		for (int i = 0; i < elements.length; i++) {
			Result r = new Result(true);
			NodeDataBuffer dataClone = new NodeDataBuffer(data);
			r = elements[i]
					.allStructureMatch(r, dataClone, payloads, positions);
			if (!r.hasMatches()) {
				rs.successful = false;
				return rs;
			}
			final List<Match> newMatches = r.matches();
			if (i == 0) {
				for (Match m : newMatches) {
					ByteArrayWrapper key = m.firstStart();
					if (!results.containsKey(key)) {
						results.put(key, new ArrayList<Match>());
					}
					results.get(key).add(m);
				}
			} else {
				Map<ByteArrayWrapper, List<Match>> resultsForElement = new HashMap<ByteArrayWrapper, List<Match>>();
				// find intersection of keys
				for (Match m : newMatches) {
					ByteArrayWrapper key = m.firstStart();
					if (!resultsForElement.containsKey(key)) {
						resultsForElement.put(key, new ArrayList<Match>());
					}
					resultsForElement.get(key).add(m);
				}
				final Set<ByteArrayWrapper> intersection = results.keySet();
				if (intersection.retainAll(resultsForElement.keySet())) {
					List<ByteArrayWrapper> toRemove = new ArrayList<ByteArrayWrapper>();
					for (ByteArrayWrapper k1 : results.keySet()) {
						if (!intersection.contains(k1)) {
							toRemove.add(k1);
						}
					}
					for (ByteArrayWrapper baw : toRemove) {
						results.remove(baw);
					}
					toRemove.clear();
					for (ByteArrayWrapper k2 : resultsForElement.keySet()) {
						if (!intersection.contains(k2)) {
							toRemove.add(k2);
						}
					}
					for (ByteArrayWrapper baw : toRemove) {
						resultsForElement.remove(baw);
					}
				}
				for (ByteArrayWrapper key : results.keySet()) {
					List<Match> rm = results.get(key);
					List<Match> m = new ArrayList<Match>();
					for (Match match1 : rm) {
						for (Match match2 : resultsForElement.get(key)) {
							Match joined = r.new Match(match1, match2);
							m.add(joined);
						}
					}
					results.put(key, m);
				}
			}
		}
		rs.successful = true;
		Set<Match> matches = new HashSet<Match>();
		for (ByteArrayWrapper key : results.keySet()) {
			matches.addAll(results.get(key));
		}
		rs.matches = matches;
		return rs;
	}

	public void setJoinType(TermJoinType joinType) {
		for (int k = 0; k < elements.length; k++) {
			elements[k].setJoinType(joinType);
		}
		for (int k = 0; k < notElements.length; k++) {
			notElements[k].setJoinType(joinType);
		}
	}

	/**
	 * Sets lookahead optimization in filter chunk elements
	 * 
	 * The conjugate is passed because the chunks are always evaluated in
	 * reverse order
	 */
	public void setLookaheadOptimization() {
		for (int k = 0; k < elements.length; k++) {
			elements[k]
					.setLookaheadOptimization(elements[k].axis().conjugate());
		}
		for (int k = 0; k < notElements.length; k++) {
			notElements[k].setLookaheadOptimization(notElements[k].axis()
					.conjugate());
		}

	}

	public TreeAxis commonAxis() {
		TreeAxis axis = null;
		for (int k = 0; k < elements.length; k++) {
			if (axis == null) {
				axis = elements[k].axis();
			} else if (!axis.equals(elements[k].axis())) {
				return null;
			}
		}
		for (int k = 0; k < notElements.length; k++) {
			if (axis == null) {
				axis = notElements[k].axis();
			} else if (!axis.equals(notElements[k].axis())) {
				return null;
			}
		}
		return axis;
	}

	//For testing ONLY!
	public FilterChunkElement[] notElements() {
		return notElements;
	}
	
	//For testing ONLY!
	public FilterChunkElement[] elements() {
		return elements;
	}

	public StringBuilder toString(StringBuilder builder) {
		for (int k = 0; k < elements.length; k++) {
			builder = elements[k].toString(builder);
			if (k != elements.length - 1) {
				builder.append(" " + Operator.AND.symbol() + " ");
			}
		}
		if (elements.length > 0 && notElements.length > 0) {
			builder.append(" " + Operator.AND.symbol() + " ");
		}
		for (int k = 0; k < notElements.length; k++) {
			builder.append(Operator.NOT.symbol());
			builder = notElements[k].toString(builder);
			if (k != notElements.length - 1) {
				builder.append(" " + Operator.AND.symbol() + " ");
			}
		}
		return builder;
	}

	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}
}
