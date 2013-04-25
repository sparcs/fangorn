package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.Position;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class LookaheadTermEarlyMRRJoin extends AbstractPairJoin implements
		HalfPairLATEJoin {

	NodePositions next;
	NodePositions buffer;
	NodePositions result;

	LookaheadTermEarlyMRRJoin(LogicalNodePositionAware nodePositionAware) {
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
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.BELOW.equals(position)) {
					result.push(next, positionLength);
					next.offset += positionLength;
				} else if (Position.AFTER.equals(position)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (Operator.ANCESTOR.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.ABOVE.equals(position)) {
					result.push(next, positionLength);
					next.offset += positionLength;
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Operator.CHILD.equals(relation)) {
						result.push(next, positionLength);
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Operator.PARENT.equals(relation)) {
						result.push(next, positionLength);
						break;
					} else if (Position.BEFORE.equals(position)) {
						break;
					}
					prev.offset += positionLength;
				}
				next.offset += positionLength;
			}
		} else if (Operator.FOLLOWING.equals(op)) {
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.AFTER.equals(position)) {
						result.push(next, positionLength);
						break;
					} else if (Position.ABOVE.equals(position)
							|| Position.BEFORE.equals(position)) {
						break;
					}
				}
			}
		} else if (Operator.PRECEDING.equals(op)) {
			int pmark = 0;
			for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
				if (pmark == prev.size)
					break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.BEFORE.equals(position)) {
						result.push(next, positionLength);
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j >= 0
							&& (Position.ABOVE.equals(position) || Position.BEFORE
									.equals(position))) {
						j -= positionLength;
						start = j;
						if (j >= 0) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j < 0)
						break;
					if (op.equals(relation)
							|| (Operator.FOLLOWING_SIBLING.equals(op) && Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation))) {
						next.offset = i;
						result.insert(next, 0, positionLength);
						break;
					} else if (Position.AFTER.equals(position)
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(relation)
							|| (Operator.PRECEDING_SIBLING.equals(op) && Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation))) {
						next.offset = i;
						result.push(next, positionLength);
						break;
					} else if (Position.BELOW.equals(position)
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j >= 0
							&& (Position.ABOVE.equals(position) || Position.BEFORE
									.equals(position))) {
						j -= positionLength;
						start = j;
						if (j >= 0) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j < 0)
						break;
					if (Operator.IMMEDIATE_FOLLOWING.equals(relation)
							|| Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation)) {
						next.offset = i;
						result.insert(next, 0, positionLength);
						break;
					}
					// if (Position.ABOVE.equals(position)) {
					// break;
					// }
				}
			}
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
			int start = 0;
			for (int i = 0; i < next.size; i += positionLength) {
				for (int j = start; j < prev.size; j += positionLength) {
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(relation)
							|| Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation)) {
						next.offset = i;
						result.push(next, positionLength);
						break;
					}
					if (Position.BEFORE.equals(position)) {
						break;
					}
				}
			}
		}
	}

	public NodePositions matchWithLookahead(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, Operator nextOp) throws IOException {
		result.reset();
		nodePositionAware.getAllPositions(next, node);
		prev.offset = 0;
		next.offset = 0;
		boolean shouldContinue = true;
		if (Operator.DESCENDANT.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.BELOW.equals(position)) {
					shouldContinue = addToResultAndContinue(next, nextOp);
					if (!shouldContinue) {
						break;
					}
					next.offset += positionLength;
				} else if (Position.AFTER.equals(position)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (Operator.ANCESTOR.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.ABOVE.equals(position)) {
					shouldContinue = addToResultAndContinue(next, nextOp);
					if (!shouldContinue) {
						break;
					}
					next.offset += positionLength;
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Operator.CHILD.equals(relation)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Operator.PARENT.equals(relation)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.BEFORE.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.AFTER.equals(position)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.ABOVE.equals(position)
							|| Position.BEFORE.equals(position)) {
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
				if (pmark == prev.size)
					break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.BEFORE.equals(position)) {
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					if (op.equals(relation)
							|| (Operator.FOLLOWING_SIBLING.equals(op) && Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation))
							|| (Operator.IMMEDIATE_FOLLOWING.equals(op) && Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation))) {
						next.offset = i;
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.ABOVE.equals(position)
							|| Position.BEFORE.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(relation)
							|| (Operator.PRECEDING_SIBLING.equals(op) && Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation))) {
						next.offset = i;
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					} else if (Position.BELOW.equals(position)
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(relation)
							|| Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation)) {
						next.offset = i;
						shouldContinue = addToResultAndContinue(next, nextOp);
						break;
					}
					if (Position.BELOW.equals(position)) {
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
		nodePositionAware.getAllPositions(next, node);
		prev.offset = 0;
		next.offset = 0;
		boolean shouldContinue = true;
		if (Operator.DESCENDANT.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.BELOW.equals(position)) {
					result.push(next, positionLength);
					break;
				} else if (Position.AFTER.equals(position)) {
					prev.offset += positionLength;
				} else {
					next.offset += positionLength;
				}
			}
		} else if (Operator.ANCESTOR.equals(op)) {
			while (next.offset < next.size && prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Position.ABOVE.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (op.equals(relation)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (op.equals(relation)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.BEFORE.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.AFTER.equals(position)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.ABOVE.equals(position)
							|| Position.BEFORE.equals(position)) {
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
				if (pmark == prev.size)
					break;
				for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, prev.offset, next.positions,
							next.offset);
					Position position = relation.getPosition();
					if (Position.BEFORE.equals(position)) {
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.BELOW.equals(position)
							|| Position.AFTER.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					if (op.equals(relation)
							|| (Operator.FOLLOWING_SIBLING.equals(op) && Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation))
							|| (Operator.IMMEDIATE_FOLLOWING.equals(op) && Operator.IMMEDIATE_FOLLOWING_SIBLING
									.equals(relation))) {
						next.offset = i;
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.ABOVE.equals(position)
							|| Position.BEFORE.equals(position)) {
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(relation)
							|| (Operator.PRECEDING_SIBLING.equals(op) && Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation))) {
						next.offset = i;
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					} else if (Position.BELOW.equals(position)
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
					Operator relation = operatorAware.mostRelevantRelation(
							prev.positions, j, next.positions, i);
					Position position = relation.getPosition();
					while (j < prev.size
							&& (Position.BELOW.equals(position) || Position.AFTER
									.equals(position))) {
						j += positionLength;
						start = j;
						if (j < prev.size) {
							relation = operatorAware.mostRelevantRelation(
									prev.positions, j, next.positions, i);
							position = relation.getPosition();
						}
					}
					if (j >= prev.size) {
						break;
					}
					if (op.equals(position)
							|| Operator.IMMEDIATE_PRECEDING_SIBLING
									.equals(relation)) {
						next.offset = i;
						result.push(next, positionLength);
						shouldContinue = false;
						break;
					}
					if (Position.BELOW.equals(position)) {
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
