package au.edu.unimelb.csse;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.join.NodePositions;

public interface LogicalNodePositionAware {
	BinaryOperatorAware getBinaryOperatorHandler();

	void getAllPositions(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;

	int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;
	
	int getPositionLength();
}
