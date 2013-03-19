package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;

/**
 * Joins the positions of a pair of nodes. Returns only the second nodes
 * positions that match the query.
 * 
 * @author sumukh
 * 
 */
public interface HalfPairJoin extends PairJoin {

	/**
	 * Ensure that all params and buffers have their offsets and sizes set
	 * appropriately before passing them into this function
	 * 
	 * @param prev
	 * @param op
	 * @param node
	 * @param buffers
	 * @throws IOException
	 */
	void match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, NodePositions... buffers)
			throws IOException;

	void match(NodePositions prev, Operator op, NodePositions next,
			NodePositions... buffers) throws IOException;
}
