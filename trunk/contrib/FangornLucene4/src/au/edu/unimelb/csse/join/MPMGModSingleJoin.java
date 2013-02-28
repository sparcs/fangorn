package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;

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
 * of the join. The second element is used to propagate a simple path join
 * 
 * @author sumukh
 * 
 */
public class MPMGModSingleJoin extends AbstractPairwiseJoin {

	public int[] join(int[] prev, Operator op, DocsAndPositionsEnum node)
			throws IOException {
		int[] result = new int[256];
		int[] curBuffer = new int[4];
		int resultSize = 0;

		int freq = node.freq();
		int posIdx = 0;
		int poff = 0;
		int pmark = 0;
		while (posIdx < freq) {
			if (pmark == prev.length)
				break;
			node.nextPosition();
			posIdx++;
			curBuffer = payloadFormat.decode(node.getPayload(), curBuffer, 0);
			poff = pmark;
			while (Op.FOLLOWING.match(prev, poff, curBuffer, 0)) {
				// skip before
				poff += 4;
				pmark = poff;
			}
			while (poff < prev.length) {
				if (op.match(prev, poff, curBuffer, 0)) { // next is child/desc
					result = addNextToResult(result, resultSize, curBuffer, 0);
					resultSize++;
					break; // abort as soon as curBuffer is a solution
				} else if (Op.PRECEDING.match(prev, poff, curBuffer, 0)) {
					// prev is after
					break;
				}
				poff += 4;
			}
		}
		return Arrays.copyOf(result, resultSize * 4);
	}

	@Override
	public boolean validOper(Operator op) {
		return op.equals(Op.CHILD) || op.equals(Op.DESCENDANT);
	}
}
