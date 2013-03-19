package au.edu.unimelb.csse.join;

import java.util.Comparator;

import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class NPAPathPositionComparator implements Comparator<int[]> {
	private int nodeIdx;
	private LogicalNodePositionAware nodePositionAware;
	private int length;

	public NPAPathPositionComparator(
			LogicalNodePositionAware nodePositionAware, int nodeIdx) {
		this.nodeIdx = nodeIdx;
		this.nodePositionAware = nodePositionAware;
		this.length = nodePositionAware.getPositionLength();
	}

	@Override
	public int compare(int[] o1, int[] o2) {
		if (o1 == null && o2 != null)
			return 1;
		else if (o2 == null && o1 != null)
			return -1;
		else if (o1 == null && o2 == null)
			return 0;
		return nodePositionAware.compare(o1, nodeIdx * length, o2, nodeIdx
				* length);
	}

}
