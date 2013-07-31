package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

/**
 * This is an adaptation of the Staircase join by Grust et al. (2003)
 * 
 * Grust, T.; Keulen, M. & Teubner, J. Staircase Join: Teach a Relational DBMS
 * to Watch its (Axis) Steps In Proceedings of the 29th International Conference
 * on Very Large Databases (VLDB), 2003, 524-535
 * 
 * Here we use Lucene's index instead of a B+-tree inverted list in an RDBMS
 * 
 * @author sumukh
 * 
 */
public abstract class StaircaseJoin implements HalfPairJoin {
	public static final JoinBuilder JOIN_BUILDER = new StaircaseJoinBuilder();
	private final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final OperatorAware operatorAware;
	NodePositions result = new NodePositions();
	NodePositions next = new NodePositions();
	NodePositions buffer = new NodePositions();
	Operator op;
	boolean isPruneOp = false;

	public StaircaseJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
	}

	public StaircaseJoin(Operator op, LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		this.op = op;
		if (Operator.DESCENDANT.equals(op) | Operator.ANCESTOR.equals(op)
				| Operator.FOLLOWING.equals(op) | Operator.PRECEDING.equals(op)) {
			isPruneOp = true;
		}
	}

	@Override
	public NodePositions match(NodePositions prev, DocsAndPositionsEnum node) throws IOException {
		prev.offset = 0;
		if (isPruneOp) {
			prune(prev);
		}
		next.reset();
		result.reset();
		nodePositionAware.getAllPositions(next, node);
		doJoin(prev, next);
		return result;
	}

	@Override
	public NodePositions match(NodePositions prev, NodePositions next) throws IOException {
		prev.offset = 0;
		if (isPruneOp) {
			prune(prev);
		}
		next.offset = 0;
		result.reset();
		doJoin(prev, next);
		return result;
	}

	abstract void doJoin(NodePositions prev, NodePositions next);

	/**
	 * Prunes the prev list Makes use of result, next, and buffer variables as
	 * buffers
	 * 
	 * @param prev
	 */
	void prune(NodePositions prev) {
		// default: do nothing
	}
}

class DescStaircase extends StaircaseJoin {

	public DescStaircase(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				result.push(next, positionLength);
				next.offset += positionLength;
			} else if (operatorAware.following(prev.positions, prev.offset,
					next.positions, next.offset)) {
				prev.offset += positionLength;
			} else {
				next.offset += positionLength;
			}
		}
	}

	@Override
	void prune(NodePositions prev) {
		NodePositions stack = result;
		NodePositions mark = next;
		stack.reset();
		mark.reset();
		stack.push(prev, positionLength);
		mark.pushInt(0);
		for (int i = positionLength; i < prev.size; i += positionLength) {
			prev.offset = i;
			if (!operatorAware.descendant(stack.positions, stack.offset,
					prev.positions, prev.offset)) {
				mark.pushInt(i);
				stack.pop(positionLength);
				stack.push(prev, positionLength);
			}
		}
		prev.retain(mark, positionLength);
	}

}

class AncStaircase extends StaircaseJoin {

	public AncStaircase(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				result.push(next, positionLength);
				next.offset += positionLength;
			} else if (operatorAware.startsAfter(prev.positions, prev.offset,
					next.positions, next.offset)) {
				prev.offset += positionLength;
			} else {
				next.offset += positionLength;
			}
		}
	}

	@Override
	void prune(NodePositions prev) {
		NodePositions stack = result;
		NodePositions mark = next;
		NodePositions offsetStack = buffer;
		stack.reset();
		mark.reset();
		offsetStack.reset();
		stack.push(prev, positionLength);
		offsetStack.pushInt(0);
		for (int i = positionLength; i < prev.size; i += positionLength) {
			prev.offset = i;
			if (operatorAware.descendant(stack.positions, stack.offset,
					prev.positions, prev.offset)) {
				stack.push(prev, positionLength);
				offsetStack.pushInt(i);
			} else {
				stack.pop(positionLength);
				mark.pushInt(offsetStack.popInt());
				boolean pushedToStack = false;
				while (stack.size > 0) {
					if (operatorAware.descendant(stack.positions, stack.offset,
							prev.positions, prev.offset)) {
						stack.push(prev, positionLength);
						offsetStack.pushInt(i);
						pushedToStack = true;
						break;
					} else {
						stack.pop(positionLength);
						offsetStack.popInt();
					}
				}
				if (!pushedToStack) {
					stack.push(prev, positionLength);
					offsetStack.pushInt(i);
				}
			}
		}
		if (offsetStack.size > 0) {
			mark.pushInt(offsetStack.popInt());
		}
		prev.retain(mark, positionLength);
	}

}

class ChildStaircase extends StaircaseJoin {

	public ChildStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		while (next.offset < next.size) {
			prev.offset = 0;
			while (prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
				} else { // N is preceding or ancestor
					break;
				}
			}
			next.offset += positionLength;
		}
	}

}

class ParentStaircase extends StaircaseJoin {

	public ParentStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		while (next.offset < next.size) {
			prev.offset = 0;
			while (prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
				} else if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
					break;
				}
				prev.offset += positionLength;
			}
			next.offset += positionLength;
		}
	}

}

class FollPrecStaircase extends StaircaseJoin {

	public FollPrecStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			if (op.match(prev, next, operatorAware)) {
				result.push(next, positionLength);
			}
		}
	}

	@Override
	void prune(NodePositions prev) {
		if (op.equals(Operator.FOLLOWING)) {
			int idx = 0;
			prev.offset = positionLength;
			while (prev.offset < prev.size
					&& operatorAware.descendant(prev.positions, idx,
							prev.positions, prev.offset)) {
				idx = prev.offset;
				prev.offset += positionLength;
			}
			prev.retain(idx, positionLength);
		} else {
			prev.retain(prev.size - positionLength, positionLength);
		}
	}
}

class FolSibImFolSibStaircase extends StaircaseJoin {

	public FolSibImFolSibStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		int start = prev.size - positionLength;
		for (int i = next.size - positionLength; i >= 0; i -= positionLength) {
			for (int j = start; j >= 0; j -= positionLength) {
				while (j >= 0
						&& operatorAware.startsBefore(prev.positions, j,
								next.positions, i)) {
					j -= positionLength;
					start = j;
				}
				if (j < 0)
					break;
				if (op.match(prev.positions, j, next.positions, i,
						operatorAware)) {
					next.offset = i;
					result.insert(next, 0, positionLength);
					break;
				} else if (operatorAware.same(prev.positions, j, next.positions, i) || (operatorAware.following(prev.positions, j,
						next.positions, i)
						&& operatorAware.relativeDepth(prev.positions, j,
								next.positions, i) > 0)) {
					continue;
				}
				break;
			}
		}
	}
}

class PrecSibImPrecSibStaircase extends StaircaseJoin {

	public PrecSibImPrecSibStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		int start = 0;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				while (j < prev.size
						&& operatorAware.startsAfter(prev.positions, j,
								next.positions, i)) {
					j += positionLength;
					start = j;
				}
				if (j >= prev.size) {
					break;
				}
				if (op.match(prev.positions, j, next.positions, i,
						operatorAware)) {
					next.offset = i;
					result.push(next, positionLength);
					break;
				} else if (operatorAware.same(prev.positions, j, next.positions, i) || operatorAware.relativeDepth(prev.positions, j,
								next.positions, i) > 0) {
					continue;
				}
				break;
			}
		}
	}

}

class ImFolStaircase extends StaircaseJoin {

	public ImFolStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		int start = prev.size - positionLength;
		for (int i = next.size - positionLength; i >= 0; i -= positionLength) {
			for (int j = start; j >= 0; j -= positionLength) {
				while (j >= 0
						&& operatorAware.startsBefore(prev.positions, j,
								next.positions, i)) {
					j -= positionLength;
					start = j;
				}
				if (j < 0)
					break;
				if (operatorAware.immediateFollowing(prev.positions, j,
						next.positions, i)) {
					next.offset = i;
					result.insert(next, 0, positionLength);
					break;
				}
			}
		}
	}

}

class ImPrecStaircase extends StaircaseJoin {

	public ImPrecStaircase(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	void doJoin(NodePositions prev, NodePositions next) {
		int start = 0;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				while (j < prev.size
						&& operatorAware.startsAfter(prev.positions, j,
								next.positions, i)) {
					j += positionLength;
					start = j;
				}
				if (j >= prev.size) {
					break;
				}
				if (operatorAware.immediatePreceding(prev.positions, j,
						next.positions, i)) {
					next.offset = i;
					result.push(next, positionLength);
					break;
				}
			}
		}
	}

}

class StaircaseJoinBuilder implements JoinBuilder {

	@Override
	public HalfPairJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.DESCENDANT.equals(op))
			return new DescStaircase(op, nodePositionAware);
		if (Operator.ANCESTOR.equals(op))
			return new AncStaircase(op, nodePositionAware);
		if (Operator.CHILD.equals(op))
			return new ChildStaircase(op, nodePositionAware);
		if (Operator.PARENT.equals(op))
			return new ParentStaircase(op, nodePositionAware);
		if (Operator.FOLLOWING.equals(op) || Operator.PRECEDING.equals(op))
			return new FollPrecStaircase(op, nodePositionAware);
		if (Operator.FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op))
			return new FolSibImFolSibStaircase(op, nodePositionAware);
		if (Operator.PRECEDING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op))
			return new PrecSibImPrecSibStaircase(op, nodePositionAware);
		if (Operator.IMMEDIATE_FOLLOWING.equals(op))
			return new ImFolStaircase(op, nodePositionAware);
		if (Operator.IMMEDIATE_PRECEDING.equals(op))
			return new ImPrecStaircase(op, nodePositionAware);
		return null;
	}

}