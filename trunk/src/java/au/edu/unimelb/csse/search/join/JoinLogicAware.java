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

import au.edu.unimelb.csse.search.NodeDataBuffer;

public interface JoinLogicAware {

	/**
	 * Joins the data in NodeDataBuffer at dataPtr with the contents of buffer
	 * array at bufferPtr
	 * 
	 * @param data
	 *            this contains the data of the first term
	 * @param dataPtr
	 *            points to the first term's positions
	 * @param buffer
	 *            this contains the data of the second term
	 * @param bufferPtr
	 *            start location of the second term's positions
	 * @return
	 */
	boolean joinBufWithBytes(NodeDataBuffer data, int dataPtr, byte[] buffer,
			int bufferPtr);

	/**
	 * Joins two byte arrays containing data in the order specified by
	 * NodeDataBuffer.RIGHT, NodeDataBuffer.LEFT, NodeDataBuffer.HEIGHT,
	 * NodeDataBuffer.PARENT
	 * 
	 * @param start
	 *            contains start bytes
	 * @param startStartPos
	 *            TODO
	 * @param end
	 *            contains end bytes
	 * @param endStartPos
	 *            TODO
	 * @return
	 */
	boolean joinBytesWithBytes(byte[] start, int startStartPos, byte[] end,
			int endStartPos);

	boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
			NodeDataBuffer buffer2, int pos2);
}
