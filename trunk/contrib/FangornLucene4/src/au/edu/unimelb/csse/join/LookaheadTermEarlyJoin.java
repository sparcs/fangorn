package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class LookaheadTermEarlyJoin extends AbstractPairJoin implements
		HalfPairLATEJoin {
	NodePositions next;
	NodePositions buffer;
	NodePositions result;

	public LookaheadTermEarlyJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
		next = new NodePositions();
		buffer = new NodePositions();
		result = new NodePositions();
	}

	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		prev.offset = 0;
		nodePositionAware.getAllPositions(next, node);
		next.offset = 0;
		doJoin(prev, op, next);
		return result;
	}

	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		prev.offset = 0;
		next.offset = 0;
		doJoin(prev, op, next);
		return result;
	}
	
	void doJoin(NodePositions prev, Operator op, NodePositions next) {
		result.reset();

		if (Operator.DESCENDANT.equals(op)) {
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
		} else if (Operator.FOLLOWING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						result.push(next, positionLength);
						break;
					} else if (operatorAware.startsBefore(prev.positions, prev.offset, next.positions, next.offset)) {
						break;
					}
				}
			}
		} else if (Operator.PRECEDING.equals(op)) {
			int pmark = 0;
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (pmark == prev.size) break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						result.push(next, positionLength);
						break;
					} else if (operatorAware.startsAfter(prev.positions, prev.offset, next.positions, next.offset)) {
						pmark = prev.offset + positionLength;
						break;
					}
				}
			}
		} else if (Operator.FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)) {
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
					} else if (operatorAware.following(prev.positions, j,
							next.positions, i)
							&& operatorAware.relativeDepth(prev.positions, j,
									next.positions, i) > 0) {
						continue;
					}
					break;
				}
			}
		} else if (Operator.PRECEDING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) {
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
					} else if (operatorAware.descendant(prev.positions, j,
							next.positions, i)
							|| operatorAware.relativeDepth(prev.positions, j,
									next.positions, i) > 0) {
						continue;
					}
					break;
				}
			}
		} else if (Operator.IMMEDIATE_FOLLOWING.equals(op)) {
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
//					if (operatorAware.ancestor(prev.positions, j,
//							next.positions, i)) {
//						break;
//					}
				}
			}
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
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
					if (operatorAware.immediatePreceding(prev.positions, j, next.positions, i)) {
						next.offset = i;
						result.push(next, positionLength);
						break;
					}
					if (operatorAware.descendant(prev.positions, j,
							next.positions, i)) {
						break;
					}
				}
			}
		}
	}

	public NodePositions matchWithLookahead(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, Operator nextOp) throws IOException {
		result.reset();
		prev.offset = 0;
		nodePositionAware.getAllPositions(next, node);
		next.offset = 0;
		boolean shouldContinue = true;
		if (Operator.DESCENDANT.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					shouldContinue = addToResultAndContinue(next, nextOp);
					if (!shouldContinue) {
						break;
					}
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
					shouldContinue = addToResultAndContinue(next, nextOp);
					if (!shouldContinue) {
						break;
					}
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
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.startsAfter(prev.positions,
							prev.offset, next.positions, next.offset)) {
						prev.offset += positionLength;
					} else { // N is preceding or ancestor
						break;
					}
				}
				if (!shouldContinue) {
					break;
				}
				next.offset += positionLength;
			}
		} else if (Operator.PARENT.equals(op)) {
			// skip the first few precedings
			while (next.offset < next.size) {
				prev.offset = 0;
				while (prev.offset < prev.size) {
					if (op.match(prev, next, operatorAware)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						// commenting out this else block will give the same
						// number of results but with fewer no. of comparisons
						break;
					}
					prev.offset += positionLength;
				}
				if (!shouldContinue)
					break;
				next.offset += positionLength;
			}
		} else if (Operator.FOLLOWING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.startsBefore(prev.positions, prev.offset, next.positions, next.offset)) {
						break;
					}
				}
				if (!shouldContinue) {
					break;
				}
			}
		} else if (Operator.PRECEDING.equals(op)) {
			int pmark = 0;
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (pmark == prev.size) break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.startsAfter(prev.positions, prev.offset, next.positions, next.offset)) {
						pmark = prev.offset + positionLength;
						break;
					}
				}
				if (!shouldContinue) {
					break;
				}
			}
		} else if (Operator.FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING.equals(op)) {
			for (int i = 0; i < next.size; i += positionLength) {
				for (int j = 0; j < prev.size; j += positionLength) {
					if (op.match(prev.positions, j, next.positions, i,
							operatorAware)) {
						next.offset = i;
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.startsBefore(prev.positions, j, next.positions, i)) {
						break;
					}
				}
				if (!shouldContinue)
					break;
			}
		} else if (Operator.PRECEDING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) {
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
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (operatorAware.descendant(prev.positions, j,
							next.positions, i)
							|| operatorAware.relativeDepth(prev.positions, j,
									next.positions, i) > 0) {
						continue;
					}
					break;
				}
				if (!shouldContinue)
					break;
			}
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
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
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					}
					if (operatorAware.descendant(prev.positions, j,
							next.positions, i)) {
						break;
					}
				}
				if (!shouldContinue)
					break;
			}
		}
		return result;
	}

	public NodePositions matchTerminateEarly(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		result.reset();
		prev.offset = 0;
		nodePositionAware.getAllPositions(next, node);
		next.offset = 0;
		boolean shouldContinue = true;
		if (Operator.DESCENDANT.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
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
					break;
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
						shouldContinue = false;
						break;
					} else if (operatorAware.startsAfter(prev.positions,
							prev.offset, next.positions, next.offset)) {
						prev.offset += positionLength;
					} else { // N is preceding or ancestor
						break;
					}
				}
				if (!shouldContinue) {
					break;
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
						shouldContinue = false;
						break;
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						// commenting out this else block will give the same
						// number of results but with fewer no. of comparisons
						break;
					}
					prev.offset += positionLength;
				}
				if (!shouldContinue)
					break;
				next.offset += positionLength;
			}
		} else if (Operator.FOLLOWING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (operatorAware.startsBefore(prev.positions, prev.offset, next.positions, next.offset)) {
						break;
					}
				}
				if (!shouldContinue) {
					break;
				}
			}
		} else if (Operator.PRECEDING.equals(op)) {
			int pmark = 0;
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (pmark == prev.size) break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					if (op.match(prev, next, operatorAware)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (operatorAware.startsAfter(prev.positions, prev.offset, next.positions, next.offset)) {
						pmark = prev.offset + positionLength;
						break;
					}
				}
				if (!shouldContinue) {
					break;
				}
			}
			
		} else if (Operator.FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING.equals(op)) {
			for (int i = 0; i < next.size; i += positionLength) {
				for (int j = 0; j < prev.size; j += positionLength) {
					if (op.match(prev.positions, j, next.positions, i,
							operatorAware)) {
						next.offset = i;
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (operatorAware.startsBefore(prev.positions, j, next.positions, i)) {
						break;
					}
				}
				if (!shouldContinue)
					break;
			}
		} else if (Operator.PRECEDING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) {
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
						shouldContinue = false;
						break;
					} else if (operatorAware.descendant(prev.positions, j,
							next.positions, i)
							|| operatorAware.relativeDepth(prev.positions, j,
									next.positions, i) > 0) {
						continue;
					}
					break;
				}
				if (!shouldContinue)
					break;
			}
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
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
						shouldContinue = false;
						break;
					}
					if (operatorAware.descendant(prev.positions, j,
							next.positions, i)) {
						break;
					}
				}
				if (!shouldContinue)
					break;
			}
		}
		return result;
	}

	private boolean addToResultAndContinue(NodePositions from, Operator nextOp) {
		if (result.size == 0) {
			result.push(from, positionLength);
		} else if (Operator.DESCENDANT.equals(nextOp)) {
			if (!operatorAware.descendant(result.positions, result.offset,
					from.positions, from.offset)) {
				result.push(from, positionLength);
			}
		} else if (Operator.ANCESTOR.equals(nextOp)) {
			if (operatorAware.descendant(result.positions, result.offset,
					from.positions, from.offset)) {
				result.removeLast(positionLength);
			} 
			result.push(from, positionLength);
		} else if (Operator.FOLLOWING.equals(nextOp)) {
			if (operatorAware.descendant(result.positions, result.offset,
					from.positions, from.offset)) {
				result.removeLast(positionLength);
				result.push(from, positionLength);
				return true;
			} else {
				return false;
			}
		} else if (Operator.PRECEDING.equals(nextOp)) {
			result.removeLast(positionLength);
			result.push(from, positionLength);
		} else {
			result.push(from, positionLength);
		}
		return true;
	}

}
