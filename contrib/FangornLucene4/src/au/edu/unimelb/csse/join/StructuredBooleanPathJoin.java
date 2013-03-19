package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.Pipe;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredBooleanPathJoin extends StructuredPathJoin implements
		ComputesBooleanResult {
	HalfPairJoinPipeline execPipeline;
	Pipe start;

	public StructuredBooleanPathJoin(String[] labels, int[] parentPos,
			Operator[] operators, HalfPairJoin join,
			LogicalNodePositionAware nodePositionAware) {
		super(labels, parentPos, operators, join, nodePositionAware);
		execPipeline = new HalfPairJoinPipeline(nodePositionAware, join);
	}

	@Override
	public boolean setup(IndexReader r) throws IOException {
		boolean success = super.setup(r);
		if (!success)
			return false;
		start = execPipeline.createExecPipeline(root, operators);
		buffers = getBuffers(execPipeline.getMaxBufferSize());
		execPipeline.setPrevAndBuffers(prev, buffers);
		return true;
	}

	@Override
	public boolean match() throws IOException {
		final NodePositions result = start.execute();
		return result.size > 0;
	}

}
