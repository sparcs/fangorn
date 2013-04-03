package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class DocPosIter extends AbstractJoin {

	private LogicalNodePositionAware nodePositionsAware;
	private NodePositions buffer;

	public DocPosIter(String[] labels, LogicalNodePositionAware nodePositionAware) {
		super(labels, null);
		buffer = new NodePositions();
		this.nodePositionsAware = nodePositionAware;
	}

	@Override
	public void setupPerDoc() throws IOException {
	}
	
	public void readAllPositions() throws IOException {
		for (int i = 0; i < postingsFreqs.length; i++) {
			nodePositionsAware.getAllPositions(buffer, postingsFreqs[i].postings);
		}
		buffer.reset();
	}

}
