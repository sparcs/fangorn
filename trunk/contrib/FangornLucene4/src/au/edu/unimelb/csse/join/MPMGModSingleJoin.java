package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

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
public class MPMGModSingleJoin extends AbstractPairJoin implements HalfPairJoin {

	public MPMGModSingleJoin(LogicalNodePositionAware nodePositionAware) {
		super(nodePositionAware);
	}

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			DocsAndPositionsEnum node, NodePositions... buffers)
			throws IOException {
		int freq = node.freq();
		NodePositions result = buffers[0]; // buffer used as result
		result.reset();
		int numNextRead = 0;
		int pmark = 0;
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePositionAware.getNextPosition(result, node);
			numNextRead++;
			prev.offset = pmark;
			boolean found = false;
			if (BinaryOperator.DESCENDANT.equals(op)
					|| BinaryOperator.CHILD.equals(op)) {
				while (prev.offset < prev.size && operatorAware.following(prev.positions, prev.offset,
						result.positions, result.offset)) {
					// skip before
					prev.offset += positionLength;
					pmark = prev.offset;
				}
				found = checkDescChild(prev, op, result, found);
			} else { // ancestor or parent
				while (prev.offset < prev.size && operatorAware.startsAfter(prev.positions, prev.offset,
						result.positions, result.offset)) {
					// skip before
					prev.offset += positionLength;
					pmark = prev.offset;
				}
				found = checkAncParent(prev, op, result, found);
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

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			NodePositions next, NodePositions... buffers) throws IOException {
		NodePositions result = buffers[0]; // buffer used as result
		result.reset();
		next.offset = 0;
		int pmark = 0;
		while (next.offset < next.size) {
			if (pmark == prev.size)
				break;	
			prev.offset = pmark;
			boolean found = false;
			if (BinaryOperator.DESCENDANT.equals(op)
					|| BinaryOperator.CHILD.equals(op)) {
				while (prev.offset < prev.size && operatorAware.following(prev.positions, prev.offset,
						next.positions, next.offset)) {
					// skip before
					prev.offset += positionLength;
					pmark = prev.offset;
				}
				found = checkDescChild(prev, op, next, found);
			} else { // ancestor or parent
				while (prev.offset < prev.size && operatorAware.startsAfter(prev.positions, prev.offset,
						next.positions, next.offset)) {
					// skip before
					prev.offset += positionLength;
					pmark = prev.offset;
				}
				found = checkAncParent(prev, op, next, found);
			}
			if (found) {
				result.push(next, positionLength);
			}
			next.offset += positionLength;
		}
	}

	private boolean checkAncParent(NodePositions prev, BinaryOperator op,
			NodePositions next, boolean found) {
		while (prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				found = true;
				break;
			} else if (operatorAware.following(next.positions, next.offset, prev.positions, prev.offset)) {
				break;
			}
			prev.offset += positionLength;
		}
		return found;
	}

	private boolean checkDescChild(NodePositions prev, BinaryOperator op,
			NodePositions next, boolean found) {
		while (prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				found = true;
				break; // solution found; abort
			} else if (operatorAware.preceding(prev.positions,
					prev.offset, next.positions, next.offset)) {
				// prev is after
				break;
			}
			prev.offset += positionLength;
		}
		return found;
	}
}
