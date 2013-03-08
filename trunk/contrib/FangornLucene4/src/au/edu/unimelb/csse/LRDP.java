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
	
	public int left(int[] positions, int off) {
		return positions[off + LEFT];
	}
	
	public int right(int[] positions, int off) {
		return positions[off + RIGHT];
	}
	
	public int depth(int[] positions, int off) {
		return positions[off + DEPTH];
	}
	
	public int parent(int[] positions, int off) {
		return positions[off + PARENT];
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
	 * @return 
	 * @throws IOException
	 */
	public int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		int nextPosition = node.nextPosition();
		physicalFormat.decode(node.getPayload(), buffer);
		return nextPosition;
	}

	@Override
	public int getPositionLength() {
		return POSITION_LENGTH;
	}

	public class LRDPOperators implements BinaryOperatorAware {

		@Override
		public boolean descendant(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] <= next[noff + LEFT]
					&& prev[poff + RIGHT] >= next[noff + RIGHT]
					&& prev[poff + DEPTH] < next[noff + DEPTH];
		}

		@Override
		public boolean ancestor(int[] prev, int poff, int[] next, int noff) {
			return descendant(next, noff, prev, poff);
		}

		@Override
		public boolean child(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] <= next[noff + LEFT]
					&& prev[poff + RIGHT] >= next[noff + RIGHT]
					&& prev[poff + DEPTH] + 1 == next[noff + DEPTH];
		}

		@Override
		public boolean parent(int[] prev, int poff, int[] next, int noff) {
			return child(next, noff, prev, poff);
		}

		@Override
		public boolean following(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + RIGHT] <= next[noff + LEFT];
		}

		@Override
		public boolean preceding(int[] prev, int poff, int[] next, int noff) {
			return following(next, noff, prev, poff);
		}

		@Override
		public boolean followingSibling(int[] prev, int poff, int[] next,
				int noff) {
			return prev[poff + RIGHT] <= next[noff + LEFT]
					&& prev[poff + PARENT] == next[noff + PARENT];
		}

		@Override
		public boolean precedingSibling(int[] prev, int poff, int[] next,
				int noff) {
			return followingSibling(next, noff, prev, poff);
		}

		@Override
		public boolean immediateFollowing(int[] prev, int poff, int[] next,
				int noff) {
			return prev[poff + RIGHT] == next[noff + LEFT];
		}

		@Override
		public boolean immediatePreceding(int[] prev, int poff, int[] next,
				int noff) {
			return immediateFollowing(next, noff, prev, poff);
		}

		@Override
		public boolean immediateFollowingSibling(int[] prev, int poff,
				int[] next, int noff) {
			return prev[poff + RIGHT] == next[noff + LEFT]
					&& prev[poff + PARENT] == next[noff + PARENT];
		}

		@Override
		public boolean immediatePrecedingSibling(int[] prev, int poff,
				int[] next, int noff) {
			return immediateFollowingSibling(next, noff, prev, poff);
		}

		/**
		 * Equivalent to PRECEDING || ANCESTOR
		 */
		@Override
		public boolean startsBefore(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] > next[noff + LEFT]
					|| (prev[poff + LEFT] == next[noff + LEFT] && prev[poff
							+ DEPTH] > next[noff + DEPTH]);
		}

		/**
		 * Equivalent to DESCENDANT || FOLLOWING
		 */
		@Override
		public boolean startsAfter(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] < next[noff + LEFT]
					|| (prev[poff + LEFT] == next[noff + LEFT] && prev[poff
							+ DEPTH] < next[noff + DEPTH]);
		}

	}
}
