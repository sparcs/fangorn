package au.edu.unimelb.csse.paypack;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.join.NodePositions;

public interface LogicalNodePositionAware {
	BinaryOperatorAware getBinaryOperatorHandler();

	void getAllPositions(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;

	int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;

	int getPositionLength();

	BytesRef[] encode(int[] positions, int numTokens)
			throws PayloadFormatException;
}
