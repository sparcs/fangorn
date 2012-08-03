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
package au.edu.unimelb.csse.exp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.queryParser.QueryBuilder;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreeTerm;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class GenerateQueries {
	private static final String[] operators = new String[] { "//", "\\\\", "/",
			"\\", "-->", "->", "<-", "<--", "==>", "<==", "=>", "<=" };
	private static final int MIN_DOC_FREQ = 10;
	private IndexSearcher reader;
	private Set<String> noResult = new HashSet<String>();
	private Set<String> generated2 = new HashSet<String>();
	private Set<String> generated3 = new HashSet<String>();
	private Set<String> generated4 = new HashSet<String>();
	private Set<String> generatedFilter = new HashSet<String>();
	private ArrayList<String> textLabels = new ArrayList<String>();
	private SimpleHitCollector hc = new SimpleHitCollector(1);
	private String pathToIndex;

	public GenerateQueries(String pathToIndex) throws CorruptIndexException,
			IOException {
		this.pathToIndex = pathToIndex;
		this.reader = new IndexSearcher(pathToIndex);
	}

	public static void main(String[] args) throws CorruptIndexException,
			IOException {
		if (args.length != 1) {
			System.out.println("usage: java " + GenerateQueries.class.getName()
					+ " <path_to_index>");
			System.exit(1);
		}

		GenerateQueries gq = new GenerateQueries(args[0]);
		gq.run("queries" + System.currentTimeMillis());
	}

	private void run(String generatedFileName) throws IOException {
		getAllTerms();
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				generatedFileName));

		System.out.println("Found " + textLabels.size()
				+ " number of text labels.");

		reader = new IndexSearcher(pathToIndex);
		System.out.println("Generating 50 queries of length 2");
		for (int i = 0; i < 50; i++) {
			String s = null;
			do {
				s = generateExprOfLength(2);
			} while (generated2.contains(s));
			generated2.add(s);
			// System.out.println(s);
		}
		System.out.println("");
		print(writer, generated2);
		System.out.println("Generating 50 queries of length 3");
		for (int i = 0; i < 50; i++) {
			String s = null;
			do {
				s = generateExprOfLength(3);
			} while (generated3.contains(s));
			generated3.add(s);
			// System.out.println(s);
		}
		System.out.println("");
		print(writer, generated3);
		System.out.println("Generating 50 queries of length 4");
		for (int i = 0; i < 50; i++) {
			String s = null;
			do {
				s = generateExprOfLength(4);
			} while (generated4.contains(s));
			generated4.add(s);
			// System.out.println(s);
		}
		System.out.println("");
		print(writer, generated4);
		System.out.println("Generating 20 filter queries");
		for (int i = 0; i < 5; i++) {
			for (int j = 1; j < 5; j++) {
				String s = null;
				do {
					s = generateFilterQueryOfLength(j, 4);
					if (s == null) {
						System.out
								.println("breaking out after reaching overflow limit");
						break;
					}
				} while (generatedFilter.contains(s));
				generatedFilter.add(s);
			}
		}

		print(writer, generatedFilter);
		writer.close();
	}

	private void print(BufferedWriter writer, Set<String> set)
			throws IOException {
		for (String q : set) {
			writer.write(q + "\n");
		}
	}

	private void getAllTerms() throws IOException {
		final TermEnum terms = reader.getIndexReader().terms();
		boolean next = terms.next();
		while (next != false) {
			Term term = terms.term();
			if (reader.docFreq(term) > MIN_DOC_FREQ) {
				String original = term.text().trim();
				if (original.equals("S")) {
					textLabels.add(original);
					next = terms.next();
					continue;
				}
				if (original.length() < 2) {
					next = terms.next();
					continue;
				}
				if (original.length() == 2) {
					if (original.toUpperCase().equals(original)) {
						textLabels.add(original);
					} else {
						next = terms.next();
						continue;
					}
				}
				int uppercases = 0;
				for (int i = 0; i < original.length()
						&& uppercases <= original.length() / 2; i++) {
					if (Character.isUpperCase(original.charAt(i))) {
						uppercases++;
					}
				}
				if (uppercases > original.length() / 2) {
					textLabels.add(original);
				}
			}
			next = terms.next();
		}
	}

	private String generateFilterQueryOfLength(int numberOfFilterExpressions,
			int maxLengthOfExprs) throws IOException {
		int numberOfResults = 0;
		String query = null;
		int iterations = 0;
		do {
			int lengthOfHead = getRandomNoGT0LTEQParam(maxLengthOfExprs);
			String head = generateExprOfLength(lengthOfHead);
			String filter = null;
			for (int i = 0; i < numberOfFilterExpressions; i++) {
				String operator = Math.random() < 0.5 ? " OR " : " AND ";
				boolean isNot = Math.random() < 0.2 ? true : false;
				int lengthOfExpr = getRandomNoGT0LTEQParam(maxLengthOfExprs);
				String expr = generateExprOfLength(lengthOfExpr);
				if (filter == null) {
					filter = "[" + expr;
				} else {
					if (isNot) {
						filter += operator + "!" + expr;
					} else {
						filter += operator + expr;
					}
				}
			}
			if (filter != null) {
				filter += "]";
				head += filter;
			}
			query = head;
			QueryBuilder builder = new QueryBuilder(query);
			TreebankQuery tq;
			try {
				tq = builder.parse(TermJoinType.EARLY_STOP_WITH_FC, false);
			} catch (ParseException e) {
				iterations++;
				continue;
			}
			hc.reset();
			reader.search(tq, hc);
			numberOfResults = hc.totalHits;
			iterations++;
		} while (numberOfResults > 0 && iterations < 1000000);
		if (!(iterations < 1000000))
			return null;
		return query;
	}

	private int getRandomNoGT0LTEQParam(int lessThanEqual) {
		int len = 0;
		while (len != 0) {
			len = (int) Math.ceil(Math.random() * lessThanEqual);
		}
		return len;
	}

	private String generateExprOfLength(int len) throws IOException {
		TreebankQuery q = getFirstTerm();
		int numberOfResults = 0;
		String prev = "";
		String query = q.toString();
		for (int i = 1; i < len; i++) {
			int trials = 0;
			numberOfResults = 0;
			String newQuery = null;
			while (numberOfResults == 0 && trials < 10000) {
				try {
					if (i == 1 && !query.startsWith("//")) {
						newQuery = query
								+ (Math.random() < 0.5 ? operators[2]
										: operators[0]) + getRandomTerm();
					} else {
						newQuery = query + getRandomOperator()
								+ getRandomTerm();
					}
					QueryBuilder builder = new QueryBuilder(newQuery);
					q = builder.parse(TermJoinType.EARLY_STOP_WITH_FC, false);
					if (noResult.contains(q.toString())) {
						System.out.println("Noresult contains " + q.toString());
						trials++;
						continue;
					}
					hc.reset();
					reader.search(q, hc);
					numberOfResults = hc.totalHits;
					if (numberOfResults == 0) {
						System.out.println("No results for " + newQuery);
						noResult.add(q.toString());
					}
				} catch (ParseException e) {
					System.out.println("Error parsing " + newQuery);
					trials++;
					continue;
				}
			}
			if (!(trials < 10000)) {
				if (i == 1) {
					q = getFirstTerm();
					query = q.toString();
					prev = "";
					i--;
				} else {
					query = prev;
					i--;
				}
			} else {
				prev = query;
				query = newQuery;
			}
		}
		return query;
	}

	private TreebankQuery getFirstTerm() throws IOException {
		TreebankQuery q = null;
		boolean firstOperatorIsDescendant = false;
		int firstTrials = 0;
		int nor = 0;
		while (nor == 0 && firstTrials < 1000) {
			firstOperatorIsDescendant = true;
			if (Math.random() < 0.3) {
				firstOperatorIsDescendant = false;
			}
			TreeTerm term = new TreeTerm(
					0, firstOperatorIsDescendant ? TreeAxis.DESCENDANT
									: TreeAxis.CHILD, new Term("sent", getRandomTerm()));
			if (noResult.contains(term.toString())) {
				System.out.println("Noresult contains " + term.toString()
						+ "; starting again");
				firstTrials++;
				continue;
			}
			TreeExpr r = new TreeExpr();
			r.addTerm(term);
			q = new TreebankQuery(r);
			hc.reset();
			reader.search(q, hc);
			nor = hc.totalHits;
			if (nor == 0) {
				System.out.println("Found no results for " + q.toString()
						+ "; starting again");
				noResult.add(q.toString());
			}
		}
		return q;
	}

	private String getRandomTerm() {
		int pos = (int) Math.floor(Math.random() * textLabels.size());
		return textLabels.get(pos);
	}

	private String getRandomOperator() {
		int pos = (int) Math.floor(Math.random() * operators.length);
		return operators[pos];
	}
}
