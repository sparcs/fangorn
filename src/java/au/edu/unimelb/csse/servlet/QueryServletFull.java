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
package au.edu.unimelb.csse.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.IndexSearcher;

import au.edu.unimelb.csse.Corpora;
import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.queryParser.QueryBuilder;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.complete.AllResults;
import au.edu.unimelb.csse.search.complete.Result;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class QueryServletFull extends HttpServlet {
	private static final long serialVersionUID = -946822334880299336L;
	protected static final int MAX_RESULTS_PER_PAGE = 10;
	private static final Logger logger = Logger
			.getLogger(QueryServletFull.class.getName());
	protected static final String DEFAULT_CORPUS = Corpora.INSTANCE
			.getDefaultCorpus();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		// req.setCharacterEncoding("utf-16");
		final String corpusParam = req.getParameter("corpus");
		String corpus = getCorpus(corpusParam);
		req.setAttribute("corpus", corpus);
		res.setCharacterEncoding("UTF-8");

		IndexSearcher searcher = getSearcher(corpus, req, res);
		if (searcher == null)
			return;

		String query = getQuery(req, res);
		if (query == null)
			return;

		String queryView = getReturnQuery(query);
		req.setAttribute("query-view", queryView);

		try {
			TreebankQuery tq = getTreebankQuery(req, res, corpus, query, null);
			SimpleHitCollector hitCollector = new SimpleHitCollector(100);
			long start = System.nanoTime();
			searcher.search(tq, hitCollector);
			int numberOfResults = hitCollector.totalHits < MAX_RESULTS_PER_PAGE ? hitCollector.totalHits
					: MAX_RESULTS_PER_PAGE;
			AllResults allResults = new AllResults(hitCollector.hits,
					numberOfResults, tq);
			Result[] resultMeta = allResults.collect(searcher);
			long end = System.nanoTime();
			setSearchTimeAttribute(req, start, end);

			req.setAttribute("totalhits", hitCollector.totalHits);
			String[] results = new String[numberOfResults];
			for (int i = 0; i < numberOfResults; i++) {
				results[i] = searcher.doc(hitCollector.hits[i]).get("sent")
						.trim();
			}
			req.setAttribute("results", results);
			req.setAttribute("metadata", resultMeta);

			// attributes for pagination
			int[] docNumInts = hitCollector
					.lastDocOfEachPage(MAX_RESULTS_PER_PAGE);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < docNumInts.length; i++) {
				sb.append(docNumInts[i] + " ");
			}
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}
			req.setAttribute("pagenum", 1);
			final String docNums = sb.toString();
			req.setAttribute("docnums", docNums);
			req.setAttribute("hash", hashValue(query, corpusParam, docNums,
					String.valueOf(hitCollector.totalHits))); //should hash value of `query' and not `queryview' 
			RequestDispatcher view = req.getRequestDispatcher("/WEB-INF/results.jsp");
			view.forward(req, res);
		} catch (ParseException e) {
			req.setAttribute("error", "Sorry! Cannot parse your query");
			logger.info("Q=\"" + query + "\";C=\"" + corpus + "\";S=\"no\"");
			RequestDispatcher view = req.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
		} catch (Exception e) {
			req.setAttribute("error", "Oops! An error has occurred. "
					+ e.getMessage() + ". The administrator will be informed.");
			logger.severe("Error searching: " + query);
			logger.severe(e.getMessage());
			RequestDispatcher view = req.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
		}
	}

	protected IndexSearcher getSearcher(String corpus, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		IndexSearcher searcher = (IndexSearcher) getServletContext()
				.getAttribute(corpus);
		if (searcher == null) {
			req
					.setAttribute(
							"error",
							"Corpus loading error. Please contact the Administrator at sghodke@csse.unimelb.edu.au");
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
		}
		return searcher;
	}

	protected void setSearchTimeAttribute(HttpServletRequest req, long start,
			long end) {
		double searchTimeMS = (end - start) / 1000000;
		if (searchTimeMS < 1) {
			req.setAttribute("searchtime", (end - start) / 1000 + " &#181;s");
		} else if (searchTimeMS > 1000) {
			req.setAttribute("searchtime", searchTimeMS / 1000 + " s");
		} else {
			req.setAttribute("searchtime", (end - start) / 1000000 + " ms");
		}
	}

	protected String getQuery(HttpServletRequest req, HttpServletResponse res)
			throws UnsupportedEncodingException, ServletException, IOException {
		final String query = req.getParameter("query");

		if (query == null || query.trim().equals("")) {
			doGet(req, res);
		}
		return query;
	}

	protected TreebankQuery getTreebankQuery(HttpServletRequest req,
			HttpServletResponse res, String corpus, String query, String page)
			throws ServletException, IOException, ParseException {
		TreebankQuery tq = null;
		QueryBuilder builder = new QueryBuilder(query);
		tq = builder.parse(TermJoinType.SIMPLE_WITH_FC, false);
		logger.fine(req.getRemoteAddr());
		if (page == null) {
			logger.info("Q=\"" + tq.toString() + "\";C=\"" + corpus
					+ "\";S=\"yes\"");
		} else {
			logger.info("Q=\"" + tq.toString() + "\";C=\"" + corpus
					+ "\";PAGE=\"" + page + "\"");
		}
/*
		if (tq == null) {
			req.setAttribute("error", "Server Error! Contact Administrator");
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
			return null;
		}
*/		return tq;
	}

	String getReturnQuery(String query)
			throws UnsupportedEncodingException {
		return query.replace("&", "&amp;").replace("\\", "\\\\").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	protected String getCorpus(String corpus) {
		if (Corpora.INSTANCE.corporaDirs().contains(corpus))
			return corpus;
		return DEFAULT_CORPUS;
	}

	protected int hashValue(String prevQuery, String prevCorpus,
			String docNums, String totalHits) {
		return prevQuery.hashCode() * 3 + prevCorpus.hashCode() * 7
				+ docNums.hashCode() * 11 + totalHits.hashCode() * 5;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.sendRedirect("index.html");
	}
}
