package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class Baseline2Join extends AbstractPairJoin implements HalfPairJoin {
	NodePositions result = new NodePositions();
	NodePositions buffer = new NodePositions();

	public Baseline2Join(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
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
		for (prev.offset = 0; prev.offset < prev.size; prev.offset += positionLength) {
			for (buffer.offset = 0; buffer.offset < buffer.size; buffer.offset += positionLength) {
				if (op.match(prev, buffer, operatorAware)) {
					result.push(buffer, positionLength);
				}
			}
		}
		return result;
	}
}
