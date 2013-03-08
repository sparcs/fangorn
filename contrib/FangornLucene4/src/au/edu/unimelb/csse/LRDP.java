package au.edu.unimelb.csse;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.join.NodePositions;
import au.edu.unimelb.csse.paypack.PhysicalPayloadFormatAware;

public class LRDP implements LogicalNodePositionAware {
	// offsets
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int DEPTH = 2;
	private static final int PARENT = 3;

	public static final int POSITION_LENGTH = 4;

	private PhysicalPayloadFormatAware physicalFormat;
	private BinaryOperatorAware binaryOperatorHandler;

	public LRDP(PhysicalPayloadFormatAware physicalFormat) {
		this.physicalFormat = physicalFormat;
		this.binaryOperatorHandler = new LRDPOperators();
	}

	public BinaryOperatorAware getBinaryOperatorHandler() {
		return binaryOperatorHandler;
	}

	public void getAllPositions(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		int freq = node.freq();
		int posIndex = 0;
		buffer.reset();
		while (posIndex < freq) {
			getNextPosition(buffer, node);
			posIndex++;
		}
	}

	/**
	 * Warning: should be called only when it is certain that there are more
	 * positions to read
	 * 
	 * @param buffer
	 * @param node
	 * @throws IOException
	 */
	public void getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		node.nextPosition();
		physicalFormat.decode(node.getPayload(), buffer);
	}

	@Override
	public int getPositionLength() {
		return POSITION_LENGTH;
	}

	public class LRDPOperators implements BinaryOperatorAware {

		@Override
		public boolean descendant(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + LEFT] <= next.positions[next.offset
					+ LEFT]
					&& prev.positions[prev.offset + RIGHT] >= next.positions[next.offset
							+ RIGHT]
					&& prev.positions[prev.offset + DEPTH] < next.positions[next.offset
							+ DEPTH];
		}

		@Override
		public boolean ancestor(NodePositions prev, NodePositions next) {
			return descendant(next, prev);
		}

		@Override
		public boolean child(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + LEFT] <= next.positions[next.offset
					+ LEFT]
					&& prev.positions[prev.offset + RIGHT] >= next.positions[next.offset
							+ RIGHT]
					&& prev.positions[prev.offset + DEPTH] + 1 == next.positions[next.offset
							+ DEPTH];
		}

		@Override
		public boolean parent(NodePositions prev, NodePositions next) {
			return child(next, prev);
		}

		@Override
		public boolean following(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + RIGHT] <= next.positions[next.offset
					+ LEFT];
		}

		@Override
		public boolean preceding(NodePositions prev, NodePositions next) {
			return following(next, prev);
		}

		@Override
		public boolean followingSibling(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + RIGHT] <= next.positions[next.offset
					+ LEFT]
					&& prev.positions[prev.offset + PARENT] == next.positions[next.offset
							+ PARENT];
		}

		@Override
		public boolean precedingSibling(NodePositions prev, NodePositions next) {
			return followingSibling(next, prev);
		}

		@Override
		public boolean immediateFollowing(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + RIGHT] == next.positions[next.offset
					+ LEFT];
		}

		@Override
		public boolean immediatePreceding(NodePositions prev, NodePositions next) {
			return immediateFollowing(next, prev);
		}

		@Override
		public boolean immediateFollowingSibling(NodePositions prev,
				NodePositions next) {
			return prev.positions[prev.offset + RIGHT] == next.positions[next.offset
					+ LEFT]
					&& prev.positions[prev.offset + PARENT] == next.positions[next.offset
							+ PARENT];
		}

		@Override
		public boolean immediatePrecedingSibling(NodePositions prev,
				NodePositions next) {
			return immediateFollowingSibling(next, prev);
		}

		/**
		 * Equivalent to PRECEDING || ANCESTOR
		 */
		@Override
		public boolean startsBefore(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + LEFT] > next.positions[next.offset
					+ LEFT]
					|| (prev.positions[prev.offset + LEFT] == next.positions[next.offset
							+ LEFT] && prev.positions[prev.offset + DEPTH] > next.positions[next.offset
							+ DEPTH]);
		}

		/**
		 * Equivalent to DESCENDANT || FOLLOWING
		 */
		@Override
		public boolean startsAfter(NodePositions prev, NodePositions next) {
			return prev.positions[prev.offset + LEFT] < next.positions[next.offset
					+ LEFT]
					|| (prev.positions[prev.offset + LEFT] == next.positions[next.offset
							+ LEFT] && prev.positions[prev.offset + DEPTH] < next.positions[next.offset
							+ DEPTH]);
		}

	}
}
