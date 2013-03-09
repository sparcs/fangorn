package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.paypack.LRDP;

/**
 * This is a holistic join algorithm proposed by Bruno et al. (2002):
 * 
 * Bruno, N.; Koudas, N. & Srivastava, D. Holistic twig joins: optimal XML
 * pattern matching SIGMOD '02: Proceedings of the 2002 ACM SIGMOD international
 * conference on Management of data, ACM, 2002, 310-321
 * 
 * @author sumukh
 * 
 */
public class PathStackJoin extends AbstractHolisticJoin {

	public PathStackJoin(String[] labels, BinaryOperator[] operators,
			LRDP nodePositionAware) {
		super(labels, operators, nodePositionAware);
	}

	public List<int[]> match() throws IOException {
		List<int[]> results = null;
		while (!shouldStop()) {
			int pos = getMinSource();
			clearPrecedingStackEntries(positions, pos * positionLength);
			boolean updated = updateStackIfNeeded(pos);
			if (updated && postingsFreqs[pos].isLeaf) {
				if (results == null) {
					results = new ArrayList<int[]>();
				}
				getPathSolutions(results, postingsFreqs[pos]);
				positionStacksSizes[postingsFreqs[pos].position]--;
			}
			getNextPosition(pos);
		}
		return results;
	}

	int getMinSource() {
		int minPos = 0;
		int min = preorderPos[0];
		int i = 1;
		while (nextPosCalledCount[minPos] > freqs[minPos]
				&& i < preorderPos.length) {
			minPos = i;
			min = preorderPos[i];
			i++;
		}
		for (; i < preorderPos.length; i++) {
			if (nextPosCalledCount[i] > freqs[i])
				continue;
			if (preorderPos[i] < min) {
				min = preorderPos[i];
				minPos = i;
			} else if (preorderPos[i] == min
					&& postingsFreqs[i].position > postingsFreqs[minPos].position) {
				minPos = i;
			}
		}
		return minPos;
	}

	void clearPrecedingStackEntries(int[] positions, int offset) {
		for (int i = 0; i < positionStacks.length; i++) {
			int[] stack = positionStacks[i];
			int stackSize = positionStacksSizes[i];
			while (stackSize > 0
					&& operatorAware.preceding(positions, offset, stack,
							(stackSize - 1) * stackLength)) {
				stackSize--;
			}
			positionStacksSizes[i] = stackSize;
		}
	}

}
