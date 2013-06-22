package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class Baseline2Join implements HalfPairJoin {
	public static final JoinBuilder JOIN_BUILDER = new Baseline2Builder();  
	private final LogicalNodePositionAware nodePositionAware;
	private final int positionLength;
	private final OperatorAware operatorAware;
	NodePositions result = new NodePositions();
	NodePositions buffer = new NodePositions();
	Operator op;

	public Baseline2Join(Operator op, LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		this.op = op;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		nodePositionAware.getAllPositions(buffer, node);
		return match(prev, op, buffer);
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		result.reset();
		for (next.offset = 0; next.offset < next.size; next.offset += positionLength) {
			for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
				if (op.match(prev, next, operatorAware)) {
					result.push(next, positionLength);
					break;
				}
			}
		}
		return result;
	}
	
}

class Baseline2Builder implements JoinBuilder {
	
	@Override
	public HalfPairJoin getHalfPairJoin(Operator op, LogicalNodePositionAware nodePositionAware) {
		return new Baseline2Join(op, nodePositionAware);
	}
	
}