package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class Baseline1Join implements FullPairJoin {
	private final LogicalNodePositionAware nodePositionAware;
	private final int positionLength;
	private final OperatorAware operatorAware;
	NodePositions buffer = new NodePositions();

	public Baseline1Join(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
	}

	@Override
	public void match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, NodePairPositions result)
			throws IOException {
		result.reset();
		nodePositionAware.getAllPositions(buffer, node);
		for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
			for (buffer.offset = 0; buffer.offset < buffer.size; buffer.offset += positionLength) {
				if (op.match(prev, buffer, operatorAware)) {
					result.add(prev, buffer, positionLength);
				}
			}
		}
	}

}
