package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.BooleanJoinPipeline.Pipe;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredBooleanPathJoin extends StructuredPathJoin implements
		ComputesBooleanResult {
	BooleanJoinPipeline execPipeline;
	Pipe start;

	public StructuredBooleanPathJoin(String[] labels, int[] parentPos,
			Operator[] operators, BooleanJoinPipeline execPipeline,
			LogicalNodePositionAware nodePositionAware) {
		super(labels, parentPos, operators, nodePositionAware);
		this.execPipeline = execPipeline;
	}

	@Override
	public boolean setup(IndexReader r) throws IOException {
		boolean success = super.setup(r);
		if (!success)
			return false;
		start = execPipeline.createExecPipeline(root, operators);
		execPipeline.setPrevBuffer(prev);
		return true;
	}

	@Override
	public boolean match() throws IOException {
		final NodePositions result = start.execute();
		return result.size > 0;
	}
	
	@Override
	public void setupPerAtomicContext() {
		super.setupPerAtomicContext();
		start = execPipeline.createExecPipeline(root, operators);
	}

}
