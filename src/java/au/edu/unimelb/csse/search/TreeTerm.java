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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.complete.Result;
import au.edu.unimelb.csse.search.complete.Result.ResultStatus;
import au.edu.unimelb.csse.search.join.LookaheadOptimization;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class TreeTerm implements FilterChunkElement {
	private TreeAxis axis;
	private Term term;
	private TermFilter filter;
	private TermPositions positions;
	private int doc;
	private TermJoinType joinType;
	// Lookahead is only set if a term is a FilterChunkElement
	private LookaheadOptimization optimization = LookaheadOptimization.NONE;

	// private variables made into fields to avoid instantiating a new
	// instance each time
	private boolean found;
	private int validInstances;
	private boolean stopAtFirst = false;

	public TreeTerm(TreeAxis axis, Term term) {
		this(axis, term, null);
	}

	public TreeTerm(TreeAxis axis, Term term, TermFilter filter) {
		this.axis = axis;
		this.term = term;
		this.filter = filter;
	}

	@Override
	public void init(IndexReader reader) throws IOException {
		positions = reader.termPositions(term);
		if (filter != null)
			filter.init(reader);
		doc = -1;
	}

	@Override
	public boolean nextDoc() throws IOException {
		found = positions.next();
		if (!found)
			return false;
		int curDoc = positions.doc();
		if (filter == null) {
			doc = curDoc;
			return true;
		}
		boolean filterHasMore = filter.skipTo(curDoc);
		if (!filterHasMore) {
			return false;
		}
		// fd should be greater than or equal to curDoc b'coz of the skipTo call
		int fd = filter.doc();
		while (fd != curDoc) {
			found = positions.skipTo(fd);
			if (!found) {
				return false;
			}
			curDoc = positions.doc();
			// now curDoc should be greater than or equal to fd
			if (fd != curDoc) {
				found = filter.skipTo(curDoc);
				if (!found) {
					return false;
				}
				fd = filter.doc();
			}
		}
		doc = curDoc;
		return true;
	}

	@Override
	public int doc() {
		return doc;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		found = positions.skipTo(target);
		if (!found)
			return false;
		int currentDoc = positions.doc();
		if (filter == null) {
			doc = currentDoc;
			return true;
		}
		boolean filterHasMore = filter.skipTo(currentDoc);
		if (!filterHasMore) {
			return false;
		}
		int fd = filter.doc();
		while (fd != currentDoc) {
			if (fd < currentDoc) {
				found = filter.skipTo(currentDoc);
				if (!found) {
					return false;
				}
				fd = filter.doc();
			} else {
				found = positions.skipTo(fd);
				if (!found) {
					return false;
				}
				currentDoc = positions.doc();
			}
		}
		doc = currentDoc;
		return true;
	}

	public boolean structureMatch(NodeDataBuffer data, byte[] payloadBuffer,
			int[] positionsBuffer) throws IOException {
		return structureMatch(data, payloadBuffer, positionsBuffer, axis);
	}

	private boolean structureMatch(NodeDataBuffer data, byte[] payloadBuffer,
			int[] positionsBuffer, TreeAxis treeAxis) throws IOException {
		validInstances = treeAxis.termJoiner(joinType)
				.joinTerm(positions, data, payloadBuffer, positionsBuffer,
						optimization, stopAtFirst);
		if (validInstances == 0) {
			return false;
		}
		data.set(validInstances, payloadBuffer, positionsBuffer);
		if (filter == null) {
			return true;
		}
		return filter.structureMatch(data, payloadBuffer, positionsBuffer);
	}

	/**
	 * This method is called only when either a TreeExpr is reversed and its
	 * last term's occurrences are found as though it had a descendant axis
	 * operator preceding it.
	 * 
	 * OR
	 * 
	 * It could also be called if a TreeTerm is present as a filter element.
	 */
	public boolean reverseStructureMatch(NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionsBuffer) throws IOException {
		int freq = positions.freq();
		validInstances = 0;
		for (int i = 0; i < freq && i < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionsBuffer[validInstances] = positions.nextPosition();
			positions.getPayload(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
					* validInstances);
			if (validInstances > 0
					&& !optimization.equals(LookaheadOptimization.NONE)) {
				int val = optimization.check(payloadBuffer, validInstances - 1);
				if (val == LookaheadOptimization.WRITE) {
					validInstances++;
				} else if (val == LookaheadOptimization.OVERWRITE) {
					positionsBuffer[validInstances - 1] = positionsBuffer[validInstances];
					System.arraycopy(payloadBuffer,
							NodeDataBuffer.PAYLOAD_LENGTH * validInstances,
							payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
									* (validInstances - 1),
							NodeDataBuffer.PAYLOAD_LENGTH);
				}
			} else {
				validInstances++;
			}
			// positionsBuffer[i] = positions.nextPosition();
			// positions.getPayload(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
			// * i);
		}
		if (validInstances == 0)
			return false;
		data.set(validInstances, payloadBuffer, positionsBuffer);
		if (filter == null) {
			return true;
		}
		return filter.structureMatch(data, payloadBuffer, positionsBuffer);
	}

	/**
	 * Checks structure match for the root element. This is different from the
	 * other structureMatch method because the root doesn't have a previous
	 * element to compare with
	 * 
	 * @param data
	 * @param payloadBuffer
	 * @return
	 * @throws IOException
	 */
	public boolean rootStructureMatch(NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionBuffer) throws IOException {
		int freq = positions.freq();
		validInstances = 0;
		if (stopAtFirst) {
			// filter is always null when TermJoinType of
			// STOP_AT_FIRST_MATCH is set
			for (int i = 0; i < freq; i++) {
				positionBuffer[validInstances] = positions.nextPosition();
				positions.getPayload(payloadBuffer,
						NodeDataBuffer.PAYLOAD_LENGTH * validInstances);
				if ((axis.equals(TreeAxis.CHILD) && payloadBuffer[NodeDataBuffer.PAYLOAD_LENGTH
						* validInstances + NodeDataBuffer.HEIGHT] == 0)
						|| axis.equals(TreeAxis.DESCENDANT)) {
					return true;
				}
			}
			return false;
		}
		// validInstances < NodeDataBuffer.BUFFER_SIZE check is done to check
		// for buffer overflow
		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionBuffer[validInstances] = positions.nextPosition();
			positions.getPayload(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
					* validInstances);
			if ((axis.equals(TreeAxis.CHILD) && payloadBuffer[NodeDataBuffer.PAYLOAD_LENGTH
					* validInstances + NodeDataBuffer.HEIGHT] == 0)
					|| axis.equals(TreeAxis.DESCENDANT)) {
				if (validInstances > 0
						&& !optimization.equals(LookaheadOptimization.NONE)) {
					int val = optimization.check(payloadBuffer,
							validInstances - 1);
					if (val == LookaheadOptimization.WRITE) {
						validInstances++;
					} else if (val == LookaheadOptimization.OVERWRITE) {
						positionBuffer[validInstances - 1] = positionBuffer[validInstances];
						System.arraycopy(payloadBuffer,
								NodeDataBuffer.PAYLOAD_LENGTH * validInstances,
								payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
										* (validInstances - 1),
								NodeDataBuffer.PAYLOAD_LENGTH);
					}
				} else {
					validInstances++;
				}
				if (axis.equals(TreeAxis.CHILD))
					break;
			}
		}
		if (validInstances == 0) {
			return false;
		}
		data.set(validInstances, payloadBuffer, positionBuffer);
		if (filter == null) {
			return true;
		}
		return filter.structureMatch(data, payloadBuffer, positionBuffer);
	}

	/**
	 * Checks structure match for the root element and stores outcome in result
	 * object. This is different from the other structureMatch method because
	 * the root doesn't have a previous element to compare with.
	 * 
	 * @param data
	 * @param payloadBuffer
	 * @return
	 * @throws IOException
	 */
	public Result rootStructureMatch(Result result, NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionBuffer) throws IOException {
		int freq = positions.freq();
		// if (freq < payloadBuffer.length / NodeDataBuffer.PAYLOAD_LENGTH) {
		int validInstances = 0;
		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionBuffer[validInstances] = positions.nextPosition();
			positions.getPayload(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
					* validInstances);
			if ((axis.equals(TreeAxis.CHILD) && payloadBuffer[NodeDataBuffer.PAYLOAD_LENGTH
					* validInstances + NodeDataBuffer.HEIGHT] == 0)
					|| axis.equals(TreeAxis.DESCENDANT)) {
				byte[] bytes = new byte[NodeDataBuffer.PAYLOAD_LENGTH];
				System.arraycopy(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
						* validInstances, bytes, 0,
						NodeDataBuffer.PAYLOAD_LENGTH);
				result.addNew(bytes, axis);
				validInstances++;
			}
		}
		if (validInstances == 0) {
			return new Result();
		}
		data.set(validInstances, payloadBuffer, positionBuffer);
		if (filter == null) {
			result.commitAfterTerm();
			return result;
		}
		ResultStatus rsf = filter.allStructureMatches(result, data,
				payloadBuffer, positionBuffer);
		if (!rsf.successful) {
			return new Result();
		}
		rsf.result.commitAfterTerm();
		return rsf.result;
	}

	public boolean hasFilter() {
		return filter != null;
	}

	public boolean conjugateAxisStructureMatch(NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionsBuffer, TreeTerm previous)
			throws IOException {
		TreeAxis effectiveAxis = TreeAxis.CONJUGATE.get(previous.axis);
		return structureMatch(data, payloadBuffer, positionsBuffer,
				effectiveAxis);
	}

	@Override
	public TreeAxis axis() {
		return axis;
	}

	public Result allStructureMatch(Result result, NodeDataBuffer data,
			byte[] payloadBuffer, int[] positionsBuffer) throws IOException {
		result = axis.termJoiner(joinType).joinTermAllMatches(result,
				positions, data, payloadBuffer, positionsBuffer, axis);
		data
				.set(
						result.newResultsIndexKeySize() > NodeDataBuffer.BUFFER_SIZE ? NodeDataBuffer.BUFFER_SIZE
								: result.newResultsIndexKeySize(),
						payloadBuffer, positionsBuffer);
		if (result.hasNewMatches() && filter != null) {
			ResultStatus rsf = filter.allStructureMatches(result, data,
					payloadBuffer, positionsBuffer);
			if (!rsf.successful) {
				return new Result();
			} else {
				result = rsf.result;
			}
		}
		result.commitAfterTerm();
		return result;

	}

	public void setJoinType(TermJoinType joinType) {
		this.joinType = joinType;
		if (filter != null) {
			filter.setJoinType(joinType);
		}
	}

	/**
	 * This sets the joinType to TermJoinType.STOP_AT_FIRST_MATCH
	 * 
	 * It should be called only on the last term in a query
	 */
	public void setStopAtFirstMatch() {
		// TODO: StopAtFirstMatch join is only set if there is no filter
		if (filter == null)
			this.stopAtFirst = true;
	}

	@Override
	public void setLookaheadOptimization(TreeAxis nextAxis) {
		TreeAxis filterCommonAxis = null;
		if (hasFilter()) {
			filter.setLookaheadOptimization();
			filterCommonAxis = filter.commonAxisOfAllChunks();
			if (filterCommonAxis == null)
				return;
			if (nextAxis == null) {
				optimization = filterCommonAxis.lookaheadOptimization();
			} else if (nextAxis.equals(filterCommonAxis)) {
				optimization = nextAxis.lookaheadOptimization();
			}
		} else {
			if (nextAxis != null) {
				optimization = nextAxis.lookaheadOptimization();
			}
		}
	}

	public TreeAxis commonAxisForAllFilterChunks(TreeTerm nextTerm) {
		if (nextTerm == null) {
			return filter.commonAxisOfAllChunks();
		}
		if (nextTerm.axis.equals(filter.commonAxisOfAllChunks())) {
			return nextTerm.axis;
		}
		return null;
	}

	//For testing ONLY!
	public String termLabel() {
		return term.text();
	}

	//For testing ONLY!
	public TermFilter filter() {
		return filter;
	}

	public StringBuilder toString(StringBuilder builder) {
		builder.append(axis.symbol());
		builder.append(term.text());
		if (filter != null) {
			builder = filter.toString(builder);
		}
		return builder;
	}
	
	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}

}
