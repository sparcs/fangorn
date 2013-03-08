package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.LogicalNodePositionAware;

/**
 * This is an adaptation of the MPMGJN join by Zhang et.al. (2001)
 * 
 * Zhang, Chun, Jeffrey Naughton, David DeWitt, Qiong Luo, and Guy Lohman. 2001.
 * On supporting containment queries in relational database management systems.
 * In Proceedings of the 2001 ACM SIGMOD International Conference on Management
 * of Data, 425--436, New York. ACM.
 * 
 * This implementation deviates from the original algorithm for node position
 * labels. Here the trees nodes are labelled using LPath's labelling scheme by
 * Bird et.al. (2005)
 * 
 * Bird, Steven G., Yi Chen, Susan B. Davidson, Haejoong Lee, and Yifeng Zheng.
 * 2005. Extending xpath to support linguistic queries. In Proceedings of
 * Programming Language Technologies for XML (PLANX), 35--46, Long Beach, USA.
 * 
 * This implementation iterates over the second join term. The skipping
 * optimizations is only applicable when the positions to be skipped appear
 * before all non skip positions
 * 
 * Additionally, this join also only returns the second elements as the result
 * of the join. The second element is used to propagate a simple path join. As a
 * result, this join can only tell if the query matches a tree, but cannot
 * provide details about the nodes where it matches in the tree.
 * 
 * @author sumukh
 * 
 */
public class MPMGModSingleJoin extends AbstractPairJoin implements
		HalfPairJoin {
	
	public MPMGModSingleJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
	}

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			DocsAndPositionsEnum node, NodePositions... buffers)
			throws IOException {
		int freq = node.freq();
		NodePositions result = buffers[0]; // buffer used as result
		int numNextRead = 0;
		int pmark = 0;
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePositionAware.getNextPosition(result, node);
			numNextRead++;
			prev.offset = pmark;
			while (operatorAware.following(prev.positions, prev.offset, result.positions, result.offset)) {
				// skip before
				prev.offset += positionLength;
				pmark = prev.offset;
			}
			boolean found = false;
			while (prev.offset < prev.size) {
				if (op.match(prev, result, operatorAware)) { // next is child/desc
					found = true;
					break; // solution found; abort
				} else if (operatorAware.preceding(prev.positions, prev.offset, result.positions, result.offset)) {
					// prev is after
					break;
				}
				prev.offset += positionLength;
			}
			if (!found) {
				result.removeLast(positionLength);
			}
		}
	}

	@Override
	public int numBuffers(BinaryOperator op) {
		return 1;
	}
}
