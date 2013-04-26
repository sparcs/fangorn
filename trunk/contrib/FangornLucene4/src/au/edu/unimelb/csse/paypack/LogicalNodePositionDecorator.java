package au.edu.unimelb.csse.paypack;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.CountingOperatorAware;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.join.NodePositions;

public class LogicalNodePositionDecorator implements LogicalNodePositionAware{
	private LogicalNodePositionAware inner;
	private CountingOperatorAware countingOperatorAware;
	
	public LogicalNodePositionDecorator(LogicalNodePositionAware inner) {
		this.inner = inner;
		this.countingOperatorAware = new CountingOperatorAware(inner.getOperatorHandler());
	}
	
	public CountingOperatorAware getCountingOperatorAware() {
		return countingOperatorAware;
	}

	@Override
	public OperatorAware getOperatorHandler() {
		return countingOperatorAware;
	}

	@Override
	public void getAllPositions(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		inner.getAllPositions(buffer, node);
	}

	@Override
	public int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		return inner.getNextPosition(buffer, node);
	}

	@Override
	public int getPositionLength() {
		return inner.getPositionLength();
	}

	@Override
	public BytesRef[] encode(int[] positions, int numTokens)
			throws PayloadFormatException {
		return inner.encode(positions, numTokens);
	}

	@Override
	public boolean isTreeRootPosition(int[] positions, int offset) {
		return inner.isTreeRootPosition(positions, offset);
	}

	@Override
	public int compare(int[] a1, int o1, int[] a2, int o2) {
		return inner.compare(a1, o1, a2, o2);
	}
	
	
}
