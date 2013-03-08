package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.LogicalNodePositionAware;

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
public class StaircaseJoin extends AbstractPairwiseJoin implements HalfPairJoin {

	private LogicalNodePositionAware nodePostionAware;
	private int positionLength;
	private BinaryOperatorAware operatorAware;

	public StaircaseJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePostionAware = nodePositionAware;
		positionLength = nodePostionAware.getPositionLength();
		operatorAware = nodePositionAware.getBinaryOperatorHandler();
	}

	@Override
	public boolean check(BinaryOperator op) {
		return true;
	}

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			DocsAndPositionsEnum node, NodePositions... buffers)
			throws IOException {
		prune(prev, op, buffers);

		NodePositions result = buffers[0];
		NodePositions next = buffers[1];
		result.reset();
		next.reset();
		
		nodePostionAware.getAllPositions(next, node);

		if (BinaryOperator.FOLLOWING.equals(op) || BinaryOperator.PRECEDING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
				}
			}
		} else if (BinaryOperator.DESCENDANT.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					next.offset += positionLength;
				} else if (operatorAware.following(prev, next)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (BinaryOperator.ANCESTOR.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					next.offset += positionLength;
				} else if (operatorAware.startsAfter(prev, next)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (BinaryOperator.CHILD.equals(op)) {
			// similar to MPMG join but the marker is on poff here
			int pmark = 0;
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					next.offset += positionLength;
					prev.offset = pmark;
				} else if (operatorAware.following(prev, next)) {
					prev.offset += positionLength;
					pmark = prev.offset;
				} else if (operatorAware.descendant(prev, next)) {
					prev.offset += positionLength;
					if (prev.offset == prev.size) {
						next.offset += positionLength;
						prev.offset = pmark;
					}
				} else { // is preceding or ancestor
					next.offset += positionLength;
					prev.offset = pmark;
				}
			}
		} else if (BinaryOperator.PARENT.equals(op)) {
			// skip the first few precedings
			int pmark = 0;
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					next.offset += positionLength;
					prev.offset = pmark;
				} else if (pmark == prev.offset
						&& operatorAware.following(prev, next)) {
					prev.offset += positionLength;
					pmark = prev.offset;
				} else if (operatorAware.startsBefore(prev, next)) {
					next.offset += positionLength;
					prev.offset = pmark;
				} else {
					prev.offset += positionLength;
				}
				if (prev.offset >= prev.size && next.offset < next.size) {
					prev.offset = pmark;
					next.offset += positionLength;
				}
			}
		}
	}


	void prune(NodePositions prev, BinaryOperator op, NodePositions[] buffers) {
		if (prev.size <= 4)
			return;
		if (BinaryOperator.DESCENDANT.equals(op)) {
			pruneDescendant(prev, buffers);
		} else if (BinaryOperator.ANCESTOR.equals(op)) {
			pruneAncestor(prev, buffers);
		} else if (BinaryOperator.FOLLOWING.equals(op)) {
			pruneFollowing(prev, buffers);
		} else if (BinaryOperator.PRECEDING.equals(op)) {
			prunePreceding(prev, buffers);
		}
	}

	void pruneAncestor(NodePositions prev, NodePositions[] buffers) {
		NodePositions stack = buffers[0];
		NodePositions mark = buffers[1];
		NodePositions offsetStack = buffers[2];
		stack.push(prev, positionLength);
		offsetStack.pushInt(0);
		for (int i = positionLength; i < prev.size; i += positionLength) {
			prev.offset = i;
			if (operatorAware.descendant(stack, prev)) {
				stack.push(prev, positionLength);
				offsetStack.pushInt(i);
			} else {
				stack.pop(positionLength);
				mark.pushInt(offsetStack.popInt());
				boolean pushedToStack = false;
				while (stack.size > 0) {
					if (operatorAware.descendant(stack, prev)) {
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

	void pruneDescendant(NodePositions prev, NodePositions[] buffers) {
		NodePositions stack = buffers[0];
		NodePositions mark = buffers[1];
		stack.push(prev, positionLength);
		mark.pushInt(0);
		for (int i = positionLength; i < prev.size; i += positionLength) {
			prev.offset = i;
			if (!operatorAware.descendant(stack, prev)) {
				mark.pushInt(i);
				stack.pop(positionLength);
				stack.push(prev, positionLength);
			}
		}
		prev.retain(mark, positionLength);
	}

	void pruneFollowing(NodePositions prev, NodePositions[] buffers) {
		NodePositions stack = buffers[0];
		stack.push(prev, positionLength);
		int idx = 0;
		prev.offset = positionLength;;
		while (prev.offset < prev.size && operatorAware.descendant(stack, prev)) {
			stack.push(prev, positionLength);
			idx = prev.offset;
			prev.offset += positionLength;
		}
		prev.retain(idx, positionLength);
	}
	
	void prunePreceding(NodePositions prev, NodePositions[] buffers) {
		prev.retain(prev.size - positionLength, positionLength);
	}

	@Override
	public int numBuffers(BinaryOperator op) {
		if (BinaryOperator.ANCESTOR.equals(op)) {
			return 3;
		} 
		return 2;
	}

}
