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

public interface JoinLogic {

	JoinLogicAware DESCENDANT = new AbstractJoinLogic() {
		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] buffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.lefts[datapos]),
					u(data.heights[datapos]), u(buffer[bufferPtr
							+ NodeDataBuffer.RIGHT]), u(buffer[bufferPtr
							+ NodeDataBuffer.LEFT]), u(buffer[bufferPtr
							+ NodeDataBuffer.HEIGHT]));
		}

		private boolean match(int startRight, int startLeft, int startHeight,
				int endRight, int endLeft, int endHeight) {
			//incrementNumberOfComparisons();
			return startRight >= endRight && startLeft <= endLeft
					&& startHeight < endHeight;
		}

		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(u(start[startStartPos + NodeDataBuffer.RIGHT]),
					u(start[startStartPos + NodeDataBuffer.LEFT]),
					u(start[startStartPos + NodeDataBuffer.HEIGHT]),
					u(end[endStartPos + NodeDataBuffer.RIGHT]),
					u(end[endStartPos + NodeDataBuffer.LEFT]),
					u(end[endStartPos + NodeDataBuffer.HEIGHT]));
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.lefts[pos1]),
					u(buffer1.heights[pos1]), u(buffer2.rights[pos2]),
					u(buffer2.lefts[pos2]), u(buffer2.heights[pos2]));
		}
	};

	JoinLogicAware ANCESTOR = new AbstractJoinLogic() {
		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.lefts[datapos]),
					u(data.heights[datapos]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.RIGHT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.LEFT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.HEIGHT]));
		}

		private boolean match(int startRight, int startLeft, int startHeight,
				int endRight, int endLeft, int endHeight) {
			//incrementNumberOfComparisons();
			return startRight <= endRight && startLeft >= endLeft
					&& startHeight > endHeight;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(u(start[startStartPos + NodeDataBuffer.RIGHT]),
					u(start[startStartPos + NodeDataBuffer.LEFT]),
					u(start[startStartPos + NodeDataBuffer.HEIGHT]),
					u(end[endStartPos + NodeDataBuffer.RIGHT]),
					u(end[endStartPos + NodeDataBuffer.LEFT]),
					u(end[endStartPos + NodeDataBuffer.HEIGHT]));
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.lefts[pos1]),
					u(buffer1.heights[pos1]), u(buffer2.rights[pos2]),
					u(buffer2.lefts[pos2]), u(buffer2.heights[pos2]));
		}

	};

	JoinLogicAware CHILD = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.lefts[datapos]),
					u(data.heights[datapos]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.RIGHT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.LEFT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.HEIGHT]));
		}

		private boolean match(int startRight, int startLeft, int startHeight,
				int endRight, int endLeft, int endHeight) {
			//incrementNumberOfComparisons();
			return startRight >= endRight && startLeft <= endLeft
					&& startHeight + 1 == endHeight;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(u(start[startStartPos + NodeDataBuffer.RIGHT]),
					u(start[startStartPos + NodeDataBuffer.LEFT]),
					u(start[startStartPos + NodeDataBuffer.HEIGHT]),
					u(end[endStartPos + NodeDataBuffer.RIGHT]),
					u(end[endStartPos + NodeDataBuffer.LEFT]),
					u(end[endStartPos + NodeDataBuffer.HEIGHT]));
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.lefts[pos1]),
					u(buffer1.heights[pos1]), u(buffer2.rights[pos2]),
					u(buffer2.lefts[pos2]), u(buffer2.heights[pos2]));
		}

	};

	JoinLogicAware PARENT = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.lefts[datapos]),
					u(data.heights[datapos]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.RIGHT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.LEFT]), u(payloadBuffer[bufferPtr
							+ NodeDataBuffer.HEIGHT]));
		}

		private boolean match(int startRight, int startLeft, int startHeight,
				int endRight, int endLeft, int endHeight) {
			//incrementNumberOfComparisons();
			return startRight <= endRight && startLeft >= endLeft
					&& startHeight == endHeight + 1;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(u(start[startStartPos + NodeDataBuffer.RIGHT]),
					u(start[startStartPos + NodeDataBuffer.LEFT]),
					u(start[startStartPos + NodeDataBuffer.HEIGHT]),
					u(end[endStartPos + NodeDataBuffer.RIGHT]),
					u(end[endStartPos + NodeDataBuffer.LEFT]),
					u(end[endStartPos + NodeDataBuffer.HEIGHT]));
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.lefts[pos1]),
					u(buffer1.heights[pos1]), u(buffer2.rights[pos2]),
					u(buffer2.lefts[pos2]), u(buffer2.heights[pos2]));
		}

	};

	JoinLogicAware FOLLOWING_SIBLING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.parents[datapos]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.LEFT]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.PARENT]));

		}

		private boolean match(int startRight, int startParent, int endLeft,
				int endParent) {
			//incrementNumberOfComparisons();
			return startRight <= endLeft && startParent == endParent;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.RIGHT],
					start[startStartPos + NodeDataBuffer.PARENT],
					end[endStartPos + NodeDataBuffer.LEFT], end[endStartPos
							+ NodeDataBuffer.PARENT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.parents[pos1]),
					u(buffer2.lefts[pos2]), u(buffer2.parents[pos2]));
		}

	};

	JoinLogicAware PRECEDING_SIBLING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.lefts[datapos]), u(data.parents[datapos]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.RIGHT]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.PARENT]));

		}

		private boolean match(int startLeft, int startParent, int endRight,
				int endParent) {
			//incrementNumberOfComparisons();
			return startLeft >= endRight && startParent == endParent;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.LEFT],
					start[startStartPos + NodeDataBuffer.PARENT],
					end[endStartPos + NodeDataBuffer.RIGHT], end[endStartPos
							+ NodeDataBuffer.PARENT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.lefts[pos1]), u(buffer1.parents[pos1]),
					u(buffer2.rights[pos2]), u(buffer2.parents[pos2]));
		}

	};

	JoinLogicAware IMMEDIATE_FOLLOWING_SIBLING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(data.parents[datapos]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.LEFT]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.PARENT]));
		}

		private boolean match(int startRight, int startParent, int endLeft,
				int endParent) {
			//incrementNumberOfComparisons();
			return startRight == endLeft && startParent == endParent;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.RIGHT],
					start[startStartPos + NodeDataBuffer.PARENT],
					end[endStartPos + NodeDataBuffer.LEFT], end[endStartPos
							+ NodeDataBuffer.PARENT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer1.parents[pos1]),
					u(buffer2.lefts[pos2]), u(buffer2.parents[pos2]));
		}

	};

	JoinLogicAware IMMEDIATE_PRECEDING_SIBLING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.lefts[datapos]), u(data.parents[datapos]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.RIGHT]),
					u(payloadBuffer[bufferPtr + NodeDataBuffer.PARENT]));
		}

		private boolean match(int startLeft, int startParent, int endRight,
				int endParent) {
			//incrementNumberOfComparisons();
			return startLeft == endRight && startParent == endParent;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.LEFT],
					start[startStartPos + NodeDataBuffer.PARENT],
					end[endStartPos + NodeDataBuffer.RIGHT], end[endStartPos
							+ NodeDataBuffer.PARENT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.lefts[pos1]), u(buffer1.parents[pos1]),
					u(buffer2.rights[pos2]), u(buffer2.parents[pos2]));
		}

	};

	JoinLogicAware FOLLOWING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(payloadBuffer[bufferPtr
					+ NodeDataBuffer.LEFT]));
		}

		private boolean match(int startRight, int endLeft) {
			//incrementNumberOfComparisons();
			return startRight <= endLeft;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.RIGHT],
					end[endStartPos + NodeDataBuffer.LEFT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer2.lefts[pos2]));
		}

	};

	JoinLogicAware PRECEDING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.lefts[datapos]), u(payloadBuffer[bufferPtr
					+ NodeDataBuffer.RIGHT]));

		}

		private boolean match(int startLeft, int endRight) {
			//incrementNumberOfComparisons();
			return startLeft >= endRight;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.LEFT],
					end[endStartPos + NodeDataBuffer.RIGHT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.lefts[pos1]), u(buffer2.rights[pos2]));
		}

	};

	JoinLogicAware IMMEDIATE_FOLLOWING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.rights[datapos]), u(payloadBuffer[bufferPtr
					+ NodeDataBuffer.LEFT]));
		}

		private boolean match(int startRight, int endLeft) {
			//incrementNumberOfComparisons();
			return startRight == endLeft;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.RIGHT],
					end[endStartPos + NodeDataBuffer.LEFT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.rights[pos1]), u(buffer2.lefts[pos2]));
		}

	};

	JoinLogicAware IMMEDIATE_PRECEDING = new AbstractJoinLogic() {

		@Override
		public boolean joinBufWithBytes(NodeDataBuffer data, int datapos,
				byte[] payloadBuffer, int bufferPtr) {
			return match(u(data.lefts[datapos]), u(payloadBuffer[bufferPtr
					+ NodeDataBuffer.RIGHT]));
		}

		private boolean match(int startLeft, int endRight) {
			//incrementNumberOfComparisons();
			return startLeft == endRight;
		}

		@Override
		public boolean joinBytesWithBytes(byte[] start, int startStartPos,
				byte[] end, int endStartPos) {
			return match(start[startStartPos + NodeDataBuffer.LEFT],
					end[endStartPos + NodeDataBuffer.RIGHT]);
		}

		@Override
		public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
				NodeDataBuffer buffer2, int pos2) {
			return match(u(buffer1.lefts[pos1]), u(buffer2.rights[pos2]));
		}

	};
}

abstract class AbstractJoinLogic implements JoinLogicAware {

	private static int numberOfComparisons = 0;

	protected int u(byte b) {
		//changed from return (b | 256) & 255; to return b & 255 because they are equivalent. dt: 2/3/11
		return b & 255;
	}

	// this is not thread safe!!!
	protected void incrementNumberOfComparisons() {
		numberOfComparisons++;
	}

	public static int getNumberOfComparisons() {
		return numberOfComparisons;
	}

}
