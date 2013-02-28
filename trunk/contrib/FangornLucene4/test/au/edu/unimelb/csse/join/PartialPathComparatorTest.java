package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.IndexTestCase;

public class PartialPathComparatorTest extends IndexTestCase {
	public void testContinuousSortStartsFromZero() throws Exception {
		// compares two 2-element paths
		PartialPathComparator comparator = new PartialPathComparator(4,
				new int[] { 0, 1 });
		int r = comparator.compare(new int[] { 1, 9, 3, 31, 2, 4, 5, 14 },
				new int[] { 1, 9, 3, 31, 4, 6, 4, 20 });
		assertTrue(r < 0);
		r = comparator.compare(new int[] { 1, 9, 3, 31, 2, 4, 5, 14 },
				new int[] { 1, 9, 3, 31, 4, 6, 4, 20, 6, 7, 4, 25 });
		assertTrue(r < 0);
	}

	public void testDiscontinousSort() throws Exception {
		PartialPathComparator comparator = new PartialPathComparator(4,
				new int[] { 0, 2, 3 });
		int r = comparator.compare(new int[] { 1, 2, 3, 4, 0, 0, 0, 0, 4, 5, 2, 12, 7,
				8, 9, 23 }, new int[] { 1, 2, 3, 4, 0, 0, 0, 0, 3, 2, 3, 10,
				12, 13, 5, 26 });
		assertTrue(r > 0);
		
		// now testing the same positions by adding an int value in a don't care position
		r = comparator.compare(new int[] { 1, 2, 3, 4, 0, 0, 0, 0, 4, 5, 2, 12, 7,
				8, 9, 23 }, new int[] { 1, 2, 3, 4, 9, 9, 9, 9, 3, 2, 3, 10,
				12, 13, 5, 26 });
		assertTrue(r > 0);
	}

}
