package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;

/**
 * Joins the positions of a pair of nodes. Returns the positions from both nodes
 * that match the query
 * 
 * @author sumukh
 * 
 */
public interface FullPairJoin extends PairJoin {

	/**
	 * Ensure that all params and buffers have their offsets and sizes reset
	 * appropriately before passing them into this function
	 * 
	 * @param prev
	 * @param op
	 * @param node
	 * @param result
	 * @param buffers
	 * @throws IOException
	 */
	void match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, NodePairPositions result,
			NodePositions... buffers) throws IOException;
	
}
