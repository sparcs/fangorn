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

import java.util.Arrays;
import java.util.BitSet;

import au.edu.unimelb.csse.search.join.JoinLogicAware;

public class NodeDataBuffer {
	public static final int BUFFER_SIZE = 64;
	public static final int RIGHT = 0;
	public static final int LEFT = 1;
	public static final int HEIGHT = 2;
	public static final int PARENT = 3;
	public static final int PAYLOAD_LENGTH = 4;
	public final byte[] rights = new byte[BUFFER_SIZE];
	public final byte[] lefts = new byte[BUFFER_SIZE];
	public final byte[] heights = new byte[BUFFER_SIZE];
	public final byte[] parents = new byte[BUFFER_SIZE];
	public final int[] positions = new int[BUFFER_SIZE];
	// private final int[] freqMults = new int[BUFFER_SIZE];
	public int size = 0;
	private final byte[] payloadSnapshot = new byte[BUFFER_SIZE
			* PAYLOAD_LENGTH];
	private final int[] positionsSnapshot = new int[BUFFER_SIZE];
	// private final int[] freqMultSnapshot = new int[BUFFER_SIZE];
	private final int[] removeBuffer = new int[BUFFER_SIZE];
	private int removeBufferSize = 0;
	private int sizeSnapshot;

	public NodeDataBuffer(NodeDataBuffer data) {
		this();
		if (data.size > 0) {
			this.size = data.size;
			System.arraycopy(data.rights, 0, rights, 0, size);
			System.arraycopy(data.lefts, 0, lefts, 0, size);
			System.arraycopy(data.heights, 0, heights, 0, size);
			System.arraycopy(data.parents, 0, parents, 0, size);
			System.arraycopy(data.positions, 0, positions, 0, size);
		}
		if (data.sizeSnapshot != 0) {
			this.sizeSnapshot = data.sizeSnapshot;
			System.arraycopy(data.payloadSnapshot, 0, payloadSnapshot, 0,
					sizeSnapshot * PAYLOAD_LENGTH);
			System.arraycopy(data.positionsSnapshot, 0, positionsSnapshot, 0,
					sizeSnapshot);
		}
		if (removeBufferSize != 0) {
			this.removeBufferSize = data.removeBufferSize;
			System.arraycopy(data.removeBuffer, 0, removeBuffer, 0,
					removeBufferSize);
		}
	}

	public NodeDataBuffer() {
	}

	public void saveSnapshot() {
		System.arraycopy(heights, 0, payloadSnapshot, 0, size);
		System.arraycopy(lefts, 0, payloadSnapshot, BUFFER_SIZE, size);
		System.arraycopy(rights, 0, payloadSnapshot, BUFFER_SIZE * 2, size);
		System.arraycopy(parents, 0, payloadSnapshot, BUFFER_SIZE * 3, size);
		// System.arraycopy(freqMults, 0, freqMultSnapshot, 0, size);
		System.arraycopy(positions, 0, positionsSnapshot, 0, size);
		sizeSnapshot = size;
	}

	public void reset() {
		this.size = 0;
	}

	public void restoreFromSnapshot() {
		System.arraycopy(payloadSnapshot, 0, heights, 0, sizeSnapshot);
		System.arraycopy(payloadSnapshot, BUFFER_SIZE, lefts, 0, sizeSnapshot);
		System.arraycopy(payloadSnapshot, BUFFER_SIZE * 2, rights, 0,
				sizeSnapshot);
		System.arraycopy(payloadSnapshot, BUFFER_SIZE * 3, parents, 0,
				sizeSnapshot);
		System.arraycopy(positionsSnapshot, BUFFER_SIZE, positions, 0,
				sizeSnapshot);
		size = sizeSnapshot;
	}

	public void set(int count, byte[] payloadBuffer, int[] positionsBuffer) {
		for (int i = 0; i < count; i++) {
			this.rights[i] = payloadBuffer[i * PAYLOAD_LENGTH + RIGHT];
			this.lefts[i] = payloadBuffer[i * PAYLOAD_LENGTH + LEFT];
			this.heights[i] = payloadBuffer[i * PAYLOAD_LENGTH + HEIGHT];
			this.parents[i] = payloadBuffer[i * PAYLOAD_LENGTH + PARENT];
			this.positions[i] = positionsBuffer[i];
		}
		size = count;
	}

	public int size() {
		return size;
	}

	public boolean posMatchesAtLeastOneBufferFTL(int thisPos,
			NodeDataBuffer otherBuffer, JoinLogicAware joinLogic) {
		for (int i = 0; i < otherBuffer.size; i++) {
			if (otherBuffer.positions[i] > positions[thisPos]) {
				return false;
			}
			if (joinLogic.joinBufWithBuf(this, thisPos, otherBuffer,
					i)) {
				return true;
			}
		}
		return false;
	}

	public boolean posMatchesAtLeastOneBufferLTF(int thisPos,
			NodeDataBuffer otherBuffer, JoinLogicAware joinLogic) {
		for (int i = otherBuffer.size - 1; i >= 0; i--) {
			if (otherBuffer.positions[i] < positions[thisPos]) {
				return false;
			}
			if (joinLogic.joinBufWithBuf(this, thisPos, otherBuffer,
					i)) {
				return true;
			}
		}
		return false;
	}

	public byte rightValue(int index) {
		return rights[index];
	}

	public byte leftValue(int index) {
		return lefts[index];
	}

	public byte heightValue(int index) {
		return heights[index];
	}

	public byte parentValue(int index) {
		return parents[index];
	}

	/**
	 * should always add in increasing order
	 * 
	 * @param loc
	 */
	public void addToRemoveBuffer(int loc) {
		removeBuffer[removeBufferSize++] = loc;
	}

	public void remove() {
		// Arrays.sort(removeBuffer, 0, removeBufferSize);
		try {
			int pos;
			for (int i = removeBufferSize - 1; i >= 0; i--) {
				pos = removeBuffer[i];
				if (pos != size - 1) {
					System.arraycopy(heights, pos + 1, heights, pos, size - pos
							- 1);
					System
							.arraycopy(lefts, pos + 1, lefts, pos, size - pos
									- 1);
					System.arraycopy(rights, pos + 1, rights, pos, size - pos
							- 1);
					System.arraycopy(parents, pos + 1, parents, pos, size - pos
							- 1);
					System.arraycopy(positions, pos + 1, positions, pos, size
							- pos - 1);
				}
				size--;
			}
			removeBufferSize = 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Rbs: " + removeBufferSize);
			System.out.println("Rb: " + Arrays.toString(removeBuffer));
			System.out.println("Main size: " + size);
			throw e;
		}
	}

	/**
	 * This method joins one NodeDataBuffer with another.
	 * 
	 * This should ideally be within FilterJoinAware but it is here because
	 * certain operations on positions would require a number of calls to
	 * NodeDataBuffer's methods otherwise
	 * 
	 * @param other
	 * @param matched
	 * @param isNot
	 * @param joinAware
	 * @param firstToLast
	 * @return
	 */
	public boolean match(NodeDataBuffer other, BitSet matched, boolean isNot,
			JoinLogicAware joinAware, boolean firstToLast) {
		if (firstToLast) {
			for (int i = 0; i < size; i++) {
				if (matched.get(i)) {
					if (!(isNot ^ posMatchesAtLeastOneBufferFTL(i, other, joinAware))) {
						matched.clear(i);
					}
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (matched.get(i)) {
					if (!(isNot ^ posMatchesAtLeastOneBufferLTF(i, other, joinAware))) {
						matched.clear(i);
					}
				}
			}
		}
		final int firstMatch = matched.nextSetBit(0);
		return firstMatch != -1 && firstMatch < size;
	}

	public int lastPosition() {
		if (size == 0)
			return -1;
		return positions[size - 1];
	}

	public int firstPosition() {
		if (size == 0)
			return -1;
		return positions[0];
	}

	public byte[] getAsBytes(int position) {
		return new byte[] { rights[position], lefts[position],
				heights[position], parents[position] };
	}

	public int getPos(byte[] data) {
		int i = 0;
		while (i < size()) {
			if (rights[i] == data[RIGHT] && lefts[i] == data[LEFT]
					&& parents[i] == data[PARENT] && heights[i] == data[HEIGHT]) {
				return i;
			}
			i++;
		}
		return -1;
	}
}
