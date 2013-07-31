package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.Position;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public abstract class MPMGMRRJoin implements HalfPairJoin {
	public static final MPMGMRRJoinBuilder JOIN_BUILDER = new MPMGMRRJoinBuilder();
	private final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final OperatorAware operatorAware;
	protected final Position[] skipPositions;
	NodePositions result = new NodePositions();
	Operator op;

	public MPMGMRRJoin(Operator op, LogicalNodePositionAware nodePositionAware, Position[] skipPositions) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		this.skipPositions = skipPositions; 
		this.op = op;
	}

	@Override
	public NodePositions match(NodePositions prev, DocsAndPositionsEnum node)
			throws IOException {
		int freq = node.freq();
		result.reset();
		int numNextRead = 0;
		int pmark = 0;
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePositionAware.getNextPosition(result, node);
			numNextRead++;
			prev.offset = pmark;
			pmark = doJoin(prev, pmark);
		}
		return result;
	}

	@Override
	public NodePositions match(NodePositions prev, NodePositions next)
			throws IOException {
		result.reset();
		next.offset = 0;
		int pmark = 0;
		while (next.offset < next.size) {
			if (pmark == prev.size)
				break;
			prev.offset = pmark;
			pmark = doJoin(prev, next, pmark);
			next.offset += positionLength;
		}
		return result;
	}
	
	boolean skipPosition(Position position) {
		for (Position pos : skipPositions) {
			if (pos.equals(position))
				return true;
		}
		return false;
	}

	protected abstract int doJoin(NodePositions prev, int pmark);

	protected abstract int doJoin(NodePositions prev, NodePositions next,
			int pmark);

}

class PositionRelationBased extends MPMGMRRJoin {
	private final Position matchPosition;

	public PositionRelationBased(Operator op,
			LogicalNodePositionAware nodePositionAware,
			Position[] skipPositions, Position matchPosition) {
		super(op, nodePositionAware, skipPositions);
		this.matchPosition = matchPosition;
	}

	@Override
	protected int doJoin(NodePositions prev, NodePositions next, int pmark) {
		Position position = operatorAware.positionRelation(prev.positions,
				prev.offset, next.positions, next.offset);
		while (skipPosition(position)) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				return pmark;
			}
			position = operatorAware.positionRelation(prev.positions,
					prev.offset, next.positions, next.offset);
		}
		if (checkMatch(prev, next, position)) {
			result.push(next, positionLength);
		}
		return pmark;
	}

	@Override
	protected int doJoin(NodePositions prev, int pmark) {
		Position position = operatorAware.positionRelation(prev.positions,
				prev.offset, result.positions, result.offset);
		while (skipPosition(position)) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				result.removeLast(positionLength);
				return pmark;
			}
			position = operatorAware.positionRelation(prev.positions,
					prev.offset, result.positions, result.offset);
		}
		if (!checkMatch(prev, result, position)) {
			result.removeLast(positionLength);
		}
		return pmark;
	}

	protected boolean checkMatch(NodePositions prev, NodePositions next,
			Position position) {
		boolean found = false;
		while (prev.offset < prev.size) {
			if (matchPosition.equals(position)) {
				found = true;
				break; // solution found; abort
			} else if (Position.BEFORE.equals(position)) {
				// prev is after
				break;
			}
			prev.offset += positionLength;
			if (prev.offset < prev.size) {
				position = operatorAware.positionRelation(prev.positions,
						prev.offset, next.positions, next.offset);
			}
		}
		return found;
	}
}

class OperatorRelationBased extends MPMGMRRJoin {
	private final Operator matchRelation;

	public OperatorRelationBased(Operator op,
			LogicalNodePositionAware nodePositionAware,
			Position[] skipPositions, Operator matchRelation) {
		super(op, nodePositionAware, skipPositions);
		this.matchRelation = matchRelation;
	}

	@Override
	protected int doJoin(NodePositions prev, NodePositions next, int pmark) {
		Operator relation = operatorAware.mostRelevantOpRelation(
				prev.positions, prev.offset, next.positions, next.offset);
		while (skipPosition(relation.getPosition())) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				return pmark;
			}
			relation = operatorAware.mostRelevantOpRelation(prev.positions,
					prev.offset, next.positions, next.offset);
		}
		if (checkMatch(prev, next, relation)) {
			result.push(next, positionLength);
		}
		return pmark;
	}

	@Override
	protected int doJoin(NodePositions prev, int pmark) {
		Operator relation = operatorAware.mostRelevantOpRelation(
				prev.positions, prev.offset, result.positions, result.offset);
		while (skipPosition(relation.getPosition())) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				result.removeLast(positionLength); // TODO: test for the
													// presence of this line
				return pmark;
			}
			relation = operatorAware.mostRelevantOpRelation(prev.positions,
					prev.offset, result.positions, result.offset);
		}
		if (!checkMatch(prev, result, relation)) {
			result.removeLast(positionLength);
		}
		return pmark;
	}

	protected boolean checkMatch(NodePositions prev, NodePositions next,
			Operator relation) {
		boolean found = false;
		while (prev.offset < prev.size) {
			if (matchRelation.equals(relation)) {
				found = true;
				break; // solution found; abort
			} else if (Position.BEFORE.equals(relation.getPosition())) {
				// prev is after
				break;
			}
			prev.offset += positionLength;
			if (prev.offset < prev.size) {
				relation = operatorAware.mostRelevantOpRelation(prev.positions,
						prev.offset, next.positions, next.offset);
			}
		}
		return found;
	}

}

class MPMGMRRJoinBuilder implements JoinBuilder {

	@Override
	public HalfPairJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.DESCENDANT.equals(op)) {
			return new PositionRelationBased(op, nodePositionAware,
					new Position[] { Position.AFTER }, Position.BELOW);
		} else if (Operator.CHILD.equals(op)) {
			return new OperatorRelationBased(op, nodePositionAware,
					new Position[] { Position.AFTER }, Operator.CHILD);
		} else if (Operator.ANCESTOR.equals(op)) {
			return new PositionRelationBased(op, nodePositionAware,
					new Position[] { Position.AFTER, Position.BELOW },
					Position.ABOVE);
		} else if (Operator.PARENT.equals(op)) {
			return new OperatorRelationBased(op, nodePositionAware,
					new Position[] { Position.AFTER, Position.BELOW },
					Operator.PARENT);
		}
		return null;
	}

}
