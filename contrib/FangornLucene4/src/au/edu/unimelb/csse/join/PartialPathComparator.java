package au.edu.unimelb.csse.join;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Compares two tree paths
 * 
 * Each path consists of node indexes arranged in the same order as their
 * appearance in the query
 * 
 * The length of the index depends on the node labelling scheme in use
 * 
 * @author sumukh
 * 
 */
class PartialPathComparator implements Comparator<int[]> {
	private int[] comparePos;

	/**
	 * 
	 * @param nodeIdxLen
	 *            is the length of the index assigned to each node; depends on
	 *            the node labelling scheme
	 * @param nodePos
	 *            is a list of node positions from the query
	 */
	public PartialPathComparator(int nodeIdxLen, int... nodePos) {
		Arrays.sort(nodePos);
		comparePos = new int[nodePos.length * nodeIdxLen];
		for (int i = 0; i < nodePos.length; i++) {
			for (int j = 0; j < nodeIdxLen; j++) {
				comparePos[i * nodeIdxLen + j] = nodePos[i] * nodeIdxLen + j;
			}
		}
	}

	@Override
	public int compare(int[] o1, int[] o2) {
		if (o1 == null && o2 != null)
			return 1;
		else if (o2 == null && o1 != null)
			return -1;
		else if (o1 == null && o2 == null)
			return 0;
		for (int pos : comparePos) {
			if (o1[pos] != o2[pos])
				return o1[pos] - o2[pos];
		}
		if (o1.length == o2.length) {
			return 0;
		} else if (o1.length < o2.length) {
			return -1;
		}
		return 1;
	}

}
