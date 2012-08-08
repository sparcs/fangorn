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
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.IndexSearcher;

import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.complete.AllResults;
import au.edu.unimelb.csse.search.complete.Result;

public class PagingServlet extends QueryServletFull {
	private static final long serialVersionUID = 5820413883678723422L;
	private static final Logger logger = Logger
			.getLogger(PagingServlet.class.getName());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		String pageNumParam = req.getParameter("j");
		String hash = req.getParameter("h");
		String prevQuery = req.getParameter("p");
		String prevCorpus = req.getParameter("c");
		String docNumsParam = req.getParameter("d");
		String totalHits = req.getParameter("t");
		if ((pageNumParam == null || hash == null || prevQuery == null
				|| docNumsParam == null || prevCorpus == null || totalHits == null)
				|| (hashValue(prevQuery, prevCorpus, docNumsParam, totalHits) != Integer
						.parseInt(hash))) {
			req.setAttribute("error", "Oops! An error has occurred.");
			logger.warning("Error searching: " + prevQuery + ". Incorrect hidden parameters in page.");
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
			return;
		}
		int requestedPage = Integer.valueOf(pageNumParam);

		String[] docStrings = docNumsParam.split(" ");
		int[] docNums = new int[docStrings.length];
		for (int i = 0; i < docStrings.length; i++) {
			docNums[i] = Integer.valueOf(docStrings[i]);
		}

		if (requestedPage - 1 > docNums.length) {
			req.setAttribute("error", "Oops! An error has occurred.");
			logger.warning("Error searching: " + prevQuery + ". Requested page exceeds number of result pages.");
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
			return;
		}

		String corpus = getCorpus(prevCorpus);
		req.setAttribute("corpus", corpus);
		res.setCharacterEncoding("UTF-8");

		IndexSearcher searcher = getSearcher(corpus, req, res);
		if (searcher == null) {
			req.setAttribute("error", "Oops! An error has occurred. Search engine not initialized.");
			logger.warning("Error searching: " + prevQuery + ". Search engine not initialized.");
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
			return;
		}

		String queryView = getReturnQuery(prevQuery);
		req.setAttribute("query-view", queryView);

		try {
			TreebankQuery tq = getTreebankQuery(req, res, corpus, prevQuery, pageNumParam);
			long start = System.nanoTime();
			SimpleHitCollector hitCollector = null;
			if (requestedPage == 1) {
				hitCollector = new SimpleHitCollector(MAX_RESULTS_PER_PAGE);
				searcher.search(tq, hitCollector);
			} else if (requestedPage % 10 < 6 && requestedPage % 10 > 1) {
				hitCollector = new SimpleHitCollector(MAX_RESULTS_PER_PAGE);
				searcher.truncatedSearch(tq, hitCollector,
						MAX_RESULTS_PER_PAGE, docNums[requestedPage - 2]);
			} else {
				if (requestedPage > docNums.length - 5) {
					int hitsToLoad = (docNums.length - requestedPage + 11)
							* MAX_RESULTS_PER_PAGE;
					hitCollector = new SimpleHitCollector(hitsToLoad);
					searcher.truncatedSearch(tq, hitCollector, hitsToLoad,
							docNums[requestedPage - 2]);
					int[] docs = hitCollector
							.lastDocOfEachPage(MAX_RESULTS_PER_PAGE);
					StringBuilder builder = new StringBuilder(docNumsParam);
					for (int i = docNums.length - requestedPage + 1
							; i < docs.length; i++) {
						builder.append(" ");
						builder.append(docs[i]);
					}
					docNumsParam = builder.toString();
				} else {
					// it has been previously loaded
					hitCollector = new SimpleHitCollector(MAX_RESULTS_PER_PAGE);
					searcher.truncatedSearch(tq, hitCollector,
							MAX_RESULTS_PER_PAGE, docNums[requestedPage - 2]);
				}
			}
			int numberOfResults = hitCollector.totalHits < MAX_RESULTS_PER_PAGE ? hitCollector.totalHits
					: MAX_RESULTS_PER_PAGE;
			AllResults allResults = new AllResults(hitCollector.hits,
					numberOfResults, tq);
			Result[] resultMeta = allResults.collect(searcher);
			long end = System.nanoTime();

			setSearchTimeAttribute(req, start, end);
			req.setAttribute("totalhits", Integer.valueOf(totalHits));
			req.setAttribute("pagenum", requestedPage);
			req.setAttribute("docnums", docNumsParam);
			req.setAttribute("hash", hashValue(prevQuery, prevCorpus, docNumsParam, totalHits)); //should hash prevQuery and not queryview
			String[] results = new String[numberOfResults];
			for (int i = 0; i < numberOfResults; i++) {
				results[i] = searcher.doc(hitCollector.hits[i]).get("sent")
						.trim();
			}
			req.setAttribute("results", results);
			req.setAttribute("metadata", resultMeta);
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/results.jsp");
			view.forward(req, res);
		} catch (ParseException e) {
			req.setAttribute("error", "Sorry! Cannot parse your query");
			logger.info("Q=\"" + prevQuery + "\";C=\"" + corpus + "\";S=\"no\"");
			RequestDispatcher view = req.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
		} catch (Exception e) {
			req.setAttribute("error", "Oops! An error has occurred. "
					+ e.getMessage() + ". The administrator will be informed.");
			logger.warning("Error searching: " + prevQuery);
			RequestDispatcher view = req
					.getRequestDispatcher("/WEB-INF/error.jsp");
			view.forward(req, res);
		}

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.sendRedirect("index.html");
	}

}
