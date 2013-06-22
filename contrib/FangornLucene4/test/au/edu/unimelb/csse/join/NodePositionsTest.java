package au.edu.unimelb.csse.join;

import junit.framework.TestCase;

public class NodePositionsTest extends TestCase {

	public void testSizeAndOffsetAreZeroWhenReset() {
		NodePositions n = new NodePositions();
		for (int i = 0; i < 8; i++) {
			n.positions[i] = i;
			n.size++;
		}
		n.offset = 4;
		n.reset();
		assertEquals(0, n.size);
		assertEquals(0, n.offset);
	}
	
	public void testInsert() throws Exception {
		NodePositions position = new NodePositions(new int[] {1, 2, 3, 4, 5, 6, 7, 8});
		NodePositions insert = new NodePositions(new int[] {9, 10, 11, 12});
		position.insert(insert, 0, 4);
		assertPositions(new int[] {9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8}, 8, position);
	}
	
	protected void assertPositions(int[] expected, int expectedOffset,
			NodePositions prev) {
		assertEquals("Incorrect number of positions", expected.length,
				prev.size);
		assertEquals("Incorrect offset", expectedOffset, prev.offset);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Incorrect value at index " + i, expected[i],
					prev.positions[i]);
		}
	}

	
	//TODO: Write more tests
}
