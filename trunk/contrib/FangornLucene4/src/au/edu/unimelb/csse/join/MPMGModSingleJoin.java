package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
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
public abstract class MPMGModSingleJoin implements HalfPairJoin {
	public static final JoinBuilder JOIN_BUILDER = new MPMGModSingleJoinBuilder();
	protected final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final OperatorAware operatorAware;
	NodePositions result = new NodePositions();
	Operator op;

	public MPMGModSingleJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
		this.op = op;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException {
		int freq = node.freq();
		result.reset();
		int numNextRead = 0;
		int pmark = 0;
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePositionAware.getNextPosition(result, node);
			numNextRead++;
			prev.offset = pmark;
			while (prev.offset < prev.size && skipCondition(prev)) {
				// skip before
				prev.offset += positionLength;
				pmark = prev.offset;
			}
			if (!join(prev, result)) {
				result.removeLast(positionLength);
			}
		}
		return result;
	}

	@Override
	public NodePositions match(NodePositions prev, Operator op,
			NodePositions next) throws IOException {
		result.reset();
		next.offset = 0;
		int pmark = 0;
		while (next.offset < next.size) {
			if (pmark == prev.size)
				break;
			prev.offset = pmark;
			while (prev.offset < prev.size && skipCondition(prev)) {
				// skip before
				prev.offset += positionLength;
				pmark = prev.offset;
			}
			if (join(prev, next)) {
				result.push(next, positionLength);
			}
			next.offset += positionLength;
		}
		return result;
	}

	abstract boolean skipCondition(NodePositions prev);

	abstract boolean join(NodePositions prev, NodePositions next);
}

class DescChildMPMGModSingle extends MPMGModSingleJoin {

	public DescChildMPMGModSingle(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	boolean skipCondition(NodePositions prev) {
		return operatorAware.following(prev.positions, prev.offset,
				result.positions, result.offset);
	}

	boolean join(NodePositions prev, NodePositions next) {
		while (prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				return true;
			} else if (operatorAware.preceding(prev.positions, prev.offset,
					next.positions, next.offset)) {
				// prev is after
				break;
			}
			prev.offset += positionLength;
		}
		return false;
	}
}

class AncParMPMGModSingle extends MPMGModSingleJoin {

	public AncParMPMGModSingle(Operator op, LogicalNodePositionAware nodePositionAware) {
		super(op, nodePositionAware);
	}

	boolean skipCondition(NodePositions prev) {
		return operatorAware.startsAfter(prev.positions, prev.offset,
				result.positions, result.offset);
	}

	boolean join(NodePositions prev, NodePositions next) {
		while (prev.offset < prev.size) {
			if (op.match(prev, next, operatorAware)) {
				return true;
			} else if (operatorAware.following(next.positions, next.offset,
					prev.positions, prev.offset)) {
				break;
			}
			prev.offset += positionLength;
		}
		return false;
	}

}

class MPMGModSingleJoinBuilder implements JoinBuilder {

	@Override
	public HalfPairJoin getHalfPairJoin(Operator op,
			LogicalNodePositionAware nodePositionAware) {
		if (Operator.ANCESTOR.equals(op) || Operator.PARENT.equals(op)) {
			return new AncParMPMGModSingle(op, nodePositionAware);
		} else if (Operator.DESCENDANT.equals(op) || Operator.CHILD.equals(op)) {
			return new DescChildMPMGModSingle(op, nodePositionAware);
		}
		return null;
	}
}
