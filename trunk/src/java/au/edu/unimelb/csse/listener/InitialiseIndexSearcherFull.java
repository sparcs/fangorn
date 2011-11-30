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
package au.edu.unimelb.csse.listener;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import au.edu.unimelb.csse.Corpora;

public class InitialiseIndexSearcherFull implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent event) {
		for (String dir : Corpora.INSTANCE.corporaDirs()) {
			destroySearcher(event, dir);
		}
	}

	private void destroySearcher(ServletContextEvent event,
			final String contextAttrName) {
		IndexSearcher searcher = (IndexSearcher) event.getServletContext()
				.getAttribute(contextAttrName);
		if (searcher != null) {
			try {
				searcher.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void contextInitialized(ServletContextEvent event) {
		for (String dir : Corpora.INSTANCE.corporaDirs()) {
			initSearcher(event, "index" + File.separator + dir, dir);
		}
	}

	private void initSearcher(ServletContextEvent event,
			final String resourceLocation, final String contextAttrName) {
		try {
			IndexSearcher searcher = null;
			File f = new File(resourceLocation);
			searcher = new IndexSearcher(FSDirectory.getDirectory(f));
			ServletContext servletContext = event.getServletContext();
			servletContext.setAttribute(contextAttrName, searcher);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
