package au.edu.unimelb.csse;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.join.NodePositions;
import au.edu.unimelb.csse.paypack.PayloadFormatException;

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
