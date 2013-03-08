package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.LogicalNodePositionAware;

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
	public void getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException {
		inner.getNextPosition(buffer, node);
	}

	@Override
	public int getPositionLength() {
		return inner.getPositionLength();
	}
	
	
}
