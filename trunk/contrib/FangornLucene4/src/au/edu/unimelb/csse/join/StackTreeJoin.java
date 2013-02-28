package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;

/**
 * This path join algorithm is an adaptation of the StackTree join described by
 * Al-Khalifa et al. (2002)
 * 
 * Al-Khalifa, S.; Jagadish, H.; Koudas, N.; Patel, J. M.; Srivastava, D. & Wu,
 * Y. Structural Joins: A Primitive for Efficient XML Query Pattern Matching
 * ICDE '02: Proceedings of the 18th International Conference on Data
 * Engineering, IEEE Computer Society, 2002, 141
 * 
 * We use the LPath numbering scheme described in Bird et.al. (2005)
 * 
 * Bird, Steven G., Yi Chen, Susan B. Davidson, Haejoong Lee, and Yifeng Zheng.
 * 2005. Extending xpath to support linguistic queries. In Proceedings of
 * Programming Language Technologies for XML (PLANX), 35--46, Long Beach, USA.
 * 
 * @author sumukh
 * 
 */
public class StackTreeJoin extends FullSolutionPairwiseJoin {

	@Override
	public boolean validOper(Operator op) {
		return Op.CHILD.equals(op) || Op.DESCENDANT.equals(op);
	}

	@Override
	public int[] join(int[] prev, Operator op, DocsAndPositionsEnum node)
			throws IOException {
		int[] result = new int[256];
		int resultSize = 0;
		int[] nextPays = getAllPositions(node);

		int noff = 0;
		int poff = 0;
		int[] stack = new int[128];
		int stackSize = 0;

		while (noff < nextPays.length && poff < prev.length) {
			if (Op.PRECEDING.match(nextPays, noff, prev, poff)
					|| Op.ANCESTOR.match(nextPays, noff, prev, poff)) {
				// prev before nextPays
				while (stackSize > 0) {
					if (Op.DESCENDANT.match(stack, stackSize * 4 - 4, prev,
							poff)) { // desc implies not following
						stack = pushToStack(prev, poff, stack, stackSize);
						stackSize++;
						break;
					}
					stackSize--;
				}
				if (stackSize == 0) {
					stack = pushToStack(prev, poff, stack, stackSize);
					stackSize++;
				}
				poff += 4;
			} else { // nextPays before prev
				while (stackSize > 0
						&& !Op.DESCENDANT.match(stack, stackSize * 4 - 4,
								nextPays, noff)) {
					stackSize--;
				}
				if (stackSize > 0
						&& op.match(stack, stackSize * 4 - 4, nextPays, noff)) {
					if (op.equals(Op.CHILD)) {
						result = addToResult(result, resultSize, stack,
								stackSize * 4 - 4, nextPays, noff);
						resultSize++;
					} else {
						for (int i = 0; i < stackSize; i++) {
							result = addToResult(result, resultSize, stack,
									i * 4, nextPays, noff);
							resultSize++;
						}
					}
				}
				// if stackSize == 0 then; do nothing
				noff += 4;
			}
		}
		while (stackSize > 0 && noff < nextPays.length) {
			while (stackSize > 0
					&& !Op.DESCENDANT.match(stack, stackSize * 4 - 4, nextPays,
							noff)) {
				stackSize--;
			}
			if (stackSize > 0
					&& op.match(stack, stackSize * 4 - 4, nextPays, noff)) {
				if (op.equals(Op.CHILD)) {
					result = addToResult(result, resultSize, stack,
							stackSize * 4 - 4, nextPays, noff);
					resultSize++;
				} else {
					for (int i = 0; i < stackSize; i++) {
						result = addToResult(result, resultSize, stack, i * 4,
								nextPays, noff);
						resultSize++;
					}
				}
			}
			noff += 4;
		}
		return Arrays.copyOf(result, resultSize * 8);
	}

	private int[] pushToStack(int[] prev, int poff, int[] stack, int stackSize) {
		if (!(stackSize * 4 < stack.length)) {
			int[] newstack = new int[stack.length + 128];
			System.arraycopy(stack, 0, newstack, 0, stack.length);
			stack = newstack;
		}
		System.arraycopy(prev, poff, stack, stackSize * 4, 4);
		return stack;
	}
}
