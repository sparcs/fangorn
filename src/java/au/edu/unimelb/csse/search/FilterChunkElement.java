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

import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.complete.Result;

/**
 * Elements that can appear within a FilterChunk
 * 
 * @author sumukh
 * 
 */
public interface FilterChunkElement extends QueryElement {
	/**
	 * This method allow the filter element to be evaluated in the reverse
	 * order. i.e. For expressions: it evaluated from the last term to the first
	 * term by finding the conjugate of the actual operator present
	 * 
	 * @param data
	 * @param payloadBuffer
	 * @param positions
	 * @return
	 * @throws IOException
	 */
	public boolean reverseStructureMatch(NodeDataBuffer data,
			byte[] payloadBuffer, int[] positions) throws IOException;

//	/**
//	 * Checks if the structural constraints in the query are satisfied at this
//	 * level for the contents of the buffer
//	 * 
//	 * @param buffer
//	 *            contains the valid occurrences of previous term
//	 * @param payloadBuffer
//	 *            payload buffer of current term
//	 * @param lookahead
//	 *            TODO
//	 * @return true if match is found; false otherwise
//	 * @throws IOException
//	 */
//	boolean structureMatch(NodeDataBuffer buffer, byte[] payloadBuffer,
//			int[] positionBuffer, LookaheadOptimization lookahead)
//			throws IOException;

	public TreeAxis axis();
	
	public void setLookaheadOptimization(TreeAxis axis);

	Result allStructureMatch(Result result, NodeDataBuffer data,
			byte[] payloadBuffer, int[] postionBuffer) throws IOException;
}
