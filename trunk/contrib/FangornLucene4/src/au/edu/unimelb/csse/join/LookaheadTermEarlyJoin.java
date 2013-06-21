package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class LookaheadTermEarlyJoin extends AbstractLookaheadJoin {

	public LookaheadTermEarlyJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
	}

	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		prev.offset = 0;
		next.offset = 0;
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
		} else if (Operator.PARENT.equals(op)) {
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
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						break;
					}
					prev.offset += positionLength;
				}
			}
		} else if (Operator.FOLLOWING.equals(op)) {
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
		} else if (Operator.PRECEDING.equals(op)) {
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
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
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
					if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						break;
					}
				}
			}
		}
		return result;
	}

	@Override
	protected NodePositions matchLookaheadFwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		prev.offset = 0;
		next.offset = 0;
		if (Operator.DESCENDANT.equals(op)) {
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
					} else if (operatorAware.following(prev.positions,
							prev.offset, next.positions, next.offset)) {
						prev.offset += positionLength;
						continue;
					}
					break;
				}
			}
		} else if (Operator.ANCESTOR.equals(op)) {
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
		} else if (Operator.CHILD.equals(op)) {
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
		} else if (Operator.PARENT.equals(op)) {
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
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						break;
					}
					prev.offset += positionLength;
				}
			}
		} else if (Operator.FOLLOWING.equals(op)) {
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
					} else if (operatorAware.startsBefore(prev.positions, prev.offset, next.positions, next.offset)) {
						break;
					}
				}
			}
		} else if (Operator.PRECEDING.equals(op)) {
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
					} else if (operatorAware.startsAfter(prev.positions, prev.offset, next.positions, next.offset)) {
						poff += prev.offset + positionLength;
						break;
					}
				}
			}			
		} else if (Operator.FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_FOLLOWING.equals(op)) {
			for (; next.offset < next.size; next.offset += positionLength) {
				PruneOperation pruneOp = getFwdIterPruneOperation(next, nextOp);
				if (PruneOperation.PRUNE.equals(pruneOp)) {
					continue;
				} else if (PruneOperation.PRUNE_STOP.equals(pruneOp)) {
					break;
				}
				for (int j = 0; j < prev.size; j += positionLength) {
					if (op.match(prev.positions, j, next.positions,
							next.offset, operatorAware)) {
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
		} else if (Operator.PRECEDING_SIBLING.equals(op)
				|| Operator.IMMEDIATE_PRECEDING_SIBLING.equals(op)) {
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
					if (op.match(prev.positions, j, next.positions,
							next.offset, operatorAware)) {
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
		} else if (Operator.IMMEDIATE_PRECEDING.equals(op)) {
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
		return result;
	}

	@Override
	protected NodePositions matchLookaheadBwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp) {
		next.offset = next.size - positionLength;
		prev.offset = prev.size - positionLength;
		if (op.equals(Operator.DESCENDANT) || op.equals(Operator.CHILD) || op.equals(Operator.FOLLOWING)) {
			int start = prev.offset;
			for (;next.offset >= 0; next.offset -= positionLength) {
				PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
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
		} else if (op.equals(Operator.ANCESTOR) || op.equals(Operator.PARENT)) {
			int start = prev.offset;
			for (; next.offset >= 0; next.offset -= positionLength) {
				PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
				if (pruneOp.equals(PruneOperation.PRUNE)) {
					continue;
				}
				boolean matched = false;
				for (prev.offset = start; prev.offset >= 0; prev.offset -= positionLength) {
					if (op.match(prev.positions, prev.offset,
							next.positions, next.offset, operatorAware)) {
						result.insert(next, 0, positionLength);
						matched = true;
						break;
					} else if (operatorAware.startsAfter(prev.positions, prev.offset,
							next.positions, next.offset)) {
						break;
					}
				}
				if (matched && pruneOp.equals(PruneOperation.JOIN_MATCH_ADD_STOP)) {
					break;
				}
			}
		} else if (op.equals(Operator.PRECEDING)) {
			for (; next.offset >= 0; next.offset -= positionLength) {
				PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
				if (pruneOp.equals(PruneOperation.PRUNE)) {
					continue;
				}
				boolean matched = false;
				for (prev.offset = prev.size - positionLength; prev.offset >= 0; prev.offset -= positionLength) {
					if (operatorAware.ancestor(prev.positions, prev.offset, next.positions, next.offset) || 
							operatorAware.startsAfter(prev.positions, prev.offset, next.positions, next.offset)) {
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
		} else if (op.equals(Operator.IMMEDIATE_FOLLOWING)) {
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
		} else if (op.equals(Operator.FOLLOWING_SIBLING)
				|| op.equals(Operator.IMMEDIATE_FOLLOWING_SIBLING)) {
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
					if (op.match(prev.positions, j, next.positions,
							next.offset, operatorAware)) {
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
		} else if (op.equals(Operator.IMMEDIATE_PRECEDING)
				|| op.equals(Operator.PRECEDING_SIBLING)
				|| op.equals(Operator.IMMEDIATE_PRECEDING_SIBLING)) {
			for (; next.offset >= 0; next.offset -= positionLength) {
				PruneOperation pruneOp = getBwdIterPruneOperation(next, nextOp);
				if (pruneOp.equals(PruneOperation.PRUNE)) {
					continue;
				}
				boolean matched = false;

				for (prev.offset = prev.size - positionLength; prev.offset >= 0; prev.offset -= positionLength) {
					if (op.match(prev.positions,
							prev.offset, next.positions, next.offset, operatorAware)) {
						result.insert(next, 0, positionLength);
						matched = true;
						break;
					} else if (operatorAware.ancestor(prev.positions,
							prev.offset, next.positions, next.offset)
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
		return result;
	}

	@Override
	public NodePositions matchTerminateEarly(NodePositions prev, Operator op,
			NodePositions next) {
		prev.offset = 0;
		next.offset = 0;
		result.reset();
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
			int poff = 0;
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
		} else if (Operator.PARENT.equals(op)) {
			int poff = 0;
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
					} else if (operatorAware.preceding(prev.positions,
							prev.offset, next.positions, next.offset)) {
						break;
					}
					prev.offset += positionLength;
				}
			}
		} else if (Operator.FOLLOWING.equals(op)) {
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
		} else if (Operator.PRECEDING.equals(op)) {
			int pmark = 0;
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
					} else if (operatorAware.startsBefore(prev.positions, j,
							next.positions, i)) {
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

}
