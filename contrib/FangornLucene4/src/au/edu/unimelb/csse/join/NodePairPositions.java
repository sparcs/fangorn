package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;


public class NodePairPositions {
	private static final int DEFAULT_SIZE = 128;
	private static final int INCREMENT_SIZE = 128;

	public int[] node1;
	public int[] node2;
	int size;

	public NodePairPositions() {
		node1 = new int[DEFAULT_SIZE];
		node2 = new int[DEFAULT_SIZE];
		size = 0;
	}

	public void reset() {
		size = 0;
	}

	private void expand() {
		int[] newNode1 = new int[node1.length + INCREMENT_SIZE];
		System.arraycopy(node1, 0, newNode1, 0, node1.length);
		node1 = newNode1;

		int[] newNode2 = new int[node2.length + INCREMENT_SIZE];
		System.arraycopy(node2, 0, newNode2, 0, node2.length);
		node2 = newNode2;
	}

	public void add(NodePositions prev, NodePositions next, int length) {
		while (size + length >= node1.length) {
			expand();
		}
		for (int i = 0; i < length; i++) {
			node1[size] = prev.positions[prev.offset + i];
			node2[size] = next.positions[next.offset + i];
			size++;
		}
	}

	public void sortedAdd(NodePositions prev, NodePositions next,
			LogicalNodePositionAware npa) {
		int length = npa.getPositionLength();
		if (size == 0
				|| (size >= length
						&& npa.compare(node1, size - length, prev.positions,
								prev.offset) <= 0 && npa.compare(node2,
						size - length, next.positions, next.offset) <= 0)) {
			add(prev, next, length);
			return;
		}
		while (size + length >= node1.length) {
			expand();
		}
		int pos = getInsertPos(prev, next, npa);
		System.arraycopy(node1, pos * length, node1, pos * length + length, size - pos);
		System.arraycopy(node2, pos * length, node2, pos * length + length, size - pos);
		for (int i = 0; i < length; i++) {
			node1[pos * length + i] = prev.positions[prev.offset + i];
			node2[pos * length + i] = next.positions[next.offset + i];
		}
		size += length;
	}

	private int getInsertPos(NodePositions prev, NodePositions next,
			LogicalNodePositionAware npa) {
		int length = npa.getPositionLength();
		int start = 0;
		int end = size / length;
		while (start <= end) {
			int mid = (start + end) / 2;
			int prevIdx = npa.compare(node1, mid * length, prev.positions, prev.offset);
			if (prevIdx == 0) {
				int nextIdx = npa.compare(node2, mid * length, next.positions, next.offset);
				if (nextIdx == 0) {
					return mid;
				} else if (nextIdx < 0) {
					start = mid + 1;
				} else {
					end = mid - 1;
				}
			} else if (prevIdx < 0) {
				start = mid + 1;
			} else {
				end = mid - 1;
			}
		}
		return start;
	}

}
