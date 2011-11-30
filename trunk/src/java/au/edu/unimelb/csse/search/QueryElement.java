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

import au.edu.unimelb.csse.search.join.TermJoinType;


/**
 * Features offered by all query elements
 * @author sumukh
 *
 */
public interface QueryElement {
	/**
	 * Used to initialize TermPositions and other variables
	 * @param reader
	 * @throws IOException
	 */
	void init(IndexReader reader) throws IOException;

	/**
	 * Moves to the next lowest document position
	 * @return false if reached end of docs; true otherwise
	 * @throws IOException
	 */
	boolean nextDoc() throws IOException;

	/**
	 * Position of current doc
	 * @return
	 */
	int doc();

	/**
	 * Skips to a position greater than or equal to doc
	 * @param doc the target doc position
	 * @return false if reached end of docs; true otherwise
	 * @throws IOException
	 */
	boolean skipTo(int doc) throws IOException;

	void setJoinType(TermJoinType joinType);

	StringBuilder toString(StringBuilder builder);
//	void setLookaheadOptimization();
}
