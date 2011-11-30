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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class QueryBuilder {

	private final QueryParser parser;

	private final String qs;

	public QueryBuilder(String query) {
		String str = query.replaceAll("[ ]+", " ");
		this.qs = str.trim();
		parser = new QueryParser(tokens().toArray(new String[] {}));
	}

	public TreebankQuery parse(TermJoinType joinType, boolean useLookahead) throws ParseException {
		return new TreebankQuery(parser.parse(), joinType, useLookahead);
	}

	public TreeExpr getExpr() throws ParseException {
		return (TreeExpr) parser.parse();
	}

	/**
	 * todo *U*
	 * 
	 * @return
	 */
	List<String> tokens() {
		Pattern pattern = Pattern
				.compile("\\[|\\]|//|/|\\\\\\\\|\\\\|-->|->|<--|<-|==>|=>|<==|<=|\\&|\\||\\!|\\bAND\\b|\\bOR\\b|\\bNOT\\b");
		Matcher matcher = pattern.matcher(qs);
		List<String> tokens = new ArrayList<String>();
		int prevEnd = 0;
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			if (prevEnd != start) {
				addTokenIfValid(tokens, prevEnd, start);
			}
			addTokenIfValid(tokens, start, end);
			prevEnd = end;
		}
		if (prevEnd != qs.length()) {
			addTokenIfValid(tokens, prevEnd, qs.length());
		}
		return tokens;
	}

	private void addTokenIfValid(List<String> tokens, int start, int end) {
		final String s = qs.substring(start, end).trim();
		if (s.length() > 0) {
			tokens.add(s);
		}
	}
}
