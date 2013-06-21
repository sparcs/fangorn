package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public abstract class AbstractLookaheadJoin extends AbstractPairJoin implements
		HalfPairLATEJoin {
	NodePositions result;
	NodePositions next;
	NodePositions buffer;

	AbstractLookaheadJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
		result = new NodePositions();
		next = new NodePositions();
		buffer = new NodePositions();
	}

	PruneOperation getFwdIterPruneOperation(NodePositions node, Operator nextOp) {
		if (result.size == 0) {
			return PruneOperation.JOIN_MATCH_ADD;
		} else if (Operator.DESCENDANT.equals(nextOp)) {
			if (operatorAware.descendant(result.positions, result.offset,
					node.positions, node.offset)) {
				return PruneOperation.PRUNE;
			}
		} else if (Operator.FOLLOWING.equals(nextOp)) {
			if (operatorAware.descendant(result.positions, result.offset,
					node.positions, node.offset)) {
				return PruneOperation.JOIN_MATCH_REPLACE;
			}
			return PruneOperation.PRUNE_STOP;
		} else if (Operator.ANCESTOR.equals(nextOp)) {
			if (operatorAware.descendant(result.positions, result.offset,
					node.positions, node.offset)) {
				return PruneOperation.JOIN_MATCH_REPLACE;
			}
		} else if (Operator.PRECEDING.equals(nextOp)) {
			return PruneOperation.JOIN_MATCH_REPLACE;
		}
		return PruneOperation.JOIN_MATCH_ADD;
	}

	PruneOperation getBwdIterPruneOperation(NodePositions node, Operator nextOp) {
		if (Operator.PRECEDING.equals(nextOp)) {
			return PruneOperation.JOIN_MATCH_ADD_STOP;
		} else if (result.size == 0) {
			return PruneOperation.JOIN_MATCH_ADD;
		} else if (Operator.ANCESTOR.equals(nextOp)) {
			if (operatorAware.ancestor(result.positions, 0, node.positions,
					node.offset)) {
				return PruneOperation.PRUNE;
			}
		} else if (Operator.DESCENDANT.equals(nextOp)) {
			if (operatorAware.ancestor(result.positions, 0, node.positions,
					node.offset)) {
				return PruneOperation.JOIN_MATCH_REPLACE_MANY;
			}
		} else if (Operator.FOLLOWING.equals(nextOp)) {
			if (operatorAware.ancestor(result.positions, 0, node.positions,
					node.offset)) {
				return PruneOperation.PRUNE;
			}
			return PruneOperation.JOIN_MATCH_REPLACE;
		}
		return PruneOperation.JOIN_MATCH_ADD;
	}

	@Override
	public NodePositions matchWithLookahead(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, Operator nextOp) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return matchWithLookahead(prev, op, next, nextOp);
	}

	public NodePositions matchTerminateEarly(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return matchTerminateEarly(prev, op, next);
	}

	public NodePositions matchWithLookahead(NodePositions prev, Operator op,
			NodePositions next, Operator nextOp) {
		result.reset();
		if (nextOp.equals(Operator.PRECEDING)
				|| nextOp.equals(Operator.ANCESTOR)) {
			return matchLookaheadBwdIter(prev, op, next, nextOp);
		}
		return matchLookaheadFwdIter(prev, op, next, nextOp);
	}
	
	protected abstract NodePositions matchLookaheadFwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp);
	
	protected abstract NodePositions matchLookaheadBwdIter(NodePositions prev,
			Operator op, NodePositions next, Operator nextOp);

	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return match(prev, op, next);
	}
}
