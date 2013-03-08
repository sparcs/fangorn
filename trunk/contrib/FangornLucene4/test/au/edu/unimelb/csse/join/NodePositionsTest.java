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
	
	//TODO: Write more tests
}
