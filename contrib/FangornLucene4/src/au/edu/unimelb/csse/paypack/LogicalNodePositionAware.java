package au.edu.unimelb.csse.paypack;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.join.NodePositions;

public interface LogicalNodePositionAware {
	OperatorAware getOperatorHandler();

	/**
	 * Reads all positions of node into buffer. Resets offset to 0 in the buffer 
	 * @param buffer
	 * @param node
	 * @throws IOException
	 */
	void getAllPositions(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;

	int getNextPosition(NodePositions buffer, DocsAndPositionsEnum node)
			throws IOException;
	
	boolean isTreeRootPosition(int[] positions, int offset);

	int getPositionLength();
	
	BytesRef[] encode(int[] positions, int numTokens)
			throws PayloadFormatException;

	int compare(int[] pos1, int off1, int[] pos2, int off2);
}
