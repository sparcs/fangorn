package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.Position;
import au.edu.unimelb.csse.join.HalfPairLATEJoin.PruneOperation;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public abstract class LookaheadTermEarlyMRRJoin extends AbstractLookaheadJoin {
	public static final LATEJoinBuilder JOIN_BUILDER = new LookaheadTermEarlyMRRJoinBuilder();

	public LookaheadTermEarlyMRRJoin(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	public NodePositions match(NodePositions prev, NodePositions next) throws IOException {
		prev.offset = 0;
		next.offset = 0;
		result.reset();
		doMatch(prev, next);
		return result;
	}

	protected NodePositions matchLookaheadFwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		prev.offset = 0;
		next.offset = 0;
		doMatchLookaheadFwdIter(prev, next, nextOp);
		return result;
	}

	protected NodePositions matchLookaheadBwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		next.offset = next.size - positionLength;
		prev.offset = prev.size - positionLength;
		doMatchLookaheadBwdIter(prev, next, nextOp);
		return result;
	}
	
	@Override
	public NodePositions matchTerminateEarly(NodePositions prev, NodePositions next) {
		prev.offset = 0;
		next.offset = 0;
		result.reset();
		doMatchTerminateEarly(prev, next);
		return result;
	}
	
	protected abstract void doMatch(NodePositions prev, NodePositions next);
	
	protected abstract void doMatchLookaheadFwdIter(NodePositions prev, NodePositions next, Operator nextOp);
	
	protected abstract void doMatchLookaheadBwdIter(NodePositions prev, NodePositions next, Operator nextOp);
	
	protected abstract void doMatchTerminateEarly(NodePositions prev, NodePositions next);

}

class DescLATEMRR extends LookaheadTermEarlyMRRJoin {
	DescFolCommonLATEMRR common;

	public DescLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new DescFolCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
			if (Position.BELOW.equals(position)) {
				result.push(next, positionLength);
				next.offset += positionLength;
			} else if (Position.AFTER.equals(position)) {
				prev.offset += positionLength;
			} else {
				next.offset += positionLength;
			}
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			while (prev.offset < prev.size) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				if (Position.BELOW.equals(position)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					continue;
				}
				break;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions,
					next.offset);
			if (Position.BELOW.equals(position)) {
				result.push(next, positionLength);
				break;
			} else if (Position.AFTER.equals(position)) {
				prev.offset += positionLength;
			} else {
				next.offset += positionLength;
			}
		}
	}
}

class AncLATEMRR extends LookaheadTermEarlyMRRJoin {

	public AncLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
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
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			while (prev.offset < prev.size) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				if (Position.ABOVE.equals(position)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					continue;
				}
				break;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = prev.offset;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
				if (Position.ABOVE.equals(position)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions,
					next.offset);
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
	}
}

class ChildLATEMRR extends LookaheadTermEarlyMRRJoin {

	public ChildLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			boolean beginning = true;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (beginning) {
					if (position.equals(Position.AFTER)) {
						prev.offset += positionLength;
						poff = prev.offset;
						continue;
					} 
					beginning = false;
				}
				if (Operator.CHILD.equals(relation)) {
					result.push(next, positionLength);
					break;
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
					prev.offset += positionLength;
				} else { 
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			prev.offset = poff;
			boolean beginning = true;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (beginning) {
					if (position.equals(Position.AFTER)) {
						prev.offset += positionLength;
						poff = prev.offset;
						continue;
					} 
					beginning = false;
				}
				if (Operator.CHILD.equals(relation)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					continue;
				}  
				break;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = prev.offset;
		for (;next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(prev.positions, prev.offset, next.positions, next.offset);
				Position position = relation.getPosition();
				if (Position.ABOVE.equals(position) || Position.BEFORE.equals(position)) {
					start = prev.offset - positionLength;
					continue;
				} else if (Operator.CHILD.equals(relation)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (Position.BELOW.equals(position)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		int poff = 0;
		boolean shouldContinue = true;
		for (; next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			boolean beginning = true;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (beginning) {
					if (position.equals(Position.AFTER)) {
						prev.offset += positionLength;
						poff = prev.offset;
						continue;
					} 
					beginning = false;
				}
				if (Operator.CHILD.equals(relation)) {
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					continue;
				}  
				break;
			}
			if (!shouldContinue) {
				break;
			}
		}
	}
	
}

class ParLATEMRR extends LookaheadTermEarlyMRRJoin {

	public ParLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) { 
			prev.offset = poff;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				Position position = relation.getPosition();
				if (Operator.PARENT.equals(relation)) {
					result.push(next, positionLength);
					break;
				} else if (Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					poff = prev.offset;
					continue;
				} else if (Position.BEFORE.equals(position)) {
					break;
				}
				prev.offset += positionLength;
			}
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			prev.offset = poff;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions,
						next.offset); 
				Position position = relation.getPosition();
				if (Operator.PARENT.equals(relation)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (Position.BELOW.equals(position)
						|| Position.AFTER.equals(position)) {
					prev.offset += positionLength;
					poff = prev.offset;
					continue;
				} else if (Position.BEFORE.equals(position)) {
					break;
				}
				prev.offset += positionLength;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = prev.offset;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(prev.positions, prev.offset, next.positions, next.offset);
				Position position = relation.getPosition();
				if (Operator.PARENT.equals(relation)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		boolean shouldContinue = true;
		while (next.offset < next.size) {
			prev.offset = 0;
			while (prev.offset < prev.size) {
				Operator relation = operatorAware.mostRelevantOpRelation(
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
	}
}

class FolLATEMRR extends LookaheadTermEarlyMRRJoin {
	DescFolCommonLATEMRR common;

	public FolLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new DescFolCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		for (; next.offset < next.size; next.offset += positionLength) {
			for (; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
				if (Position.AFTER.equals(position)) {
					result.push(next, positionLength);
					break;
				} else if (Position.ABOVE.equals(position)
						|| Position.BEFORE.equals(position)) {
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset); 
				if (Position.AFTER.equals(position)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (Position.ABOVE.equals(position) || Position.BEFORE.equals(position)) {
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		boolean shouldContinue = true;
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
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
	}
}

class PrecLATEMRR extends LookaheadTermEarlyMRRJoin {

	public PrecLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int pmark = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			if (pmark == prev.size)
				break;
			for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
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
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (prev.offset = poff; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
				if (Position.BEFORE.equals(position)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					poff += prev.offset + positionLength;
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = prev.size - positionLength; prev.offset >= 0; prev.offset -= positionLength) {
				Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
				if (Position.ABOVE.equals(position) || 
						Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					break;
				} else if (Position.BEFORE.equals(position)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}	
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		boolean shouldContinue = true;
		int pmark = 0;
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			if (pmark == prev.size)
				break;
			for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
				Position position = operatorAware.positionRelation(
						prev.positions, prev.offset, next.positions,
						next.offset);
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
	}
}

class FolSibImFolSibLATEMRR extends LookaheadTermEarlyMRRJoin {
	FolSibImFolSibImFolCommonLATEMRR common;

	public FolSibImFolSibLATEMRR(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new FolSibImFolSibImFolCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int start = prev.size - positionLength;
		for (int i = next.size - positionLength; i >= 0; i -= positionLength) {
			for (int j = start; j >= 0; j -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j >= 0
						&& (Position.ABOVE.equals(position) || Position.BEFORE
								.equals(position))) {
					j -= positionLength;
					start = j;
					if (j >= 0) {
						relation = operatorAware.mostRelevantOpRelation(
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
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.fwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = prev.size - positionLength;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (int j = start; j >= 0; j -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, next.offset);
				Position position = relation.getPosition();
				while (j >= 0
						&& (Position.ABOVE.equals(position) || Position.BEFORE.equals(position))) {
					j -= positionLength;
					start = j;
					if (j >= 0) {
						relation = operatorAware.mostRelevantOpRelation(
								prev.positions, j, next.positions, next.offset);
						position = relation.getPosition();
					}
				}
				if (j < 0)
					break;
				if (op.equals(relation) || (op.equals(Operator.FOLLOWING_SIBLING) && relation.equals(Operator.IMMEDIATE_FOLLOWING_SIBLING))) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (Position.AFTER.equals(position)
						&& operatorAware.relativeDepth(prev.positions, j,
								next.positions, next.offset) > 0) {
					continue;
				}
				break;
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		common.termEarlyJoin(prev, next, result);
	}
	
}

class PrecSibImPrecSibLATEMRR extends LookaheadTermEarlyMRRJoin {
	PrecSibImPrecSibImPrecCommonLATEMRR common;

	public PrecSibImPrecSibLATEMRR(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new PrecSibImPrecSibImPrecCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int start = 0;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
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
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, next.offset);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
								prev.positions, j, next.positions, next.offset);
						position = relation.getPosition();
					}
				}
				if (j >= prev.size) {
					break;
				}
				if (op.equals(relation)
						|| (Operator.PRECEDING_SIBLING.equals(op) && Operator.IMMEDIATE_PRECEDING_SIBLING
								.equals(relation))) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (Position.BELOW.equals(position)
						|| operatorAware.relativeDepth(prev.positions, j,
								next.positions, next.offset) > 0) {
					continue;
				}
				break;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		int start = 0;
		boolean shouldContinue = true;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
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
	}
}

class ImFolLATEMRR extends LookaheadTermEarlyMRRJoin {
	FolSibImFolSibImFolCommonLATEMRR common;

	public ImFolLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new FolSibImFolSibImFolCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int start = prev.size - positionLength;
		for (int i = next.size - positionLength; i >= 0; i -= positionLength) {
			for (int j = start; j >= 0; j -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j >= 0
						&& (Position.ABOVE.equals(position) || Position.BEFORE
								.equals(position))) {
					j -= positionLength;
					start = j;
					if (j >= 0) {
						relation = operatorAware.mostRelevantOpRelation(
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
				} else if (Position.BELOW.equals(position)) {
					if (!operatorAware.isLeftAligned(prev.positions, prev.offset, next.positions, next.offset)) {
						break;
					}
					continue;
				}
				break;
			}
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.fwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = prev.size - positionLength;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (int j = start; j >= 0; j -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, next.offset);
				Position position = relation.getPosition();
				while (j >= 0
						&& (Position.ABOVE.equals(position) || Position.BEFORE.equals(position))) {
					j -= positionLength;
					start = j;
					if (j >= 0) {
						relation = operatorAware.mostRelevantOpRelation(
								prev.positions, j, next.positions, next.offset);
						position = relation.getPosition();
					}
				}
				if (j < 0)
					break;
				if (Operator.IMMEDIATE_FOLLOWING.equals(relation) || Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(relation)) {
					result.insert(next, 0, positionLength);
					matched = true;
				} else if (Position.BELOW.equals(position)) {
					if (!operatorAware.isLeftAligned(prev.positions, j,
							next.positions, next.offset)) {
						break;
					}
					continue;
				}
				break;
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		common.termEarlyJoin(prev, next, result);
	}
}

class ImPrecLATEMRR extends LookaheadTermEarlyMRRJoin {
	PrecSibImPrecSibImPrecCommonLATEMRR common;

	public ImPrecLATEMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new PrecSibImPrecSibImPrecCommonLATEMRR(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int start = 0;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
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

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int start = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, next.offset);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
								prev.positions, j, next.positions, next.offset);
						position = relation.getPosition();
					}
				}
				if (j >= prev.size) {
					break;
				}
				if (op.equals(relation)
						|| Operator.IMMEDIATE_PRECEDING_SIBLING
								.equals(relation)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				}
				if (Position.AFTER.equals(position)) {
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		common.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		boolean shouldContinue = true;
		int start = 0;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = start; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				while (j < prev.size
						&& (Position.BELOW.equals(position) || Position.AFTER
								.equals(position))) {
					j += positionLength;
					start = j;
					if (j < prev.size) {
						relation = operatorAware.mostRelevantOpRelation(
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
				if (Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					break;
				}
			}
			if (!shouldContinue)
				break;
		}

	}
	
}

class DescFolCommonLATEMRR {
	private LookaheadTermEarlyMRRJoin parentJoin;
	private OperatorAware operatorAware;
	private int positionLength;
	private Operator op;

	public DescFolCommonLATEMRR(LookaheadTermEarlyMRRJoin parent, Operator op) {
		parentJoin = parent;
		operatorAware = parent.operatorAware;
		positionLength = parent.positionLength;
		this.op = op;
	}
	
	void bwdIterJoin(NodePositions prev, NodePositions next, NodePositions result, Operator nextOp) {
		int start = prev.offset;
		for (;next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = parentJoin.getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				Position position = operatorAware.positionRelation(prev.positions, prev.offset, next.positions, next.offset);
				if (Position.ABOVE.equals(position) || Position.BEFORE.equals(position)) {
					start -= positionLength;
					continue;
				} else if (Position.BELOW.equals(position) && op.equals(Operator.DESCENDANT) || Position.AFTER.equals(position) && op.equals(Operator.FOLLOWING)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}
}

class FolSibImFolSibImFolCommonLATEMRR {
	private LookaheadTermEarlyMRRJoin parentJoin;
	private OperatorAware operatorAware;
	private int positionLength;
	private Operator op;

	public FolSibImFolSibImFolCommonLATEMRR(LookaheadTermEarlyMRRJoin parent, Operator op) {
		parentJoin = parent;
		operatorAware = parent.operatorAware;
		positionLength = parent.positionLength;
		this.op = op;
	}
	
	void fwdIterJoin(NodePositions prev, NodePositions next, NodePositions result, Operator nextOp) {
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = parentJoin.getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (int j = 0; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, next.offset);
				Position position = relation.getPosition();
				if (op.equals(relation)
						|| (Operator.IMMEDIATE_FOLLOWING_SIBLING
								.equals(relation) && (Operator.FOLLOWING_SIBLING
								.equals(op) || Operator.IMMEDIATE_FOLLOWING
								.equals(op)))) {

					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (Position.ABOVE.equals(position)
						|| Position.BEFORE.equals(position)) {
					break;
				}
			}
		}
	}
	
	void termEarlyJoin(NodePositions prev, NodePositions next, NodePositions result) {
		boolean shouldContinue = true;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = 0; j < prev.size; j += positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, j, next.positions, i);
				Position position = relation.getPosition();
				if (op.equals(relation)
						|| (Operator.IMMEDIATE_FOLLOWING_SIBLING
								.equals(relation) && (Operator.FOLLOWING_SIBLING
								.equals(op) || Operator.IMMEDIATE_FOLLOWING
								.equals(op)))) {
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
	}
}

class PrecSibImPrecSibImPrecCommonLATEMRR {
	private LookaheadTermEarlyMRRJoin parentJoin;
	private OperatorAware operatorAware;
	private int positionLength;
	private Operator op;

	public PrecSibImPrecSibImPrecCommonLATEMRR(LookaheadTermEarlyMRRJoin parent, Operator op) {
		parentJoin = parent;
		operatorAware = parent.operatorAware;
		positionLength = parent.positionLength;
		this.op = op;
	}
	
	void bwdIterJoin(NodePositions prev, NodePositions next, NodePositions result, Operator nextOp) {
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = parentJoin.getBwdIterPruneOperation(next, nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;

			for (prev.offset = prev.size - positionLength; prev.offset >= 0; prev.offset -= positionLength) {
				Operator relation = operatorAware.mostRelevantOpRelation(
						prev.positions, prev.offset, next.positions, next.offset);
				Position position = relation.getPosition();
				if (op.equals(relation)
						|| (Operator.IMMEDIATE_PRECEDING_SIBLING
								.equals(relation) && (Operator.PRECEDING_SIBLING
								.equals(op) || Operator.IMMEDIATE_PRECEDING
								.equals(op)))) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (Position.ABOVE.equals(position)
						|| Position.BELOW.equals(position) || Position.AFTER.equals(position)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}
}

class LookaheadTermEarlyMRRJoinBuilder implements LATEJoinBuilder {

	@Override
	public HalfPairLATEJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.DESCENDANT.equals(op)) return new DescLATEMRR(op, nodePositionAware);
		if (Operator.ANCESTOR.equals(op)) return new AncLATEMRR(op, nodePositionAware);
		if (Operator.CHILD.equals(op)) return new ChildLATEMRR(op, nodePositionAware);
		if (Operator.PARENT.equals(op)) return new ParLATEMRR(op, nodePositionAware);
		if (Operator.FOLLOWING.equals(op)) return new FolLATEMRR(op, nodePositionAware);
		if (Operator.PRECEDING.equals(op)) return new PrecLATEMRR(op, nodePositionAware);
		if (Operator.IMMEDIATE_FOLLOWING.equals(op)) return new ImFolLATEMRR(op, nodePositionAware);
		if (Operator.IMMEDIATE_PRECEDING.equals(op)) return new ImPrecLATEMRR(op, nodePositionAware);
		if (Operator.FOLLOWING_SIBLING.equals(op) || Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)) return new FolSibImFolSibLATEMRR(op, nodePositionAware);
		if (Operator.PRECEDING_SIBLING.equals(op) || Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) return new PrecSibImPrecSibLATEMRR(op, nodePositionAware);
		return null;
	}
	
}
