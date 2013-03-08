package au.edu.unimelb.csse.join;

public class NodePairPositions {
	private static final int DEFAULT_SIZE = 128;
	private static final int INCREMENT_SIZE = 128;
	
	private int[] node1;
	private int[] node2;
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
}
