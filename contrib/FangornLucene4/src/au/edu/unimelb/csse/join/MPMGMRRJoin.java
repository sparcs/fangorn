package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.Position;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class MPMGMRRJoin extends AbstractPairJoin implements HalfPairJoin {
	NodePositions result = new NodePositions();

	public MPMGMRRJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		int freq = node.freq();
		result.reset();
		int numNextRead = 0;
		int pmark = 0;
		boolean descOrChildOp = Operator.DESCENDANT.equals(op)
				|| Operator.CHILD.equals(op);
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePositionAware.getNextPosition(result, node);
			numNextRead++;
			prev.offset = pmark;
			Operator relation = operatorAware.mostRelevantOpRelation(
					prev.positions, prev.offset, result.positions,
					result.offset);
			while (Position.AFTER.equals(relation.getPosition())
					|| !descOrChildOp
					&& Position.BELOW.equals(relation.getPosition())) {
				// skip
				prev.offset += positionLength;
				pmark = prev.offset;
				if (prev.offset >= prev.size) {
					break;
				}
				relation = operatorAware.mostRelevantOpRelation(prev.positions,
						prev.offset, result.positions, result.offset);
			}
			if (!checkMatch(prev, op, result, relation)) {
				result.removeLast(positionLength);
			}
		}
		return result;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		result.reset();
		next.offset = 0;
		int pmark = 0;
		boolean descOrChildOp = Operator.DESCENDANT.equals(op)
				|| Operator.CHILD.equals(op);
		while (next.offset < next.size) {
			if (pmark == prev.size)
				break;
			prev.offset = pmark;
			Operator relation = operatorAware.mostRelevantOpRelation(
					prev.positions, prev.offset, next.positions, next.offset);
			while (Position.AFTER.equals(relation.getPosition())
					|| !descOrChildOp
					&& Position.BELOW.equals(relation.getPosition())) {
				// skip
				prev.offset += positionLength;
				pmark = prev.offset;
				if (prev.offset >= prev.size) {
					break;
				}
				relation = operatorAware.mostRelevantOpRelation(prev.positions,
						prev.offset, next.positions, next.offset);
			}
			if (checkMatch(prev, op, next, relation)) {
				result.push(next, positionLength);
			}
			next.offset += positionLength;
		}
		return result;
	}

	private boolean checkMatch(NodePositions prev, Operator op,
			NodePositions next, Operator relation) {
		boolean found = false;
		while (prev.offset < prev.size) {
			if (op.equals(relation) || Operator.DESCENDANT.equals(op)
					&& Operator.CHILD.equals(relation)
					|| Operator.ANCESTOR.equals(op)
					&& Operator.PARENT.equals(relation)) {
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
