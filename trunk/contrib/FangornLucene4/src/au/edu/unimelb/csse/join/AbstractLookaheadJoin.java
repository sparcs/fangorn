package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public abstract class AbstractLookaheadJoin implements
		HalfPairLATEJoin {
	protected final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final OperatorAware operatorAware;
	NodePositions result;
	NodePositions next;
	NodePositions buffer;
	Operator op;


	AbstractLookaheadJoin(Operator op, LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		result = new NodePositions();
		next = new NodePositions();
		buffer = new NodePositions();
		this.op = op;
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
	public NodePositions matchWithLookahead(NodePositions prev, DocsAndPositionsEnum node,
			Operator nextOp) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return matchWithLookahead(prev, next, nextOp);
	}

	public NodePositions matchTerminateEarly(NodePositions prev, DocsAndPositionsEnum node) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return matchTerminateEarly(prev, next);
	}

	public NodePositions matchWithLookahead(NodePositions prev, NodePositions next,
			Operator nextOp) {
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

	public NodePositions match(NodePositions prev, DocsAndPositionsEnum node) throws IOException {
		nodePositionAware.getAllPositions(next, node);
		return match(prev, next);
	}
}
