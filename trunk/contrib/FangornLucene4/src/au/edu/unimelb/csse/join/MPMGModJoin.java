package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.BinaryOperatorAware;
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
 * @author sumukh
 * 
 */
public class MPMGModJoin extends AbstractPairwiseJoin implements FullPairJoin {
	
	private LogicalNodePositionAware nodePostionAware;
	private int positionLength;
	private BinaryOperatorAware operatorAware;

	public MPMGModJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePostionAware = nodePositionAware;
		positionLength = nodePostionAware.getPositionLength();
		operatorAware = nodePositionAware.getBinaryOperatorHandler();
	}

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			DocsAndPositionsEnum node, NodePairPositions result,
			NodePositions... buffers) throws IOException {
		NodePositions buffer = buffers[0];
		int freq = node.freq();
		int numNextRead = 0;
		int pmark = 0;
		while (numNextRead < freq) {
			if (pmark == prev.size)
				break;
			nodePostionAware.getNextPosition(buffer, node);
			numNextRead++;
			prev.offset = pmark;
			while (operatorAware.following(prev, buffer)) {
				// skip before
				prev.offset += positionLength;
				pmark = prev.offset;
			}
			while (prev.offset < prev.size) {
				if (op.match(prev, buffer, operatorAware)) { // next is child/desc
					result.add(prev, buffer, positionLength);
				} else if (operatorAware.preceding(prev, buffer)) {
					// prev is after
					break;
				}
				prev.offset += positionLength;
			}
			buffer.reset();
		}
	}

	@Override
	public int numBuffers(BinaryOperator op) {
		return 1;
	}

}
