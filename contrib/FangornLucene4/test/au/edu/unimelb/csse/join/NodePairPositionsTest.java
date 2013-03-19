package au.edu.unimelb.csse.join;

import junit.framework.TestCase;

import org.junit.Test;

import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.LRDP;

public class NodePairPositionsTest extends TestCase {
	private LRDP lrdp = new LRDP(new BytePacking(4));

	public void testSortedAddIntoEmptyJustAdds() throws Exception {
		NodePairPositions npp = new NodePairPositions();

		int[] node11 = new int[] { 0, 4, 0, 0 };
		int[] node21 = new int[] { 1, 3, 1, 2 };

		npp.sortedAdd(getNodePositions(node11), getNodePositions(node21), lrdp);

		assertNodePairPositions(node11, node21, 0, 4, npp);
	}

	@Test
	public void testSortedInsertDoesNothingWhenAlreadySorted() throws Exception {
		int[] node11 = new int[] { 0, 4, 0, 0 };
		int[] node21 = new int[] { 1, 3, 1, 2 };
		NodePairPositions npp = getNodePairPositions(node11, node21);

		int[] node12 = new int[] { 0, 4, 0, 0 };
		int[] node22 = new int[] { 2, 3, 2, 4 };
		NodePositions prev = getNodePositions(node12);
		NodePositions next = getNodePositions(node22);

		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(node11, node21, 0, 4, npp);
		assertNodePairPositions(node12, node22, 1, 4, npp);
	}

	@Test
	public void testSortedInsertSwapsOnePositionBiggerNode1() throws Exception {
		int[] node11 = new int[] { 1, 4, 0, 0 };
		int[] node21 = new int[] { 1, 3, 1, 2 };
		NodePairPositions npp = getNodePairPositions(node11, node21);

		int[] node12 = new int[] { 0, 4, 0, 0 };
		int[] node22 = new int[] { 3, 4, 2, 4 };
		NodePositions prev = getNodePositions(node12);
		NodePositions next = getNodePositions(node22);

		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(node12, node22, 0, 4, npp);
		assertNodePairPositions(node11, node21, 1, 4, npp);
	}

	@Test
	public void testSortedInsertSwapsOnePositionBiggerNode2() throws Exception {
		int[] node11 = new int[] { 0, 4, 0, 0 };
		int[] node21 = new int[] { 2, 3, 1, 5 };
		NodePairPositions npp = getNodePairPositions(node11, node21);

		NodePositions prev = getNodePositions(new int[] { 0, 4, 0, 0 });
		NodePositions next = getNodePositions(new int[] { 1, 2, 1, 3 });

		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 1,
				3 }, 0, 4, npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 1,
				5 }, 1, 4, npp);
	}

	@Test
	public void testSortedInsertsMany() throws Exception {
		int[] node11 = new int[] { 0, 4, 0, 0 };
		int[] node21 = new int[] { 0, 1, 2, 1 };
		NodePairPositions npp = getNodePairPositions(node11, node21);

		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);

		NodePositions prev = getNodePositions(new int[] { 0, 4, 0, 0 });
		NodePositions next = getNodePositions(new int[] { 1, 2, 3, 2 });
		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength(), npp);

		prev = getNodePositions(new int[] { 1, 3, 1, 6 });
		next = getNodePositions(new int[] { 1, 2, 3, 2 });
		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 2, lrdp.getPositionLength(), npp);

		prev = getNodePositions(new int[] { 0, 4, 0, 0 });
		next = getNodePositions(new int[] { 2, 3, 3, 3 });
		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 3,
				3 }, 2, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 3, lrdp.getPositionLength(), npp);

		prev = getNodePositions(new int[] { 1, 3, 1, 6 });
		next = getNodePositions(new int[] { 2, 3, 3, 3 });
		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 3,
				3 }, 2, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 3, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 2, 3, 3,
				3 }, 4, lrdp.getPositionLength(), npp);

		prev = getNodePositions(new int[] { 0, 4, 0, 0 });
		next = getNodePositions(new int[] { 3, 4, 2, 5 });
		npp.sortedAdd(prev, next, lrdp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 3,
				3 }, 2, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 3, 4, 2,
				5 }, 3, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 4, lrdp.getPositionLength(), npp);
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 2, 3, 3,
				3 }, 5, lrdp.getPositionLength(), npp);
	}

	private NodePairPositions getNodePairPositions(int[] node1, int[] node2) {
		NodePairPositions npp = new NodePairPositions();
		assertTrue("Both arrays should be of the same length",
				node1.length == node2.length);
		System.arraycopy(node1, 0, npp.node1, 0, node1.length);
		System.arraycopy(node2, 0, npp.node2, 0, node2.length);
		npp.size = node1.length;
		return npp;
	}

	private NodePositions getNodePositions(int[] a) {
		NodePositions np = new NodePositions();
		System.arraycopy(a, 0, np.positions, 0, a.length);
		np.size = a.length;
		np.offset = 0;
		return np;
	}

	protected void assertNodePairPositions(int[] expectedPos1,
			int[] expectedPos2, int idx, int posLength, NodePairPositions npp) {
		int arrOff = idx * posLength;
		for (int i = 0; i < posLength; i++) {
			assertEquals("Incorrect value in result node1 at position "
					+ (arrOff + i), expectedPos1[i], npp.node1[arrOff + i]);
			assertEquals("Incorrect value in result node2 at position "
					+ (arrOff + i), expectedPos2[i], npp.node2[arrOff + i]);
		}
	}

}
