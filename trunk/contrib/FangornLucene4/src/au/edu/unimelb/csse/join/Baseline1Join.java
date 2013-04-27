package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class Baseline1Join extends AbstractPairJoin implements FullPairJoin {
	NodePositions buffer = new NodePositions();

	public Baseline1Join(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
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
