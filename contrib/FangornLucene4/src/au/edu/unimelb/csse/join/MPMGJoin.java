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
public class MPMGJoin extends FullSolutionPairwiseJoin {
	
	public int[] join(int[] prev, Operator op, DocsAndPositionsEnum node)
			throws IOException {
		int[] result = new int[256];
		int resultSize = 0;
		int[] nextPays = getAllPositions(node);
		int nmark = 0;
		int poff = 0;
		int noff = 0;

		while (poff < prev.length) {
			if (noff == nextPays.length) {
				poff += 4;
				noff = nmark;
			} else if (op.match(prev, poff, nextPays, noff)) {
				// if next descendant/child
				result = addToResult(result, resultSize, prev, poff, nextPays,
						noff);
				resultSize++;
				noff += 4;
			} else if (Op.PRECEDING.match(prev, poff, nextPays, noff)
					|| Op.ANCESTOR.match(prev, poff, nextPays, noff)) {
				// comparison not counted
				noff += 4;
				nmark = noff;
			} else if (Op.DESCENDANT.equals(op)
					|| !Op.DESCENDANT.match(prev, poff, nextPays, noff)) {
				// desc: skip to next prev
				// child: skip if not descendant
				// comparison not counted
				poff += 4;
				noff = nmark;
			} else { // is descendant but op is child so just iterate
				noff += 4;
			}
		}
		return Arrays.copyOf(result, resultSize * 8);
	}

	@Override
	public boolean validOper(Operator op) {
		return op.equals(Op.CHILD) || op.equals(Op.DESCENDANT);
	}
}
