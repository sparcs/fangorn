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

abstract class AbstractJoin implements TermJoinAware {
	protected JoinLogicAware logic;

	AbstractJoin(JoinLogicAware logic) {
		this.logic = logic;
	}

	abstract boolean shouldLoadPayload(int pos, NodeDataBuffer data);

	protected int u(byte b) {
		return (b | 256) & 255;
	}

	protected byte[] getByteArray(byte[] payloadBuffer, int bufferPtr) {
		byte right = payloadBuffer[bufferPtr + NodeDataBuffer.RIGHT];
		byte left = payloadBuffer[bufferPtr + NodeDataBuffer.LEFT];
		byte height = payloadBuffer[bufferPtr + NodeDataBuffer.HEIGHT];
		byte parent = payloadBuffer[bufferPtr + NodeDataBuffer.PARENT];
		byte[] end = new byte[] { right, left, height, parent };
		return end;
	}

	protected void overwritePrevPayloadBuf(byte[] payloadBuffer,
			int currentPos) {
		System.arraycopy(payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
				* currentPos, payloadBuffer, NodeDataBuffer.PAYLOAD_LENGTH
				* (currentPos - 1), NodeDataBuffer.PAYLOAD_LENGTH);
	}

	protected void overwritePrevPositionPayloadBuf(byte[] payloadBuffer,
			int[] positionsBuffer, int currentPos) {
		positionsBuffer[currentPos - 1] = positionsBuffer[currentPos];
		overwritePrevPayloadBuf(payloadBuffer, currentPos);
	}
}
