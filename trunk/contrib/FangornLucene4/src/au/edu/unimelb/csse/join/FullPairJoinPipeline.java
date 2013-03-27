package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class FullPairJoinPipeline {
	Pipe start;
	final LogicalNodePositionAware nodePositionAware;
	final FullPairJoin join;
	Operator[] operators;
	NodePositions prevPositions;
	NodePositions[] buffers;
	NodePairPositions nodePairPositions;
	List<int[]> results;
	private int length;
	int totalLength;

	public FullPairJoinPipeline(LogicalNodePositionAware nodePositionAware,
			FullPairJoin join) {
		this.nodePositionAware = nodePositionAware;
		this.length = nodePositionAware.getPositionLength();
		prevPositions = new NodePositions();
		nodePairPositions = new NodePairPositions();
		this.join = join;
	}

	public Pipe createExecPipeline(PostingsAndFreq pfRoot, Operator[] operators) {
		totalLength = operators.length * length;
		this.operators = operators;
		start = new FirstPipe(pfRoot, operators[pfRoot.position]);
		if (pfRoot.children.length == 1) {
			recCreatePipe(pfRoot.children[0], pfRoot.position, start);
		} else if (pfRoot.children.length > 1) {
			Pipe prevPipe = start;
			int parentPos = pfRoot.position;
			for (int i = 0; i < pfRoot.children.length; i++) {
				prevPipe = recCreatePipe(pfRoot.children[i], parentPos,
						prevPipe);
			}
		}
		return start;
	}

	private Pipe recCreatePipe(PostingsAndFreq node, int parentPos,
			Pipe prevPipe) {
		Pipe pipe = new SimplePipe(node, operators[node.position], parentPos,
				prevPipe);
		if (node.children.length > 0) {
			if (node.children.length == 1) {
				pipe = recCreatePipe(node.children[0], node.position, pipe);
			} else {
				for (int i = 0; i < node.children.length; i++) {
					pipe = recCreatePipe(node.children[i], node.position, pipe);
				}
			}
		}
		return pipe;
	}

	void setPrevAndBuffers(NodePositions prev, NodePositions[] buffers) {
		this.prevPositions = prev;
		this.buffers = buffers;
	}

	void setPrevPositionsFromResults(int position, Comparator<int[]> comparator) {
		Collections.sort(results, comparator);
		prevPositions.reset();
		for (int[] result : results) {
			if (prevPositions.size == 0) {
				for (int i = 0; i < length; i++) {
					prevPositions.positions[i] = result[position * length + i];
					prevPositions.size++;
				}
			} else {
				if (nodePositionAware.compare(result, position * length,
						prevPositions.positions, prevPositions.offset) != 0) {
					while (prevPositions.size + length > prevPositions.positions.length) {
						prevPositions.expand();
					}
					for (int i = 0; i < length; i++) {
						prevPositions.positions[prevPositions.size + i] = result[position
								* length + i];
					}
					prevPositions.size += length;
					prevPositions.offset += length;
				}
			}
		}
	}

	void mergeNodePairPositionsWithResults(int parentPos, int currentPos) {
		List<int[]> newResults = new ArrayList<int[]>();
		for (int i = 0, j = 0, k = 0; i < prevPositions.size
				&& j < nodePairPositions.size;) {
			while (nodePositionAware.compare(prevPositions.positions, i,
					results.get(k), parentPos * length) != 0) {
				k++;
			}
			int c = nodePositionAware.compare(prevPositions.positions, i,
					nodePairPositions.node1, j);
			if (c == 0) {
				int m = k;
				while (m < results.size()
						&& nodePositionAware.compare(prevPositions.positions,
								i, results.get(m), parentPos * length) == 0) {
					int[] resultOld = results.get(m);
					int[] result = Arrays.copyOf(resultOld, resultOld.length);
					for (int n = 0; n < length; n++) {
						result[currentPos * length + n] = nodePairPositions.node2[j
								+ n];
					}
					newResults.add(result);
					m++;
				}
				j += length;
			} else if (c < 0) {
				i += length;
			}
		}
		results = newResults;
	}

	class FirstPipe extends AbstractPipe {
		private static final int PARENT_POS = 0;

		public FirstPipe(PostingsAndFreq node, Operator op) {
			super(node, op);
		}

		@Override
		public NodePairPositions execute() throws IOException {
			prevPositions.reset();
			nodePositionAware.getAllPositions(prevPositions, node.postings);
			if (Operator.CHILD.equals(op)) {
				if (nodePositionAware.isTreeRootPosition(
						prevPositions.positions, 0)) {
					prevPositions.offset = 0;
					prevPositions.size = nodePositionAware.getPositionLength();
				} else {
					prevPositions.reset();
				}
			}
			if (prevPositions.size == 0) {
				nodePairPositions.reset();
				results.clear();
				return nodePairPositions;
			}
			results = new ArrayList<int[]>();
			for (int i = 0; i < prevPositions.size / length; i++) {
				int[] result = new int[totalLength];
				for (int j = 0; j < length; j++) {
					result[PARENT_POS + j] = prevPositions.positions[i * length
							+ j];
				}
				results.add(result);
			}
			if (next == null) {
				return nodePairPositions;
			}
			return next.execute();
		}
	}

	class SimplePipe extends AbstractPipe {
		int parentPos;
		private NPAPathPositionComparator comparator;

		public SimplePipe(PostingsAndFreq node, Operator op, int parentPos,
				Pipe prevPipe) {
			super(node, op);
			this.parentPos = parentPos;
			prevPipe.setNext(this);
			comparator = new NPAPathPositionComparator(nodePositionAware,
					parentPos);
		}

		@Override
		public NodePairPositions execute() throws IOException {
			setPrevPositionsFromResults(parentPos, comparator);
			join.match(prevPositions, op, node.postings, nodePairPositions,
					buffers);
			if (nodePairPositions.size == 0) {
				results.clear();
				return nodePairPositions;
			}
			mergeNodePairPositionsWithResults(parentPos, node.position);
			if (next == null) {
				return nodePairPositions;
			}
			return next.execute();
		}

	}

	abstract class AbstractPipe implements Pipe {
		PostingsAndFreq node;
		Operator op;
		Pipe next;

		public AbstractPipe(PostingsAndFreq node, Operator op) {
			this.node = node;
			this.op = op;
		}

		@Override
		public void setNext(Pipe pipe) {
			next = pipe;
		}

		@Override
		public Pipe getNext() {
			return next;
		}

	}

	interface Pipe {

		NodePairPositions execute() throws IOException;

		void setNext(Pipe pipe);

		Pipe getNext();
	}
}
