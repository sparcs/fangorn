package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;


/**
 * Joins the positions of a pair of nodes. Returns only the second nodes
 * positions that match the query.
 * 
 * @author sumukh
 * 
 */
public interface HalfPairJoin {

	/**
	 * Ensure that all params and buffers have their offsets and sizes set
	 * appropriately before passing them into this function
	 * 
	 * @param prev
	 * @param node
	 * @param buffers
	 * @throws IOException
	 */
	NodePositions match(NodePositions prev, DocsAndPositionsEnum node) throws IOException;

	NodePositions match(NodePositions prev, NodePositions next)
			throws IOException;
}
