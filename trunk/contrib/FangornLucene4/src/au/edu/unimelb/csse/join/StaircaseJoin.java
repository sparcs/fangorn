package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
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
public class StaircaseJoin extends AbstractPairJoin implements HalfPairJoin {
	//only Ancestor operator uses 3 buffers, rest use 2
	NodePositions[] buffers = new NodePositions[] { new NodePositions(),
			new NodePositions(), new NodePositions() };

	public StaircaseJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
	}

	@Override
	public boolean check(Operator op) {
		return true;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		prev.offset = 0;
		prune(prev, op);
		NodePositions next = buffers[1];
		next.reset();
		nodePositionAware.getAllPositions(next, node);
		doJoin(prev, op, next);
		return buffers[0];
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op, NodePositions next) throws IOException {
		prev.offset = 0;
		prune(prev, op);
		next.offset = 0;
		doJoin(prev, op, next);
		return buffers[0];
	}

	private void doJoin(NodePositions prev, Operator op, NodePositions next) {
		NodePositions result = buffers[0];
		result.reset();

		if (Operator.FOLLOWING.equals(op) || Operator.PRECEDING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
				}
			}
		} else if (Operator.DESCENDANT.equals(op)) {
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
		} else if (Operator.ANCESTOR.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					next.offset += positionLength;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (Operator.CHILD.equals(op)) {
			// similar to MPMG join but the marker is on poff here
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
		} else if (Operator.PARENT.equals(op)) {
			// skip the first few precedings
			while (next.offset < next.size) {
				prev.offset = 0;
				while (prev.offset < prev.size) {
					if (op.match(prev, next, operatorAware)) {
						result.push(next, positionLength);
						break;
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						// commenting out this else block will give the same
						// number of results but with fewer no. of comparisons
						break;
					}
					prev.offset += positionLength;
				}
				next.offset += positionLength;
			}
		}
	}

	void prune(NodePositions prev, Operator op) {
		if (prev.size <= 4)
			return;
		if (Operator.DESCENDANT.equals(op)) {
			pruneDescendant(prev);
		} else if (Operator.ANCESTOR.equals(op)) {
			pruneAncestor(prev);
		} else if (Operator.FOLLOWING.equals(op)) {
			pruneFollowing(prev);
		} else if (Operator.PRECEDING.equals(op)) {
			prunePreceding(prev);
		}
	}

	void pruneAncestor(NodePositions prev) {
		NodePositions stack = buffers[0];
		NodePositions mark = buffers[1];
		NodePositions offsetStack = buffers[2];
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

	void pruneDescendant(NodePositions prev) {
		NodePositions stack = buffers[0];
		NodePositions mark = buffers[1];
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

	void pruneFollowing(NodePositions prev) {
		int idx = 0;
		prev.offset = positionLength;
		while (prev.offset < prev.size
				&& operatorAware.descendant(prev.positions, idx,
						prev.positions, prev.offset)) {
			idx = prev.offset;
			prev.offset += positionLength;
		}
		prev.retain(idx, positionLength);
	}

	void prunePreceding(NodePositions prev) {
		prev.retain(prev.size - positionLength, positionLength);
	}

}