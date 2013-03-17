package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

abstract class StructuredPathJoin extends AbstractJoin {
	NodePositions[] buffers;
	NodePositions prev;
	LogicalNodePositionAware nodePositionAware;

	StructuredPathJoin(String[] labels, int[] parentPos,
			BinaryOperator[] operators, PairJoin join, LogicalNodePositionAware nodePositionAware) {
		super(labels, parentPos, operators);
		prev = new NodePositions();
		this.nodePositionAware = nodePositionAware;
	}

	
	@Override
	public void setupPerDoc() throws IOException {
		// TODO: possible place to perform pipeline reorganisation for
		// optimisation?
	}


	public void setupBuffers(int maxBufferSize) {
		buffers = new NodePositions[maxBufferSize];
		for (int i = 0; i < buffers.length; i++) {
			buffers[i] = new NodePositions();
		}
	}
	
}
