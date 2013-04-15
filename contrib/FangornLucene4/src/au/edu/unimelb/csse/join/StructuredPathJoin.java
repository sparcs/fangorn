package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

abstract class StructuredPathJoin extends AbstractJoin {
	NodePositions[] buffers;
	NodePositions prev;
	LogicalNodePositionAware nodePositionAware;

	StructuredPathJoin(String[] labels, int[] parentPos,
			Operator[] operators, LogicalNodePositionAware nodePositionAware) {
		super(labels, parentPos, operators);
		this.nodePositionAware = nodePositionAware;
		prev = new NodePositions();
	}

	NodePositions[] getBuffers(int size) {
		NodePositions[] buffers = new NodePositions[size];
		for (int i = 0; i < size; i++) {
			buffers[i] = new NodePositions();
		}
		return buffers;
	}
	
	
	@Override
	public void setupPerDoc() throws IOException {
		// TODO: possible place to perform pipeline reorganisation for
		// optimisation?
	}


	
	
}
