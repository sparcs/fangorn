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
package au.edu.unimelb.csse.search.join;

import java.io.IOException;

import org.apache.lucene.index.TermPositions;

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.NodeDataBuffer;
import au.edu.unimelb.csse.search.complete.Result;

public interface TermJoinAware {

	/**
	 * This is a fast term joining method that checks if a term occurrence
	 * matches any of the previous node's occurrences and returns the number of
	 * occurrences in the current term that match the previous term
	 * 
	 * @param term
	 *            is the TermPositions object of the current term
	 * @param previous
	 *            is the object containing the previous term's valid occurrences
	 * @param payloadBuffer
	 *            is a buffer used in the join operation
	 * @param positionsBuffer
	 *            is a buffer used in the join operation
	 * @param optimization TODO
	 * @param stopAtFirst TODO
	 * @return the number of matches at this term
	 * @throws IOException
	 */
	int joinTerm(TermPositions term, NodeDataBuffer previous,
			byte[] payloadBuffer, int[] positionsBuffer, LookaheadOptimization optimization, boolean stopAtFirst) throws IOException;

	/**
	 * This method finds all the matching results at the current term
	 * 
	 * @param result
	 *            stores the results so far
	 * @param term
	 *            is the TermPositions object of the current term
	 * @param previous
	 *            is the object containing the previous term's valid occurrences
	 * @param payloadBuffer
	 *            is a buffer used in the join operation
	 * @param positionsBuffer
	 *            is a buffer used in the join operation
	 * @param axis
	 *            is the TreeAxis object containing this joinAware instance;
	 *            this is used to avoid a cyclic dependency between
	 *            TermJoinAware and TreeAxis
	 * @return returns all matches for the join
	 * @throws IOException
	 */
	Result joinTermAllMatches(Result result, TermPositions term,
			NodeDataBuffer previous, byte[] payloadBuffer,
			int[] positionsBuffer, TreeAxis axis) throws IOException;

}
