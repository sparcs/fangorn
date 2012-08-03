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
package au.edu.unimelb.csse.search.complete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.edu.unimelb.csse.JSONAble;
import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.NodeDataBuffer;

/**
 * Each Result object stores the result nodes in a particular sentence
 * 
 * A sentence can match the query pattern several times. For each match there
 * are multiple nodes involved. A match is stored in the SingleResult object.
 * 
 * @author sumukh
 * 
 */
public class Result implements JSONAble, Cloneable {
	private List<Match> matches = new ArrayList<Match>();
	private List<Match> newMatches = new ArrayList<Match>();
	private Map<ByteArrayWrapper, List<Integer>> index = new HashMap<ByteArrayWrapper, List<Integer>>();
	private Map<ByteArrayWrapper, List<Integer>> newIndex = new HashMap<ByteArrayWrapper, List<Integer>>();
	private boolean allowUknownElements;

	/**
	 * This is the default constructor
	 */
	public Result() {
		this(false);
	}

	/**
	 * We should allow unknown elements at the start of expressions within
	 * filter expressions
	 * 
	 * @param allowUnknownElements
	 */
	public Result(boolean allowUnknownElements) {
		this.allowUknownElements = allowUnknownElements;
	}

	public void addNew(byte[] end, TreeAxis operator, int termId) {
		ByteArrayWrapper s = new ByteArrayWrapper(new byte[] {});
		ByteArrayWrapper e = new ByteArrayWrapper(end);
		addToMatchesUpdateIndex(new Match(), s, e, operator, termId);
	}

	public void addNew(byte[] start, byte[] end, TreeAxis operator, int termId) {
		ByteArrayWrapper s = new ByteArrayWrapper(start);
		ByteArrayWrapper e = new ByteArrayWrapper(end);

		if (index.containsKey(s)) {
			List<Integer> positions = index.get(s);
			for (int position : positions) {
				Match old = matches.get(position);
				try {
					Match newMatch = (Match) old.clone();
					addToMatchesUpdateIndex(newMatch, s, e, operator, termId);
				} catch (CloneNotSupportedException e1) { // Unreachable code!
					throw new RuntimeException("Single result match should be cloneable.");
				}
			}
		} else if (allowUknownElements) {
			addToMatchesUpdateIndex(new Match(), s, e, operator,termId);
		}
	}

	private void addToMatchesUpdateIndex(Match match, ByteArrayWrapper s,
			ByteArrayWrapper e, TreeAxis operator, int termId) {
		match.add(s, e, operator, termId);
		newMatches.add(match);
		List<Integer> list = (newIndex.containsKey(e) ? newIndex.get(e) : new ArrayList<Integer>());
		list.add(newMatches.size() - 1); // latest match is always at the end of list
		newIndex.put(e, list);
	}

	public void commitAfterTerm() {
		index.clear();
		index.putAll(newIndex);
		newIndex.clear();
		matches.clear();
		matches.addAll(newMatches);
		newMatches.clear();
	}

	@Override
	public String toString() {
		String line1 = "Number of matches: " + matches.size() + "\n";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < matches.size(); i++) {
			b.append("Match " + i + ":\n");
			b.append(matches.get(i).toString());
		}
		return line1 + b.toString();
	}

	public boolean hasNewMatches() {
		return newMatches.size() > 0;
	}

	public boolean hasMatches() {
		return matches.size() > 0;
	}

	public int lastTermMatchSize() {
		return newIndex.size();
	}

	public List<Match> matches() {
		return matches;
	}

	public String asJSONString() {
		StringBuilder b = new StringBuilder();
		b.append("{\"num\":\"");
		b.append(matches.size());
		b.append("\",\"ms\":[");
		for (Match match : matches) {
			match.asJSONString(b);
			b.append(",");
		}
		if (matches.size() > 0) b.deleteCharAt(b.length() - 1);
		b.append("]}");
		return b.toString();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Result c = new Result();
		for (Match m : matches) c.matches.add((Match) m.clone());
		for (Match m : newMatches) c.newMatches.add((Match) m.clone());
		for (ByteArrayWrapper baw : index.keySet()) c.index.put((ByteArrayWrapper) baw.clone(), index.get(baw));
		for (ByteArrayWrapper baw : newIndex.keySet()) c.newIndex.put((ByteArrayWrapper) baw.clone(), newIndex.get(baw));
		return c;
	}
	
	public String matchesAsJSONString() {
		StringBuilder b = new StringBuilder();
		b.append("{\"ms\":[");
		for (Match match : matches) {
			match.asJSONString(b);
			b.append(",");
		}
		if (matches.size() > 0)
			b.deleteCharAt(b.length() - 1);
		b.append("]}");
		return b.toString();
	}

	public int numberOfMatches() {
		return matches.size();
	}

	/**
	 * This stores the edges between start and end elements of different result
	 * matches along with the operator between the nodes. The operator is stored
	 * mainly to allow correct rendering of the relation on the client.
	 * 
	 * @author sumukh
	 * 
	 */
	public class Match implements Cloneable, EfficientJSONAble {
		private List<ByteArrayWrapper> starts, ends;
		private List<TreeAxis> operators;
		private List<Integer> termIds;

		public Match() {
			starts = new ArrayList<ByteArrayWrapper>();
			ends = new ArrayList<ByteArrayWrapper>();
			operators = new ArrayList<TreeAxis>();
			termIds = new ArrayList<Integer>();
		}

		public ByteArrayWrapper firstStart() {
			return starts.size() == 0 ? null : starts.get(0);
		}

		public Match(Match match1, Match match2) {
			this();
			starts.addAll(match1.starts);
			ends.addAll(match1.ends);
			operators.addAll(match1.operators);
			termIds.addAll(match1.termIds);
			starts.addAll(match2.starts);
			ends.addAll(match2.ends);
			operators.addAll(match2.operators);
			termIds.addAll(match2.termIds);
		}

		public String asJSONString() {
			StringBuilder b = new StringBuilder();
			asJSONString(b);
			return b.toString();
		}

		public void add(ByteArrayWrapper s, ByteArrayWrapper e,
				TreeAxis operator, int termId) {
			starts.add(s);
			ends.add(e);
			operators.add(operator);
			termIds.add(termId);
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			Match result = new Match();
			for (ByteArrayWrapper start : starts) result.starts.add((ByteArrayWrapper) start.clone());
			for (ByteArrayWrapper end : ends) result.ends.add((ByteArrayWrapper) end.clone());
			result.operators.addAll(operators);
			result.termIds.addAll(termIds);
			return result;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < operators.size(); i++) {
				b.append(starts.get(i) + operators.get(i).name()
						+ ends.get(i) + ":" + termIds.get(i) + "\n");
			}
			return b.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Match)) {
				return false;
			}
			Match other = (Match) obj;
			return equalLists(starts, other.starts)
					&& equalLists(ends, other.ends)
					&& equalLists(operators, other.operators)
					&& equalLists(termIds, other.termIds);
		}

		private boolean equalLists(List<? extends Object> list1, List<? extends Object> list2) {
			if (list1.size() != list2.size()) return false; // we check size without null check coz the vars are always init in the constructor
			int i = 0;
			while (i < list1.size()) {
				if (!(list1.get(i).equals(list2.get(i))))
					return false;
				i++;
			}
			return true;
		}


		public List<MatchedPair> pairs() {
			if (starts.size() != ends.size() || ends.size() != operators.size() || operators.size() != termIds.size()) { //should not happen!
				return null;
			}
			List<MatchedPair> result = new ArrayList<MatchedPair>();
			int i = 0;
			while (i < starts.size()) {
				result.add(new MatchedPair(starts.get(i), ends.get(i), operators.get(i), termIds.get(i)));
				i++;
			}
			return result;
		}

		@Override
		public void asJSONString(StringBuilder builder) {
			final List<MatchedPair> pairs = pairs();
			builder.append("{\"m\":[");
			for (MatchedPair pair : pairs) {
				pair.asJSONString(builder);
				builder.append(",");
			}
			if (pairs.size() > 0) builder.deleteCharAt(builder.length() - 1);
			builder.append("]}");
		}
	}

	public class MatchedPair implements EfficientJSONAble {
		ByteArrayWrapper start;
		ByteArrayWrapper end;
		TreeAxis operator;
		int termId;

		public MatchedPair(ByteArrayWrapper start, ByteArrayWrapper end, TreeAxis operator, int termId) {
			this.start = start;
			this.end = end;
			this.operator = operator;
			this.termId = termId;
		}

		public String asJSONString() {
			StringBuilder b = new StringBuilder();
			asJSONString(b);
			return b.toString();
		}
		
		@Override
		public void asJSONString(StringBuilder builder) {
			builder.append("{\"s\":\"");
			for (byte b : start.data) {
				builder.append((b | 256) & 255);
				builder.append("_");
			}
			if (start.data.length > 0)
				builder.deleteCharAt(builder.length() - 1);
			builder.append("\",\"e\":\"");
			for (byte b : end.data) {
				builder.append((b | 256) & 255);
				builder.append("_");
			}
			if (end.data.length > 0)
				builder.deleteCharAt(builder.length() - 1);
			builder.append("\",\"o\":\"");
			builder.append(operator.id());
			builder.append("\",\"t\":\"");
			builder.append(termId);
			builder.append("\"}");
		}
	}

	/**
	 * This class encapsulates the position information of one node. ie. 4 bytes
	 * in the usual sense
	 * 
	 * @author sumukh
	 * 
	 */
	public class ByteArrayWrapper implements Cloneable {
		byte[] data;

		public ByteArrayWrapper(byte[] data) {
			this.data = data;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ByteArrayWrapper))
				return false;
			return Arrays.equals(data, ((ByteArrayWrapper) obj).data);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return new ByteArrayWrapper(Arrays.copyOf(data, data.length));
		}

		@Override
		public String toString() {
			return Arrays.toString(data);
		}

		public byte[] getData() {
			return data;
		}

		public NodeDataBuffer asNodeDataBuffer3() {
			NodeDataBuffer r = new NodeDataBuffer();
			r.rights[0] = data[NodeDataBuffer.RIGHT];
			r.lefts[0] = data[NodeDataBuffer.LEFT];
			r.parents[0] = data[NodeDataBuffer.PARENT];
			r.heights[0] = data[NodeDataBuffer.HEIGHT];
			r.size = 1;
			return r;
		}
	}

	interface EfficientJSONAble extends JSONAble {
		void asJSONString(StringBuilder builder);
	}

	List<Integer> indexToMatches(ByteArrayWrapper baw) {
		return index.get(baw);
	}

	List<Integer> indexToNewMatches(ByteArrayWrapper baw) {
		return newIndex.get(baw);
	}

	public static class MatchStatus {
		public Set<Match> matches;
		public boolean successful;
	}

	public static class ResultStatus {
		public Result result;
		public boolean successful;
	}

	public void removeNewMatchesFor(ByteArrayWrapper baw) {
		List<Integer> nmis = newIndex.get(baw);
		Collections.sort(nmis);
		for (int i = nmis.size() - 1; i >= 0; i--) {
			newMatches.remove(nmis.get(i));
		}
		newIndex.remove(baw);
	}

	public List<Match> newMatches() {
		return newMatches;
	}

	/**
	 * This adds onto the new matches and new index
	 * 
	 * @param ms
	 * @param keepOld
	 *            TODO
	 */
	public void addToMatches(Map<ByteArrayWrapper, List<Match>> ms,
			boolean keepOld) {
		for (ByteArrayWrapper key : ms.keySet()) {
			final List<Match> list = ms.get(key);
			if (newIndex.containsKey(key)) {
				final List<Integer> ind = newIndex.get(key);
				final List<Integer> newInd = new ArrayList<Integer>();
				for (Integer pos : ind) {
					final Match match1 = newMatches.get((int) pos);
					for (int i = 0; i < list.size(); i++) {
						final Match match2 = list.get(i);
						Match joined = new Match(match1, match2);
						if (i == 0) {
							if (keepOld) {
								newMatches.add(joined);
								newInd.add(pos);
								newInd.add(newMatches.size() - 1);
							} else {
								newMatches.set((int) pos, joined);
								newInd.add(pos);
							}
						} else {
							newMatches.add(joined);
							newInd.add(newMatches.size() - 1);
						}
					}
				}
				newIndex.put(key, newInd);
			}
		}
	}

	public void retainInNew(Set<ByteArrayWrapper> toRetain) {
		List<ByteArrayWrapper> toRemove = new ArrayList<ByteArrayWrapper>();
		for (ByteArrayWrapper key : newIndex.keySet()) {
			if (!toRetain.contains(key)) {
				toRemove.add(key);
			}
		}
		for (ByteArrayWrapper key : toRemove) {
			newIndex.remove(key);
		}
		if (toRemove.size() > 0) {
			// offset the deleted matches
			List<Match> newNewMatches = new ArrayList<Match>();
			for (ByteArrayWrapper key : newIndex.keySet()) {
				List<Integer> i = newIndex.get(key);
				List<Integer> ii = new ArrayList<Integer>();
				for (Integer j : i) {
					newNewMatches.add(newMatches.get((int) j));
					ii.add(newNewMatches.size() - 1);
				}
				newIndex.put(key, ii);
			}
			newMatches = newNewMatches;
		}
	}
}
