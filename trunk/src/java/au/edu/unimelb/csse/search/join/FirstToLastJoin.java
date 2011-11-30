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

abstract class FirstToLastJoin extends AbstractJoin {

	FirstToLastJoin(JoinLogicAware logic) {
		super(logic);
	}

	@Override
	boolean shouldLoadPayload(int pos, NodeDataBuffer data) {
		return pos > data.firstPosition();
	}

	@Override
	public Result joinTermAllMatches(Result result, TermPositions term,
			NodeDataBuffer previous, byte[] payloadBuffer,
			int[] positionsBuffer, TreeAxis axis) throws IOException {
		int freq = term.freq();
		int bufferPtr = 0;
		int validInstances = 0;

		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionsBuffer[validInstances] = term.nextPosition();
			int position = positionsBuffer[validInstances];
			if (shouldLoadPayload(position, previous)) {
				bufferPtr = NodeDataBuffer.PAYLOAD_LENGTH * validInstances;
				term.getPayload(payloadBuffer, bufferPtr);
				boolean updatedValidInstanceFlag = false;
				for (int j = 0; j < previous.size()
						&& validInstances < NodeDataBuffer.BUFFER_SIZE
						&& position >= previous.positions[j]; j++) {
					byte[] start = previous.getAsBytes(j);
					if (logic.joinBytesWithBytes(start, 0, payloadBuffer,
							bufferPtr)) {
						if (!updatedValidInstanceFlag) {
							validInstances++;
							updatedValidInstanceFlag = true;
						}
						byte[] end = getByteArray(payloadBuffer, bufferPtr);
						result.addNew(start, end, axis);
					}
				}
			}
		}
		return result;
	}
}

class SimpleFTLJoin extends FirstToLastJoin {

	SimpleFTLJoin(JoinLogicAware logic) {
		super(logic);
	}

	@Override
	public int joinTerm(TermPositions term, NodeDataBuffer previous,
			byte[] payloadBuffer, int[] positionsBuffer,
			LookaheadOptimization optimization, boolean stopAtFirst)
			throws IOException {
		int freq = term.freq();
		int bufferPtr = 0;
		int validInstances = 0;
		int val;

		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			term.nextPosition();
			bufferPtr = NodeDataBuffer.PAYLOAD_LENGTH * validInstances;
			term.getPayload(payloadBuffer, bufferPtr);
			val = LookaheadOptimization.WRITE;
			if (!optimization.equals(LookaheadOptimization.NONE)
					&& validInstances > 0) {
				val = optimization.check(payloadBuffer, validInstances - 1);
				if (val == LookaheadOptimization.IGNORE)
					continue;
			}
			for (int j = 0; j < previous.size()
					&& validInstances < NodeDataBuffer.BUFFER_SIZE; j++) {
				if (logic.joinBufWithBytes(previous, j, payloadBuffer,
						bufferPtr)) {
					if (val == LookaheadOptimization.WRITE) {
						validInstances++;
					} else if (val == LookaheadOptimization.OVERWRITE) {
						overwritePrevPayloadBuf(payloadBuffer, validInstances);
					}
					if (stopAtFirst)
						return 1;
					break;
				}
			}
		}
		return validInstances;
	}
}

class SimpleFTLWithFirstCheckJoin extends FirstToLastJoin {

	SimpleFTLWithFirstCheckJoin(JoinLogicAware logic) {
		super(logic);
	}

	@Override
	public int joinTerm(TermPositions term, NodeDataBuffer previous,
			byte[] payloadBuffer, int[] positionsBuffer,
			LookaheadOptimization optimization, boolean stopAtFirst)
			throws IOException {
		int freq = term.freq();
		int bufferPtr = 0;
		int validInstances = 0;
		int val;

		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionsBuffer[validInstances] = term.nextPosition();
			if (shouldLoadPayload(positionsBuffer[validInstances], previous)) {
				bufferPtr = NodeDataBuffer.PAYLOAD_LENGTH * validInstances;
				term.getPayload(payloadBuffer, bufferPtr);
				val = LookaheadOptimization.WRITE;
				if (!optimization.equals(LookaheadOptimization.NONE)
						&& validInstances > 0) {
					val = optimization.check(payloadBuffer, validInstances - 1);
					if (val == LookaheadOptimization.IGNORE)
						continue;
				}
				for (int j = 0; j < previous.size()
						&& validInstances < NodeDataBuffer.BUFFER_SIZE; j++) {
					if (logic.joinBufWithBytes(previous, j, payloadBuffer,
							bufferPtr)) {
						if (val == LookaheadOptimization.WRITE) {
							validInstances++;
						} else if (val == LookaheadOptimization.OVERWRITE) {
							overwritePrevPositionPayloadBuf(payloadBuffer,
									positionsBuffer, validInstances);
						}

						if (stopAtFirst)
							return 1;
						break;
					}
				}
			}
		}
		return validInstances;
	}

}

class EarlyStopFTLJoin extends FirstToLastJoin {

	EarlyStopFTLJoin(JoinLogicAware logic) {
		super(logic);
	}

	@Override
	public int joinTerm(TermPositions term, NodeDataBuffer previous,
			byte[] payloadBuffer, int[] positionsBuffer,
			LookaheadOptimization optimization, boolean stopAtFirst)
			throws IOException {
		int freq = term.freq();
		int bufferPtr = 0;
		int validInstances = 0;
		int val;

		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionsBuffer[validInstances] = term.nextPosition();
			bufferPtr = NodeDataBuffer.PAYLOAD_LENGTH * validInstances;
			term.getPayload(payloadBuffer, bufferPtr);
			val = LookaheadOptimization.WRITE;
			if (!optimization.equals(LookaheadOptimization.NONE)
					&& validInstances > 0) {
				val = optimization.check(payloadBuffer, validInstances - 1);
				if (val == LookaheadOptimization.IGNORE)
					continue;
			}
			for (int j = 0; j < previous.size()
					&& validInstances < NodeDataBuffer.BUFFER_SIZE
					&& positionsBuffer[validInstances] >= previous.positions[j]; j++) {
				if (logic.joinBufWithBytes(previous, j, payloadBuffer,
						bufferPtr)) {
					if (val == LookaheadOptimization.WRITE) {
						validInstances++;
					} else if (val == LookaheadOptimization.OVERWRITE) {
						overwritePrevPositionPayloadBuf(payloadBuffer,
								positionsBuffer, validInstances);
					}
					if (stopAtFirst)
						return 1;
					break;
				}
			}
		}
		return validInstances;
	}
}

class EarlyStopFTLWithFirstCheckJoin extends FirstToLastJoin {

	EarlyStopFTLWithFirstCheckJoin(JoinLogicAware logic) {
		super(logic);
	}

	@Override
	public int joinTerm(TermPositions term, NodeDataBuffer previous,
			byte[] payloadBuffer, int[] positionsBuffer,
			LookaheadOptimization optimization, boolean stopAtFirst)
			throws IOException {
		int freq = term.freq();
		int bufferPtr = 0;
		int validInstances = 0;
		int val;

		for (int i = 0; i < freq && validInstances < NodeDataBuffer.BUFFER_SIZE; i++) {
			positionsBuffer[validInstances] = term.nextPosition();
			if (shouldLoadPayload(positionsBuffer[validInstances], previous)) {
				bufferPtr = NodeDataBuffer.PAYLOAD_LENGTH * validInstances;
				term.getPayload(payloadBuffer, bufferPtr);
				val = LookaheadOptimization.WRITE;
				if (!optimization.equals(LookaheadOptimization.NONE)
						&& validInstances > 0) {
					val = optimization.check(payloadBuffer, validInstances - 1);
					if (val == LookaheadOptimization.IGNORE)
						continue;
				}
				for (int j = 0; j < previous.size()
						&& validInstances < NodeDataBuffer.BUFFER_SIZE
						&& positionsBuffer[validInstances] >= previous.positions[j]; j++) {
					if (logic.joinBufWithBytes(previous, j, payloadBuffer,
							bufferPtr)) {
						if (val == LookaheadOptimization.WRITE) {
							validInstances++;
						} else if (val == LookaheadOptimization.OVERWRITE) {
							overwritePrevPositionPayloadBuf(payloadBuffer,
									positionsBuffer, validInstances);
						}
						if (stopAtFirst)
							return 1;
						break;
					}
				}
			}
		}
		return validInstances;
	}
}
