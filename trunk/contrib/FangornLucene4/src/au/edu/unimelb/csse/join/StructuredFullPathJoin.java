package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.FullPairJoinPipeline.Pipe;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredFullPathJoin extends StructuredPathJoin implements ComputesFullResults {
	NodePairPositions pairResult;
	FullPairJoinPipeline execPipeline;
	Pipe start;

	public StructuredFullPathJoin(String[] labels, int[] parentPos,
			Operator[] operators, FullPairJoin join, LogicalNodePositionAware nodePositionAware) {
		super(labels, parentPos, operators, join, nodePositionAware);
		execPipeline = new FullPairJoinPipeline(nodePositionAware, join);
	}
	
	@Override
	public boolean setup(IndexReader r) throws IOException {
		boolean success = super.setup(r);
		if (!success)
			return false;
		start = execPipeline.createExecPipeline(root, operators);
		buffers = getBuffers(maxBufferSize(join, operators));
		execPipeline.setPrevAndBuffers(prev, buffers);
		return true;
	}
	
	int maxBufferSize(PairJoin join, Operator[] ops) {
		int max = 0;
		for (Operator op : ops) {
			int numBuf = join.numBuffers(op);
			if (numBuf > max) {
				max = numBuf;
			}
		}
		return max;
	}

	@Override
	public List<int[]> match() throws IOException {
		start.execute();
		return execPipeline.results;
	}

}
