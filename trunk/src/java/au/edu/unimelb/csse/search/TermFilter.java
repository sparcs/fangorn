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
import au.edu.unimelb.csse.search.complete.Result.ResultStatus;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class TermFilter implements QueryElement {
	public static final String FILTER_OPEN = "[";
	public static final String FILTER_CLOSE = "]";
	
	private FilterChunk[] chunks = new FilterChunk[] {};
	private int[] chunksAtMinDoc;
	private int numChunksAtMinDoc;
	private int doc;
	private int[] endedChunks;
	private int numEndedChunks;
	private final NodeDataBuffer filterBuffer = new NodeDataBuffer();
	private final BitSet matched = new BitSet(NodeDataBuffer.BUFFER_SIZE);
	private final BitSet matchedPerFilter = new BitSet(
			NodeDataBuffer.BUFFER_SIZE);

	public TermFilter(FilterChunkElement first) {
		this(first, false);
	}

	public TermFilter(FilterChunkElement first, boolean isNot) {
		FilterChunk chunk = new FilterChunk();
		chunk.addElement(first, isNot);
		addToChunks(chunk);
	}

	private void addToChunks(FilterChunk chunk) {
		FilterChunk[] newChunks = new FilterChunk[chunks.length + 1];
		System.arraycopy(chunks, 0, newChunks, 0, chunks.length);
		newChunks[chunks.length] = chunk;
		chunks = newChunks;
	}

	public void addTerm(Operator operator, FilterChunkElement term) {
		addTerm(operator, term, false);
	}

	public void addTerm(Operator operator, FilterChunkElement term,
			boolean isNot) {
		if (Operator.OR.equals(operator)) {
			// branch
			addToChunks(new FilterChunk());
		}
		chunks[chunks.length - 1].addElement(term, isNot);
	}

	public void init(IndexReader reader) throws IOException {
		for (int i = 0; i < chunks.length; i++) {
			chunks[i].init(reader);
		}
		chunksAtMinDoc = new int[chunks.length];
		numChunksAtMinDoc = -1;
		endedChunks = new int[chunks.length];
		doc = -1;
		numEndedChunks = 0;
	}

	public boolean nextDoc() throws IOException {
		boolean found;
		int nextDoc = Integer.MAX_VALUE;
		if (numChunksAtMinDoc == -1) {// first time
			for (int i = 0; i < chunks.length; i++) {
				found = chunks[i].nextDoc();
				if (!found) {
					endedChunks[numEndedChunks++] = i;
				}
				if (found) {
					int d = chunks[i].doc();
					if (d < nextDoc) {
						nextDoc = d;
						numChunksAtMinDoc = 1;
						chunksAtMinDoc[0] = i;
					} else if (d == nextDoc) {
						chunksAtMinDoc[numChunksAtMinDoc++] = i;
					}
				}
			}
		} else {
			// only calls next on the chunks at the smallest doc
			for (int i = 0; i < numChunksAtMinDoc; i++) {
				found = chunks[chunksAtMinDoc[i]].nextDoc();
				if (!found) {
					endedChunks[numEndedChunks++] = i;
				}
			}
			// find min doc and chunks at that doc
			int endedChunksIndex = 0;
			for (int i = 0; i < chunks.length && numEndedChunks < chunks.length; i++) {
				if (endedChunksIndex < numEndedChunks
						&& i == endedChunks[endedChunksIndex]) {
					endedChunksIndex++;
					continue;
				}
				int d = chunks[i].doc();
				if (d < nextDoc) {
					nextDoc = d;
					numChunksAtMinDoc = 1;
					chunksAtMinDoc[0] = i;
				} else if (d == nextDoc) {
					chunksAtMinDoc[numChunksAtMinDoc++] = i;
				}
			}
		}
		if (nextDoc != Integer.MAX_VALUE) {
			doc = nextDoc;
		}
		return numEndedChunks != chunks.length;
	}

	public int doc() {
		return doc;
	}

	public boolean skipTo(int target) throws IOException {
		int endedChunksIndex = 0;
		boolean found;
		int oldNoMoreInChunk = numEndedChunks;
		// skipTo operates on all chunks less than target unlike next
		for (int i = 0; i < chunks.length && numEndedChunks < chunks.length; i++) {
			if (endedChunksIndex < oldNoMoreInChunk
					&& i == endedChunks[endedChunksIndex]) {
				endedChunksIndex++;
				continue;
			}
			int d = chunks[i].doc();
			if (d < target) {
				found = chunks[i].skipTo(target);
				if (!found) {
					endedChunks[numEndedChunks++] = i;
				}
			}
		}
		// find min doc and chunks at that doc
		endedChunksIndex = 0;
		int nextDoc = Integer.MAX_VALUE;
		for (int i = 0; i < chunks.length && numEndedChunks < chunks.length; i++) {
			if (endedChunksIndex < numEndedChunks
					&& i == endedChunks[endedChunksIndex]) {
				endedChunksIndex++;
				continue;
			}
			int d = chunks[i].doc();
			if (d < nextDoc) {
				nextDoc = d;
				numChunksAtMinDoc = 1;
				chunksAtMinDoc[0] = i;
			} else if (d == nextDoc) {
				chunksAtMinDoc[numChunksAtMinDoc++] = i;
			}
		}
		if (nextDoc != Integer.MAX_VALUE) {
			doc = nextDoc;
		}
		return numEndedChunks != chunks.length;
	}

	public boolean structureMatch(NodeDataBuffer data, byte[] payloads,
			int[] positions) throws IOException {
		filterBuffer.reset();
		matched.clear(0, data.size);
		for (int i = 0; i < numChunksAtMinDoc; i++) {
			FilterChunk c = chunks[chunksAtMinDoc[i]];
			if (c.doc() == doc) {// check even if chunk contains a single
				// "not" expr/term
				matchedPerFilter.set(0, data.size);
				if (c.structureMatch(filterBuffer, data, matchedPerFilter,
						payloads, positions)) {
					matched.or(matchedPerFilter);
				}
			} else if (c.isOnlyNots() && c.doc() > doc) {// need not be at this
				// doc
				// status quo on matches
				matched.set(0, data.size());
				return true;
			}
		}
		int j = 0;
		int nextClearBit = matched.nextClearBit(j);
		while (nextClearBit != -1 && nextClearBit < data.size()) {
			data.addToRemoveBuffer(nextClearBit);
			j = nextClearBit + 1;
			if (j >= data.size()) {
				break;
			}
			nextClearBit = matched.nextClearBit(j);
		}
		data.remove();
		if (data.size() == 0)
			return false;
		return true;
	}

	/**
	 * Finds all hits that match the filter criteria
	 * 
	 * @param result
	 *            the result object that contains existing results.
	 * @param data
	 *            the data that contains the matched elements of the term to
	 *            which this filter is attached.
	 * @param payloads
	 *            a buffer to read in payloads.
	 * @param positions
	 *            a buffer to read in positions.
	 * @return
	 * @throws IOException
	 */
	public ResultStatus allStructureMatches(Result result,
			NodeDataBuffer data, byte[] payloads, int[] positions)
			throws IOException {
		matched.clear(0, data.size);
		boolean hasSingleNotExprMatched = false;
		Map<ByteArrayWrapper, List<Match>> ms = new HashMap<ByteArrayWrapper, List<Match>>();
		for (int i = 0; i < numChunksAtMinDoc; i++) {
			filterBuffer.reset();
			FilterChunk c = chunks[chunksAtMinDoc[i]];
			if (c.doc() == doc) {
				matchedPerFilter.set(0, data.size);
				MatchStatus rs = c.allStructureMatches(filterBuffer, data,
						matchedPerFilter, payloads, positions);
				if (rs.successful) {
					if (c.isOnlyNots()) {
						hasSingleNotExprMatched = true;
						matched.or(matchedPerFilter);
					} else {
						final Map<ByteArrayWrapper, List<Match>> matches = new HashMap<ByteArrayWrapper, List<Match>>();
						for (Match m : rs.matches) {
							final ByteArrayWrapper parentTermsByteArray = m.firstStart();
							if (!matches.containsKey(parentTermsByteArray)) {
								matches.put(parentTermsByteArray, new ArrayList<Match>());
							}
							matches.get(parentTermsByteArray).add(m);
						}
						Set<ByteArrayWrapper> keys = matches.keySet();
						for (int j = 0; j < data.size; j++) {
							final ByteArrayWrapper d = result.new ByteArrayWrapper(data.getAsBytes(j));
							if (!matchedPerFilter.get(j)) {
								//this means that a not filter has removed this result earlier
								matches.remove(d);
							} else if (!keys.contains(d)) {
								matchedPerFilter.clear(j);
							}
						}
						//this has to be done here.. because matchedPerFilter is only updated in the above chunk of code
						matched.or(matchedPerFilter);
						for (ByteArrayWrapper key : keys) {
							if (!ms.containsKey(key)) {
								ms.put(key, new ArrayList<Match>());
							}
							ms.get(key).addAll(matches.get(key));
						}
					}
				}
			} else if (c.isOnlyNots() && c.doc() > doc) {
				matched.set(0, data.size);
			}
		}
		int j = 0;
		int nextClearBit = matched.nextClearBit(j);
		while (nextClearBit != -1 && nextClearBit < data.size) {
			data.addToRemoveBuffer(nextClearBit);
			j = nextClearBit + 1;
			if (j >= data.size) {
				break;
			}
			nextClearBit = matched.nextClearBit(j);
		}
		data.remove();
		ResultStatus rs = new ResultStatus();
		if (data.size == 0) {
			rs.successful = false;
			return rs;
		}
		rs.successful = true;
		Set<ByteArrayWrapper> toRetain = new HashSet<ByteArrayWrapper>();
		for (int i = 0; i < data.size; i++) {
			toRetain.add(result.new ByteArrayWrapper(data.getAsBytes(i)));
		}
		//the order of the next two statements is important
		result.retainInNew(toRetain);
		result.addToMatches(ms, hasSingleNotExprMatched);
		rs.result = result;
		return rs;
	}

	public void setJoinType(TermJoinType joinType) {
		for (int i = 0; i < chunks.length; i++) {
			chunks[i].setJoinType(joinType);
		}
	}

	public void setLookaheadOptimization() {
		for (int i = 0; i < chunks.length; i++) {
			chunks[i].setLookaheadOptimization();
		}
	}

	public TreeAxis commonAxisOfAllChunks() {
		TreeAxis axis = null;
		for (int i = 0; i < chunks.length; i++) {
			if (chunks[i].commonAxis() == null) return null;
			if (axis == null) {
				axis = chunks[i].commonAxis();
			} else if (!axis.equals(chunks[i].commonAxis())) {
				return null;
			}
		}
		return axis;
	}

	//For testing ONLY!
	public FilterChunk[] chunks() {
		return chunks;
	}

	public StringBuilder toString(StringBuilder builder) {
		builder.append(FILTER_OPEN);
		for (int i = 0; i < chunks.length; i++) {
			builder = chunks[i].toString(builder);
			if (i != chunks.length - 1) {
				builder.append(" " + Operator.OR.symbol() + " ");
			}
		}
		builder.append(FILTER_CLOSE);
		return builder;
	}
	
	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}
}
