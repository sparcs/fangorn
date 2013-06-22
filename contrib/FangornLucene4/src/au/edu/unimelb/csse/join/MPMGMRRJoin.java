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
	NodePositions result = new NodePositions();
	Operator op;

	public MPMGMRRJoin(Operator op, LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		this.op = op;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
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
			pmark = doJoinResult(prev, pmark);
		}
		return result;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
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
	
	protected abstract int doJoinResult(NodePositions prev, int pmark);

	protected abstract int doJoin(NodePositions prev, NodePositions next, int pmark);
	
}

abstract class PositionRelationBased extends MPMGMRRJoin {

	public PositionRelationBased(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}
	
	@Override
	protected int doJoin(NodePositions prev, NodePositions next, int pmark) {
		Position position = operatorAware.positionRelation(
				prev.positions, prev.offset, next.positions, next.offset);
		while (loopCondition(position)) {
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
	protected int doJoinResult(NodePositions prev, int pmark) {
		Position position = operatorAware.positionRelation(
				prev.positions, prev.offset, result.positions,
				result.offset);
		while (loopCondition(position)) {
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
			if (matchCondition(position)) {
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
	
	abstract boolean loopCondition(Position position);
	
	abstract boolean matchCondition(Position position);
}

abstract class OperatorRelationBased extends MPMGMRRJoin {

	public OperatorRelationBased(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}
	
	@Override
	protected int doJoin(NodePositions prev, NodePositions next, int pmark) {
		Operator relation = operatorAware.mostRelevantOpRelation(
				prev.positions, prev.offset, next.positions, next.offset);
		while (loopCondition(relation)) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				return pmark;
			}
			relation = operatorAware.mostRelevantOpRelation(prev.positions,
					prev.offset, next.positions, next.offset);
		}
		if (checkMatch(prev, op, next, relation)) {
			result.push(next, positionLength);
		}
		return pmark;
	}
	
	@Override
	protected int doJoinResult(NodePositions prev, int pmark) {
		Operator relation = operatorAware.mostRelevantOpRelation(
				prev.positions, prev.offset, result.positions,
				result.offset);
		while (loopCondition(relation)) {
			// skip
			prev.offset += positionLength;
			pmark = prev.offset;
			if (prev.offset >= prev.size) {
				return pmark;
			}
			relation = operatorAware.mostRelevantOpRelation(prev.positions,
					prev.offset, result.positions, result.offset);
		}
		if (!checkMatch(prev, op, result, relation)) {
			result.removeLast(positionLength);
		}
		return pmark;
	}
	
	protected boolean checkMatch(NodePositions prev, Operator op,
			NodePositions next, Operator relation) {
		boolean found = false;
		while (prev.offset < prev.size) {
			if (matchCondition(relation)) {
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
	
	abstract boolean loopCondition(Operator relation);
	
	abstract boolean matchCondition(Operator relation);
	
}

class DescMPMGMRR extends PositionRelationBased {

	public DescMPMGMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	boolean loopCondition(Position position) {
		return Position.AFTER.equals(position);
	}

	@Override
	boolean matchCondition(Position position) {
		return Position.BELOW.equals(position);
	}
	
}

class ChildMPMGMRR extends OperatorRelationBased {

	public ChildMPMGMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	boolean loopCondition(Operator relation) {
		return Position.AFTER.equals(relation.getPosition());
	}

	@Override
	boolean matchCondition(Operator relation) {
		return Operator.CHILD.equals(relation);
	}
	
}

class AncMPMGMRR extends PositionRelationBased {

	public AncMPMGMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	boolean loopCondition(Position position) {
		return Position.AFTER.equals(position) || Position.BELOW.equals(position);
	}

	@Override
	boolean matchCondition(Position position) {
		return Position.ABOVE.equals(position);
	}
	
}

class ParMPMGMRR extends OperatorRelationBased {

	public ParMPMGMRR(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	@Override
	boolean loopCondition(Operator relation) {
		Position position = relation.getPosition();
		return Position.AFTER.equals(position) || Position.BELOW.equals(position);
	}

	@Override
	boolean matchCondition(Operator relation) {
		return Operator.PARENT.equals(relation);
	}
	
}

class MPMGMRRJoinBuilder implements JoinBuilder {

	@Override
	public HalfPairJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.DESCENDANT.equals(op)) {
			return new DescMPMGMRR(op, nodePositionAware);
		} else if (Operator.CHILD.equals(op)) {
			return new ChildMPMGMRR(op, nodePositionAware);
		} else if (Operator.ANCESTOR.equals(op)) {
			return new AncMPMGMRR(op, nodePositionAware);
		} else if (Operator.PARENT.equals(op)) {
			return new ParMPMGMRR(op, nodePositionAware);
		}
		return null;
	}
	
}
