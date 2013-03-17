package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;
import au.edu.unimelb.csse.paypack.PayloadFormatException;

public class LogicalNodePositionDecorator implements LogicalNodePositionAware{
	private LogicalNodePositionAware inner;
	private CountingBinaryOperatorAware countingOperatorAware;
	
	public LogicalNodePositionDecorator(LogicalNodePositionAware inner) {
		this.inner = inner;
		this.countingOperatorAware = new CountingBinaryOperatorAware(inner.getBinaryOperatorHandler());
	}
	
	CountingBinaryOperatorAware getCountingBinaryOperator() {
		return countingOperatorAware;
	}

	@Override
	public BinaryOperatorAware getBinaryOperatorHandler() {
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
	
	
}
