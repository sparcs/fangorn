package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.join.HalfPairLATEJoin.PruneOperation;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public abstract class LookaheadTermEarlyJoin extends AbstractLookaheadJoin {
	public static final LATEJoinBuilder JOIN_BUILDER = new LookaheadTermEarlyJoinBuilder(); 

	public LookaheadTermEarlyJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		prev.offset = 0;
		next.offset = 0;
		result.reset();
		doMatch(prev, next);
		return result;
	}

	@Override
	protected NodePositions matchLookaheadFwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		prev.offset = 0;
		next.offset = 0;
		doMatchLookaheadFwdIter(prev, next, nextOp);
		return result;
	}

	@Override
	protected NodePositions matchLookaheadBwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		next.offset = next.size - positionLength;
		prev.offset = prev.size - positionLength;
		doMatchLookaheadBwdIter(prev, next, nextOp);
		return result;
	}

	@Override
	public NodePositions matchTerminateEarly(NodePositions prev, Operator op,
			NodePositions next) {
		prev.offset = 0;
		next.offset = 0;
		result.reset();
		doMatchTerminateEarly(prev, next);
		return result;
	}

	protected abstract void doMatch(NodePositions prev, NodePositions next);

	protected abstract void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp);

	protected abstract void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp);

	protected abstract void doMatchTerminateEarly(NodePositions prev,
			NodePositions next);

}

class DescLATE extends LookaheadTermEarlyJoin {
	DescFolCommonLATE descFol;

	public DescLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		descFol = new DescFolCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
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
				if (operatorAware.descendant(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (operatorAware.following(prev.positions, prev.offset,
						next.positions, next.offset)) {
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
		descFol.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
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
	}

}

class AncLATE extends LookaheadTermEarlyJoin {
	AncParCommonLATE ancPar;

	public AncLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		ancPar = new AncParCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
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
				if (operatorAware.ancestor(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
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
		ancPar.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		while (next.offset < next.size && prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				result.push(next, positionLength);
				break;
			} else if (operatorAware.startsAfter(prev.positions, prev.offset,
					next.positions, next.offset)) {
				prev.offset += positionLength;
			} else {
				next.offset += positionLength;
			}
		}
	}

}

class ChildLATE extends LookaheadTermEarlyJoin {

	public ChildLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			while (prev.offset < prev.size
					&& operatorAware.following(prev.positions, prev.offset,
							next.positions, next.offset)) {
				prev.offset += positionLength;
				poff = prev.offset;
			}
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
		}
	}

	@Override
	protected void doMatchLookaheadFwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		int poff = prev.offset;
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			prev.offset = poff;
			while (prev.offset < prev.size
					&& operatorAware.following(prev.positions, prev.offset,
							next.positions, next.offset)) {
				prev.offset += positionLength;
				poff = prev.offset;
			}
			while (prev.offset < prev.size) {
				if (operatorAware.child(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
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
			PruneOperation pruneOp = getBwdIterPruneOperation(next,
					nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				if (operatorAware.startsBefore(prev.positions, prev.offset,
						next.positions, next.offset)) {
					start = prev.offset - positionLength;
					continue;
				} else if (op.match(prev.positions, prev.offset,
						next.positions, next.offset, operatorAware)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (operatorAware.descendant(prev.positions, prev.offset, next.positions, next.offset)) {
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
		for (; shouldContinue && next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			while (prev.offset < prev.size
					&& operatorAware.following(prev.positions, prev.offset,
							next.positions, next.offset)) {
				prev.offset += positionLength;
				poff = prev.offset;
			}
			while (prev.offset < prev.size) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
				} else {
					break;
				}
			}
		}
	}

}

class ParLATE extends LookaheadTermEarlyJoin {
	AncParCommonLATE ancPar;

	public ParLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		ancPar = new AncParCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			while (prev.offset < prev.size) {
				if (operatorAware.parent(prev.positions, prev.offset,
						next.positions, next.offset)) {
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
					poff = prev.offset;
					continue;
				} else if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
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
				if (operatorAware.parent(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
					poff = prev.offset;
					continue;
				} else if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
					break;
				}
				prev.offset += positionLength;
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		ancPar.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		int poff = 0;
		boolean shouldContinue = true;
		for (; shouldContinue && next.offset < next.size; next.offset += positionLength) {
			prev.offset = poff;
			while (prev.offset < prev.size) {
				if (operatorAware.parent(prev.positions, prev.offset,
						next.positions, next.offset)) {
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
					poff = prev.offset;
					continue;
				} else if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
					break;
				}
				prev.offset += positionLength;
			}
		}
	}

}

class FolLATE extends LookaheadTermEarlyJoin {
	DescFolCommonLATE descFol;

	public FolLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		descFol = new DescFolCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		for (; next.offset < next.size; next.offset += positionLength) {
			for (; prev.offset < prev.size; prev.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsBefore(prev.positions,
						prev.offset, next.positions, next.offset)) {
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
				if (operatorAware.following(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsBefore(prev.positions,
						prev.offset, next.positions, next.offset)) {
					break;
				}
			}
		}
	}

	@Override
	protected void doMatchLookaheadBwdIter(NodePositions prev,
			NodePositions next, Operator nextOp) {
		descFol.bwdIterJoin(prev, next, result, nextOp);
	}

	@Override
	protected void doMatchTerminateEarly(NodePositions prev, NodePositions next) {
		boolean shouldContinue = true;
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (operatorAware.startsBefore(prev.positions,
						prev.offset, next.positions, next.offset)) {
					break;
				}
			}
			if (!shouldContinue) {
				break;
			}
		}
	}

}

class PrecLATE extends LookaheadTermEarlyJoin {

	public PrecLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int pmark = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			if (pmark == prev.size)
				break;
			for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
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
				if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
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
				if (operatorAware.ancestor(prev.positions, prev.offset,
						next.positions, next.offset)
						|| operatorAware.startsAfter(prev.positions,
								prev.offset, next.positions, next.offset)) {
					break;
				} else if (op.match(prev.positions, prev.offset,
						next.positions, next.offset, operatorAware)) {
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
		int pmark = 0;
		boolean shouldContinue = true;
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			if (pmark == prev.size)
				break;
			for (prev.offset = pmark; prev.offset < prev.size; prev.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
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

class FolSibImFolSibLATE extends LookaheadTermEarlyJoin {
	FolSibImFolSibImFolCommonLATE common;

	public FolSibImFolSibLATE(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new FolSibImFolSibImFolCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
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
				while (j >= 0
						&& operatorAware.startsBefore(prev.positions, j,
								next.positions, next.offset)) {
					j -= positionLength;
					start = j;
				}
				if (j < 0)
					break;
				if (op.match(prev.positions, j, next.positions, next.offset,
						operatorAware)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (operatorAware.following(prev.positions, j,
						next.positions, next.offset)
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

class ImFolLATE extends LookaheadTermEarlyJoin {
	FolSibImFolSibImFolCommonLATE common;

	public ImFolLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new FolSibImFolSibImFolCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
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
				} else if (operatorAware.descendant(prev.positions, j,
						next.positions, i)) {
					if (!operatorAware.isLeftAligned(prev.positions, j,
							next.positions, i)) {
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
				while (j >= 0
						&& operatorAware.startsBefore(prev.positions, j,
								next.positions, next.offset)) {
					j -= positionLength;
					start = j;
				}
				if (j < 0)
					break;
				if (operatorAware.immediateFollowing(prev.positions, j,
						next.positions, next.offset)) {
					result.insert(next, 0, positionLength);
					matched = true;
				} else if (operatorAware.descendant(prev.positions, j,
						next.positions, next.offset)) {
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

class PrecSibImPrecSibLATE extends LookaheadTermEarlyJoin {
	PrecSibImPrecSibImPrecCommonLATE common;

	public PrecSibImPrecSibLATE(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new PrecSibImPrecSibImPrecCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
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
				} else if (operatorAware.descendant(prev.positions, j,
						next.positions, i)
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
				while (j < prev.size
						&& operatorAware.startsAfter(prev.positions, j,
								next.positions, next.offset)) {
					j += positionLength;
					start = j;
				}
				if (j >= prev.size) {
					break;
				}
				if (op.match(prev.positions, j, next.positions, next.offset,
						operatorAware)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
				} else if (operatorAware.descendant(prev.positions, j,
						next.positions, next.offset)
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
	}

}

class ImPrecLATE extends LookaheadTermEarlyJoin {
	PrecSibImPrecSibImPrecCommonLATE common;

	public ImPrecLATE(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
		common = new PrecSibImPrecSibImPrecCommonLATE(this, op);
	}

	@Override
	protected void doMatch(NodePositions prev, NodePositions next) {
		int poff = 0;
		for (; next.offset < next.size; next.offset += positionLength) {
			for (prev.offset = poff; prev.offset < prev.size; prev.offset += positionLength) {
				while (prev.offset < prev.size
						&& operatorAware.startsAfter(prev.positions,
								prev.offset, next.positions, next.offset)) {
					prev.offset += positionLength;
					poff = prev.offset;
				}
				if (prev.offset >= prev.size) {
					break;
				}
				if (operatorAware.immediatePreceding(prev.positions,
						prev.offset, next.positions, next.offset)) {
					result.push(next, positionLength);
					break;
				}
				if (operatorAware.preceding(prev.positions, prev.offset,
						next.positions, next.offset)) {
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
				while (j < prev.size
						&& operatorAware.startsAfter(prev.positions, j,
								next.positions, next.offset)) {
					j += positionLength;
					start = j;
				}
				if (j >= prev.size) {
					break;
				}
				if (operatorAware.immediatePreceding(prev.positions, j,
						next.positions, next.offset)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsAfter(prev.positions, j,
						next.positions, next.offset)) {
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
		int start = 0;
		boolean shouldContinue = true;
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
				if (operatorAware.descendant(prev.positions, j, next.positions,
						i)) {
					break;
				}
			}
			if (!shouldContinue)
				break;
		}
	}

}

class DescFolCommonLATE {
	LookaheadTermEarlyJoin parentJoin;
	OperatorAware operatorAware;
	int positionLength;
	Operator op;

	public DescFolCommonLATE(LookaheadTermEarlyJoin join, Operator op) {
		parentJoin = join;
		this.op = op;
		operatorAware = parentJoin.operatorAware;
		positionLength = parentJoin.positionLength;
	}

	void bwdIterJoin(NodePositions prev, NodePositions next,
			NodePositions result, Operator nextOp) {
		int start = prev.offset;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = parentJoin.getBwdIterPruneOperation(next,
					nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				if (operatorAware.startsBefore(prev.positions, prev.offset,
						next.positions, next.offset)) {
					start -= positionLength;
					continue;
				} else if (op.match(prev.positions, prev.offset,
						next.positions, next.offset, operatorAware)) {
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

class AncParCommonLATE {
	LookaheadTermEarlyJoin parentJoin;
	OperatorAware operatorAware;
	int positionLength;
	Operator op;

	public AncParCommonLATE(LookaheadTermEarlyJoin join, Operator op) {
		parentJoin = join;
		this.op = op;
		operatorAware = parentJoin.operatorAware;
		positionLength = parentJoin.positionLength;
	}

	void bwdIterJoin(NodePositions prev, NodePositions next,
			NodePositions result, Operator nextOp) {
		int start = prev.offset;
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = parentJoin.getBwdIterPruneOperation(next,
					nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;
			for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
				if (op.match(prev.positions, prev.offset, next.positions,
						next.offset, operatorAware)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (operatorAware.startsAfter(prev.positions,
						prev.offset, next.positions, next.offset)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}

	}
}

class FolSibImFolSibImFolCommonLATE {
	LookaheadTermEarlyJoin parentJoin;
	OperatorAware operatorAware;
	int positionLength;
	Operator op;

	public FolSibImFolSibImFolCommonLATE(LookaheadTermEarlyJoin join, Operator op) {
		parentJoin = join;
		this.op = op;
		operatorAware = parentJoin.operatorAware;
		positionLength = parentJoin.positionLength;
	}

	void fwdIterJoin(NodePositions prev, NodePositions next,
			NodePositions result, Operator nextOp) {
		for (; next.offset < next.size; next.offset += positionLength) {
			PruneOperation pruneOp = parentJoin.getFwdIterPruneOperation(next,
					nextOp);
			if (PruneOperation.PRUNE.equals(pruneOp)) {
				continue;
			} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
				break;
			}
			for (int j = 0; j < prev.size; j += positionLength) {
				if (op.match(prev.positions, j, next.positions, next.offset,
						operatorAware)) {
					if (PruneOperation.JOIN_MATCH_REPLACE.equals(pruneOp)) {
						result.removeLast(positionLength);
					}
					result.push(next, positionLength);
					break;
				} else if (operatorAware.startsBefore(prev.positions, j,
						next.positions, next.offset)) {
					break;
				}
			}
		}
	}

	void termEarlyJoin(NodePositions prev, NodePositions next,
			NodePositions result) {
		boolean shouldContinue = true;
		for (int i = 0; i < next.size; i += positionLength) {
			for (int j = 0; j < prev.size; j += positionLength) {
				if (op.match(prev.positions, j, next.positions, i,
						operatorAware)) {
					next.offset = i;
					result.push(next, positionLength);
					shouldContinue = false;
					break;
				} else if (operatorAware.startsBefore(prev.positions, j,
						next.positions, i)) {
					break;
				}
			}
			if (!shouldContinue)
				break;
		}
	}
}

class PrecSibImPrecSibImPrecCommonLATE {
	LookaheadTermEarlyJoin parentJoin;
	OperatorAware operatorAware;
	int positionLength;
	Operator op;

	public PrecSibImPrecSibImPrecCommonLATE(LookaheadTermEarlyJoin join, Operator op) {
		parentJoin = join;
		this.op = op;
		operatorAware = parentJoin.operatorAware;
		positionLength = parentJoin.positionLength;
	}

	void bwdIterJoin(NodePositions prev, NodePositions next,
			NodePositions result, Operator nextOp) {
		for (; next.offset >= 0; next.offset -= positionLength) {
			PruneOperation pruneOp = parentJoin.getBwdIterPruneOperation(next,
					nextOp);
			if (pruneOp.equals(PruneOperation.PRUNE)) {
				continue;
			}
			boolean matched = false;

			for (prev.offset = prev.size - positionLength; prev.offset >= 0; prev.offset -= positionLength) {
				if (op.match(prev.positions, prev.offset, next.positions,
						next.offset, operatorAware)) {
					result.insert(next, 0, positionLength);
					matched = true;
					break;
				} else if (operatorAware.ancestor(prev.positions, prev.offset,
						next.positions, next.offset)
						|| operatorAware.startsAfter(prev.positions,
								prev.offset, next.positions, next.offset)) {
					break;
				}
			}
			if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
				break;
			}
		}
	}
}

class LookaheadTermEarlyJoinBuilder implements LATEJoinBuilder {

	@Override
	public HalfPairLATEJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.DESCENDANT.equals(op)) return new DescLATE(op, nodePositionAware);
		if (Operator.ANCESTOR.equals(op)) return new AncLATE(op, nodePositionAware);
		if (Operator.CHILD.equals(op)) return new ChildLATE(op, nodePositionAware);
		if (Operator.PARENT.equals(op)) return new ParLATE(op, nodePositionAware);
		if (Operator.FOLLOWING.equals(op)) return new FolLATE(op, nodePositionAware);
		if (Operator.PRECEDING.equals(op)) return new PrecLATE(op, nodePositionAware);
		if (Operator.IMMEDIATE_FOLLOWING.equals(op)) return new ImFolLATE(op, nodePositionAware);
		if (Operator.FOLLOWING_SIBLING.equals(op) || Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)) return new FolSibImFolSibLATE(op, nodePositionAware);
		if (Operator.IMMEDIATE_PRECEDING.equals(op)) return new ImPrecLATE(op, nodePositionAware);
		if (Operator.PRECEDING_SIBLING.equals(op) || Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) return new PrecSibImPrecSibLATE(op, nodePositionAware);
		return null;
	}
	
}
