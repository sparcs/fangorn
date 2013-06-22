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
 * This implementation deviates slightly from the original algorithm for node
 * position labels. Here the trees nodes are labelled using LPath's labelling
 * scheme by Bird et.al. (2005)
 * 
 * Bird, Steven G., Yi Chen, Susan B. Davidson, Haejoong Lee, and Yifeng Zheng.
 * 2005. Extending xpath to support linguistic queries. In Proceedings of
 * Programming Language Technologies for XML (PLANX), 35--46, Long Beach, USA.
 * 
 * @author sumukh
 * 
 */
public class MPMGJoin implements FullPairJoin {
	private final LogicalNodePositionAware nodePositionAware;
	private final int positionLength;
	private final OperatorAware operatorAware;
	NodePositions buffer = new NodePositions();

	public MPMGJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
	}

	@Override
	public void match(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, NodePairPositions result) throws IOException {
		buffer.reset();
		result.reset();
		prev.offset = 0;
		nodePositionAware.getAllPositions(buffer, node);
		int nmark = 0;

		while (prev.offset + positionLength <= prev.size) {
			if (buffer.offset == buffer.size) {
				prev.offset += positionLength;
				buffer.offset = nmark;
			} else if (op.match(prev, buffer, operatorAware)) {
				// if next descendant/child
				result.add(prev, buffer, positionLength);
				buffer.offset += positionLength;
			} else if (operatorAware.startsBefore(prev.positions, prev.offset, buffer.positions, buffer.offset)) {
				buffer.offset += positionLength;
				nmark = buffer.offset;
			} else if (Operator.DESCENDANT.equals(op)
					|| !operatorAware.descendant(prev.positions, prev.offset, buffer.positions, buffer.offset)) {
				// desc: skip to next prev
				// child: skip if not descendant
				prev.offset += positionLength;
				buffer.offset = nmark;
			} else { // is descendant but op is child so just iterate
				buffer.offset += positionLength;
			}
		}
	}
}
