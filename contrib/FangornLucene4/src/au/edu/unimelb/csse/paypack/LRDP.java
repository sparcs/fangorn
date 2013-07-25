package au.edu.unimelb.csse.paypack;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.Position;
import au.edu.unimelb.csse.join.NodePositions;

public class LRDP implements LogicalNodePositionAware {
	// offsets
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int DEPTH = 2;
	private static final int PARENT = 3;

	public static final int POSITION_LENGTH = 4;

	private PhysicalPayloadFormatAware physicalFormat;
	private OperatorAware binaryOperatorHandler;

	public LRDP(PhysicalPayloadFormat ppf) {
		this.physicalFormat = ppf.getPhysicalPFA();
		this.binaryOperatorHandler = new LRDPTreeOperators();
	}

	public OperatorAware getOperatorHandler() {
		return binaryOperatorHandler;
	}

	public void setLeft(int[] positions, int offset, int value) {
		positions[offset + LEFT] = value;
	}

	public void setRight(int[] positions, int offset, int value) {
		positions[offset + RIGHT] = value;
	}

	public void setDepth(int[] positions, int offset, int value) {
		positions[offset + DEPTH] = value;
	}

	public void setParent(int[] positions, int offset, int value) {
		positions[offset + PARENT] = value;
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
		// reset buffer offset
		buffer.offset = 0;
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
	@Override
	public int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		int nextPosition = node.nextPosition();
		physicalFormat.decode(node.getPayload(), buffer);
		buffer.offset = buffer.size - POSITION_LENGTH;
		return nextPosition;
	}

	@Override
	public int getPositionLength() {
		return POSITION_LENGTH;
	}

	public class LRDPTreeOperators implements OperatorAware {

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

		@Override
		public boolean same(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] == next[noff + LEFT]
					&& prev[poff + RIGHT] == next[noff + RIGHT]
					&& prev[poff + DEPTH] == next[noff + DEPTH];
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

		@Override
		public int relativeDepth(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + DEPTH] - next[noff + DEPTH];
		}

		@Override
		public Operator mostRelevantOpRelation(int[] prev, int poff, int[] next,
				int noff) {
			int leftDiff = prev[poff + LEFT] - next[noff + LEFT]; 
			int rightDiff = prev[poff + RIGHT] - next[noff + RIGHT];
			if (leftDiff <= 0) {
				if (rightDiff >= 0) {
					int depthDiff = prev[poff + DEPTH] - next[noff + DEPTH];
					if (depthDiff < 0) {
						if (depthDiff == -1) {
							return Operator.CHILD;
						}
						return Operator.DESCENDANT;
					} else if (depthDiff > 0) {
						if (depthDiff == 1) {
							return Operator.PARENT;
						}
						return Operator.ANCESTOR;
					}
					return Operator.SAME;
				} else if (leftDiff == 0 && rightDiff < 0) {
					int depthDiff = prev[poff + DEPTH] - next[noff + DEPTH];
					if (depthDiff == 1) {
						return Operator.PARENT;
					}
					return Operator.ANCESTOR;
				}
				int rightLeftDiff = prev[poff + RIGHT] - next[noff + LEFT];
				int parentDiff = prev[poff + PARENT] - next[noff + PARENT];
				if (rightLeftDiff == 0) {
					if (parentDiff == 0) {
						return Operator.IMMEDIATE_FOLLOWING_SIBLING;
					}
					return Operator.IMMEDIATE_FOLLOWING;
				}
				if (parentDiff == 0) {
					return Operator.FOLLOWING_SIBLING;
				}
				return Operator.FOLLOWING;
			}
			if (rightDiff <= 0) {
				int depthDiff = prev[poff + DEPTH] - next[noff + DEPTH];
				if (depthDiff == 1) {
					return Operator.PARENT;
				}
				return Operator.ANCESTOR;
			}
			int leftRightDiff = prev[poff + LEFT] - next[noff + RIGHT];
			int parentDiff = prev[poff + PARENT] - next[noff + PARENT];
			if (leftRightDiff == 0) {
				if (parentDiff == 0) {
					return Operator.IMMEDIATE_PRECEDING_SIBLING;
				}
				return Operator.IMMEDIATE_PRECEDING;
			}
			if (parentDiff == 0) {
				return Operator.PRECEDING_SIBLING;
			}
			return Operator.PRECEDING;
		}

		@Override
		public Position positionRelation(int[] prev, int poff, int[] next,
				int noff) {
			int leftDiff = prev[poff + LEFT] - next[noff + LEFT];
			int rightDiff = prev[poff + RIGHT] - next[noff + RIGHT];
			if (leftDiff > 0) {
				if (rightDiff > 0) {
					return Position.BEFORE;
				}
				return Position.ABOVE;
			} else if (leftDiff < 0) {
				if (rightDiff < 0) {
					return Position.AFTER;
				}
				return Position.BELOW;
			}
			if (rightDiff > 0) {
				return Position.BELOW;
			} else if (rightDiff < 0) {
				return Position.ABOVE;
			}
			int depthDiff = prev[poff + DEPTH] - next[noff + DEPTH];
			if (depthDiff < 0) {
				return Position.BELOW;
			} else if (depthDiff > 0) {
				return Position.ABOVE;
			}
			return Position.SAME;
		}

		@Override
		public boolean isLeftAligned(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + LEFT] - next[noff + LEFT] == 0;
		}
	}

	@Override
	public BytesRef[] encode(int[] positions, int numTokens)
			throws PayloadFormatException {
		return physicalFormat.encode(positions, numTokens);
	}

	@Override
	public boolean isTreeRootPosition(int[] positions, int offset) {
		return depth(positions, offset) == 0;
	}

	public int depth(int[] payloads, int offset) {
		return payloads[offset + DEPTH];
	}

	@Override
	public int compare(int[] pos1, int off1, int[] pos2, int off2) {
		int leftDiff = pos1[off1 + LEFT] - pos2[off2 + LEFT];
		if (leftDiff != 0) {
			return leftDiff;
		}
		int rightDiff = pos1[off1 + RIGHT] - pos2[off2 + RIGHT];
		if (rightDiff != 0) {
			return -1 * rightDiff;
		}
		int depthDiff = pos1[off1 + DEPTH] - pos2[off2 + DEPTH];
		if (depthDiff != 0) {
			return depthDiff;
		}
		return pos1[off1 + PARENT] - pos2[off2 + PARENT];
	}

	public static enum PhysicalPayloadFormat {
		BYTE1111(new BytePacking(4)), BYTE2212(new BytePacking2212());

		private PhysicalPayloadFormatAware ppfa;

		PhysicalPayloadFormat(PhysicalPayloadFormatAware ppfa) {
			this.ppfa = ppfa;
		}

		public PhysicalPayloadFormatAware getPhysicalPFA() {
			return ppfa;
		}
	}

}
