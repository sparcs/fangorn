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

public interface LookaheadOptimization {
	public static final int WRITE = 1;
	public static final int IGNORE = 0;
	public static final int OVERWRITE = -1;
	
	LookaheadOptimization NONE = new LookaheadOptimization() {

		@Override
		public int check(byte[] payloadBuffer, int i) {
			return WRITE;
		}

	};
	LookaheadOptimization RETAIN_ANCESTOR = new LookaheadOptimization() {

		@Override
		public int check(byte[] payloadBuffer, int pos) {
			int ind = pos * NodeDataBuffer.PAYLOAD_LENGTH;
			int nextInd = (pos + 1) * NodeDataBuffer.PAYLOAD_LENGTH;
			if (JoinLogic.DESCENDANT.joinBytesWithBytes(payloadBuffer, ind, payloadBuffer, nextInd)) {
				return IGNORE;
			}
			return WRITE;
		}

	};
	LookaheadOptimization RETAIN_CHILDREN = new LookaheadOptimization() {

		@Override
		public int check(byte[] payloadBuffer, int pos) {
			int ind = pos * NodeDataBuffer.PAYLOAD_LENGTH;
			int nextInd = (pos + 1) * NodeDataBuffer.PAYLOAD_LENGTH;
			if (JoinLogic.ANCESTOR.joinBytesWithBytes(payloadBuffer, ind, payloadBuffer, nextInd)) {
				return OVERWRITE;
			}
			return WRITE;
		}
		
	};
	LookaheadOptimization RETAIN_LEFTMOST = new LookaheadOptimization() {

		@Override
		public int check(byte[] payloadBuffer, int pos) {
			int ind = pos * NodeDataBuffer.PAYLOAD_LENGTH;
			int nextInd = (pos + 1) * NodeDataBuffer.PAYLOAD_LENGTH;
			if (JoinLogic.FOLLOWING.joinBytesWithBytes(payloadBuffer, ind, payloadBuffer, nextInd)) {
				return IGNORE;
			}
			return WRITE;
		}
		
	};
	LookaheadOptimization RETAIN_RIGHTMOST = new LookaheadOptimization() {

		@Override
		public int check(byte[] payloadBuffer, int pos) {
			int ind = pos * NodeDataBuffer.PAYLOAD_LENGTH;
			int nextInd = (pos + 1) * NodeDataBuffer.PAYLOAD_LENGTH;
			if (JoinLogic.PRECEDING.joinBytesWithBytes(payloadBuffer, ind, payloadBuffer, nextInd)) {
				return OVERWRITE;
			}
			return WRITE;
		}
		
	};

	/**
	 * This method checks if the contents of the position buffer at position pos
	 * and postions pos + 1 can be optimised in any way.
	 * 
	 * @param payloadBuffer
	 * @param i
	 * @return 0 if pos + 1 should be ignored
	 * 1 if pos + 1 should be added to the buffer
	 * -1 if pos + 1 should overwrite 
	 */
	int check(byte[] payloadBuffer, int pos);
}
