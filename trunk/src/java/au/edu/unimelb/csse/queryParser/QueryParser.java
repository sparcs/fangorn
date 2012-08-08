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
package au.edu.unimelb.csse.queryParser;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.FilterChunkElement;
import au.edu.unimelb.csse.search.Operator;
import au.edu.unimelb.csse.search.TermFilter;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreeTerm;

public class QueryParser {
	private String[] ts;
	private static final String FIELD = "sent";
	private static final Map<String, TreeAxis> axes = new HashMap<String, TreeAxis>();
	private static final Map<String, Operator> OperatorSyms = new HashMap<String, Operator>();
	private static final Map<String, Operator> Operators = new HashMap<String, Operator>();
	private int termId;
	static {
		axes.put(TreeAxis.DESCENDANT.symbol(), TreeAxis.DESCENDANT);
		axes.put(TreeAxis.CHILD.symbol(), TreeAxis.CHILD);
		axes.put(TreeAxis.ANCESTOR.symbol(), TreeAxis.ANCESTOR);
		axes.put(TreeAxis.PARENT.symbol(), TreeAxis.PARENT);
		axes.put(TreeAxis.FOLLOWING.symbol(), TreeAxis.FOLLOWING);
		axes.put(TreeAxis.IMMEDIATE_FOLLOWING.symbol(), TreeAxis.IMMEDIATE_FOLLOWING);
		axes.put(TreeAxis.FOLLOWING_SIBLING.symbol(), TreeAxis.FOLLOWING_SIBLING);
		axes.put(TreeAxis.IMMEDIATE_FOLLOWING_SIBLING.symbol(), TreeAxis.IMMEDIATE_FOLLOWING_SIBLING);
		axes.put(TreeAxis.PRECEDING.symbol(), TreeAxis.PRECEDING);
		axes.put(TreeAxis.IMMEDIATE_PRECEDING.symbol(), TreeAxis.IMMEDIATE_PRECEDING);
		axes.put(TreeAxis.PRECEDING_SIBLING.symbol(), TreeAxis.PRECEDING_SIBLING);
		axes.put(TreeAxis.IMMEDIATE_PRECEDING_SIBLING.symbol(), TreeAxis.IMMEDIATE_PRECEDING_SIBLING);

		OperatorSyms.put(Operator.AND.symbol(), Operator.AND);
		OperatorSyms.put(Operator.OR.symbol(), Operator.OR);
		OperatorSyms.put(Operator.NOT.symbol(), Operator.NOT);
		Operators.put(Operator.AND.text(), Operator.AND);
		Operators.put(Operator.OR.text(), Operator.OR);
		Operators.put(Operator.NOT.text(), Operator.NOT);
		Operators.putAll(OperatorSyms);
	}
	private static enum PREV_TOKEN {NONE, TERM, NOT, ANDorOR};

	public QueryParser(String[] tokens) {
		this.ts = tokens;
		this.termId = 0; //this is the number assigned to consecutive nodes in the query string; incremented after each term
	}

	public TreeExpr parse() throws ParseException {
		TreeExpr expr = new TreeExpr();
		int pos = 0;
		while (pos < ts.length) {
			pos = parseTerm(expr, pos);
		}
		return expr;
	}

	private int parseTerm(TreeExpr expr, int pos) throws ParseException {
		if (pos + 2 > ts.length) {
			throw new ParseException();
		}
		TreeAxis axis = axes.get(ts[pos]);
		if (axis == null) {
			throw new ParseException();
		}
		String next = ts[pos + 1];
		if (axes.containsKey(next) || next.equals(TermFilter.FILTER_OPEN) || next.equals(TermFilter.FILTER_CLOSE)
				|| OperatorSyms.containsKey(next)) {
			throw new ParseException();
		}
		if (pos + 2 == ts.length || !ts[pos + 2].equals(TermFilter.FILTER_OPEN)) {
			expr.addTerm(new TreeTerm(termId++, axis, new Term(FIELD, next)));
			return pos + 2;
		}
		return parseFilter(expr, axis, next, pos + 3);
	}

	private int parseFilter(TreeExpr expr, TreeAxis axis, String label, int pos)
			throws ParseException {
		int nextPos = pos;
		boolean isNot = false;
		PREV_TOKEN prev = PREV_TOKEN.NONE;
		TermFilter filter = null;
		Operator prevOperator = null;
		TreeExpr newExpr = new TreeExpr();
		int tid = this.termId++;
		while (nextPos < ts.length && !ts[nextPos].equals(TermFilter.FILTER_CLOSE)) {
			if (Operators.containsKey(ts[nextPos].toLowerCase())) {
				Operator o = Operators.get(ts[nextPos].toLowerCase());
				if ((prev.equals(PREV_TOKEN.NONE) && !o.equals(Operator.NOT))||
						(prev.equals(PREV_TOKEN.NOT) && o.equals(Operator.NOT))||
						(prev.equals(PREV_TOKEN.NOT) && (o.equals(Operator.AND) || o.equals(Operator.OR)))||
						(prev.equals(PREV_TOKEN.TERM) && o.equals(Operator.NOT))||
						(prev.equals(PREV_TOKEN.ANDorOR) && (o.equals(Operator.AND) || o.equals(Operator.OR)))) {
					throw new ParseException();
				}
				if (Operator.NOT.equals(o)) {
					isNot = true;
					prev = PREV_TOKEN.NOT;
				} else {
					filter = addExprToFilter(filter, prevOperator, isNot,
							newExpr);
					prevOperator = o;
					newExpr = new TreeExpr();
					isNot = false;
					prev = PREV_TOKEN.ANDorOR;
				}
				nextPos++;
				continue;
			}
			nextPos = parseTerm(newExpr, nextPos);
			prev = PREV_TOKEN.TERM;
		}
		if (!(nextPos < ts.length) || !prev.equals(PREV_TOKEN.TERM))
			throw new ParseException();
		nextPos++;// accounting for QueryParser.FC
		
		filter = addExprToFilter(filter, prevOperator, isNot, newExpr);
		expr.addTerm(new TreeTerm(tid, axis, new Term(FIELD, label), filter));
		return nextPos;
	}

	private TermFilter addExprToFilter(TermFilter filter,
			Operator prevOperator, boolean isNot, TreeExpr newExpr) {
		FilterChunkElement chunkElement = getFilterChunkElement(newExpr);
		if (filter == null) {
			filter = new TermFilter(chunkElement, isNot);
		} else {
			filter.addTerm(prevOperator, chunkElement, isNot);
		}
		return filter;
	}

	private FilterChunkElement getFilterChunkElement(TreeExpr newExpr) {
		if (newExpr.size() == 1) {
			return newExpr.getTerm(0);
		}
		return newExpr;
	}

	public void reset(String[] tokens) {
		this.ts = tokens;
	}
}
