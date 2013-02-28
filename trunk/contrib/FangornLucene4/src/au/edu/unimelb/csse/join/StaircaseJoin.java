package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;

/**
 * This is an adaptation of the Staircase join by Grust et al. (2003)
 * 
 * Grust, T.; Keulen, M. & Teubner, J. Staircase Join: Teach a Relational DBMS
 * to Watch its (Axis) Steps In Proceedings of the 29th International Conference
 * on Very Large Databases (VLDB), 2003, 524-535
 * 
 * Here we use Lucene's index instead of a B+-tree inverted list in an RDBMS
 * 
 * @author sumukh
 * 
 */
public class StaircaseJoin extends AbstractPairwiseJoin {

	@Override
	public boolean validOper(Operator op) {
		return true;
	}

	@Override
	public int[] join(int[] prev, Operator op, DocsAndPositionsEnum node)
			throws IOException {
		int[] result = new int[DEFAULT_BUF_SIZE];
		int resultSize = 0;
		int[] nextPays = getAllPositions(node);

		prev = prune(prev, op);

		if (Op.FOLLOWING.equals(op) || Op.PRECEDING.equals(op)) {
			for (int i = 0; i < nextPays.length; i += 4) {
				if (op.match(prev, 0, nextPays, i)) {
					result = addNextToResult(result, resultSize, nextPays, i);
					resultSize++;
				}
			}
		} else if (Op.DESCENDANT.equals(op)) {
			int poff = 0;
			int noff = 0;
			while (noff < nextPays.length && poff < prev.length) {
				if (op.match(prev, poff, nextPays, noff)) {
					result = addNextToResult(result, resultSize, nextPays, noff);
					resultSize++;
					noff += 4;
				} else if (Op.FOLLOWING.match(prev, poff, nextPays, noff)) {
					poff += 4;
				} else {
					noff += 4;
				}
			}
		} else if (Op.ANCESTOR.equals(op)) {
			int poff = 0;
			int noff = 0;
			while (noff < nextPays.length && poff < prev.length) {
				if (op.match(prev, poff, nextPays, noff)) {
					result = addNextToResult(result, resultSize, nextPays, noff);
					resultSize++;
					noff += 4;
				} else if (Op.DESCENDANT.match(prev, poff, nextPays, noff)
						|| Op.FOLLOWING.match(prev, poff, nextPays, noff)) {
					poff += 4;
				} else {
					noff += 4;
				}
			}
		} else if (Op.CHILD.equals(op)) {
			// similar to MPMG join but the marker is on poff here
			int poff = 0;
			int pmark = 0;
			int noff = 0;
			while (noff < nextPays.length && poff < prev.length) {
				if (op.match(prev, poff, nextPays, noff)) {
					result = addNextToResult(result, resultSize, nextPays, noff);
					resultSize++;
					noff += 4;
					poff = pmark;
				} else if (Op.FOLLOWING.match(prev, poff, nextPays, noff)) {
					poff += 4;
					pmark = poff;
				} else if (Op.DESCENDANT.match(prev, poff, nextPays, noff)) {
					poff += 4;
					if (poff == prev.length) {
						noff += 4;
						poff = pmark;
					}
				} else { // is preceding or ancestor
					noff += 4;
					poff = pmark;
				}
			}
		} else if (Op.PARENT.equals(op)) {
			// skip the first few precedings
			int poff = 0;
			int pmark = 0;
			int noff = 0;
			while (noff < nextPays.length && poff < prev.length) {
				if (op.match(prev, poff, nextPays, noff)) {
					result = addNextToResult(result, resultSize, nextPays, noff);
					resultSize++;
					noff += 4;
					poff = pmark;
				} else if (pmark == poff && Op.FOLLOWING.match(prev, poff, nextPays, noff)) {
					poff += 4;
					pmark = poff;
				} else if (Op.ANCESTOR.match(prev, poff, nextPays, noff) || Op.PRECEDING.match(prev, poff, nextPays, noff)) {
					noff += 4;
					poff = pmark;
				} else {
					poff += 4;
				}
				if (poff >= prev.length && noff < nextPays.length) {
					poff = pmark;
					noff += 4;
				}
			}
		}
		return Arrays.copyOf(result, resultSize * 4);
	}

	int[] prune(int[] prev, Operator op) {
		if (prev.length <= 4)
			return prev;
		int[] toRemovePos = new int[prev.length - 1];
		int toRemLen = 0;
		if (Op.DESCENDANT.equals(op)) {
			int[] before = new int[] { prev[0], prev[1], prev[2], prev[3] };
			for (int i = 0; i < (prev.length - 4) / 4; i++) {
				if (Op.DESCENDANT.match(before, 0, prev, i * 4 + 4)) {
					toRemovePos[toRemLen++] = i * 4 + 4;
				} else { // has to be following
					for (int j = 0; j < 4; j++) {
						before[j] = prev[i * 4 + 4 + j];
					}
				}
			}
		} else if (Op.ANCESTOR.equals(op)) {
			boolean[] toRemoveFlags = new boolean[prev.length / 4];
			// here each stack element has: treepos (4int) + prevIdx (1int)
			int[] stack = new int[prev.length + prev.length / 4];
			int stackSize = 0;
			for (int i = 0; i < prev.length / 4; i++) {
				while (stackSize > 0) {
					if (Op.DESCENDANT.match(stack, stackSize * 5 - 5, prev,
							i * 4)) { // desc implies not following
						int prevIdx = stack[stackSize * 5 - 1]; // -5 + 4 = -1
						toRemoveFlags[prevIdx] = true;
						pushToStack(prev, i, stack, stackSize);
						stackSize++;
						break;
					}
					stackSize--;
				}
				if (stackSize == 0) {
					pushToStack(prev, i, stack, stackSize);
					stackSize++;
				}
			}
			for (int i = 0; i < toRemoveFlags.length; i++) {
				if (toRemoveFlags[i]) {
					toRemovePos[toRemLen++] = i * 4;
				}
			}
		} else if (Op.FOLLOWING.equals(op)) {
			int[] result = new int[] { prev[0], prev[1], prev[2], prev[3] };
			int i = 1;
			// not descendant => following
			while (i < prev.length / 4
					&& Op.DESCENDANT.match(result, 0, prev, i * 4)) {
				for (int j = 0; j < 4; j++) {
					result[j] = prev[i * 4 + j];
				}
				i++;
			}
			return result;
		} else if (Op.PRECEDING.equals(op)) {
			int last = prev.length - 4;
			return new int[] { prev[last], prev[last + 1], prev[last + 2],
					prev[last + 3] };
		}
		if (toRemLen > 0) {
			for (int i = toRemLen - 1; i >= 0; i--) {
				int removePos = toRemovePos[i];
				if (removePos != prev.length - 4) {
					System.arraycopy(prev, removePos + 4, prev, removePos,
							prev.length - removePos - 4);
				}
			}
			return Arrays.copyOf(prev, prev.length - toRemLen * 4);
		}
		return prev;
	}

	void pushToStack(int[] prev, int i, int[] stack, int stackSize) {
		System.arraycopy(prev, i * 4, stack, stackSize * 5, 4);
		stack[stackSize * 5 + 4] = i;
	}

}
