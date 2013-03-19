package au.edu.unimelb.csse.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.LRDP;

public class NPAPathPositionComparatorTest extends IndexTestCase {
	LRDP lrdp = new LRDP(new BytePacking(4));

	public void testSortingEqualPositionsLeavesItUnchanged() throws Exception {
		NPAPathPositionComparator comparator = new NPAPathPositionComparator(
				lrdp, 2);

		int[] res0 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4 };
		int[] res1 = new int[] { 1, 2, 1, 2, 0, 0, 0, 0, 1, 2, 3, 4 };
		List<int[]> result = setupResult(res0, res1);

		Collections.sort(result, comparator);

		assertIntArray(res0, result.get(0));
		assertIntArray(res1, result.get(1));
	}

	public void testSortsByTreePosition() throws Exception {
		NPAPathPositionComparator comparator = new NPAPathPositionComparator(
				lrdp, 2);

		int[] res0 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 2, 4, 5, 10 };
		int[] res1 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 1, 8, 3, 4 };
		List<int[]> result = setupResult(res0, res1);

		Collections.sort(result, comparator);

		assertIntArray(res1, result.get(0));
		assertIntArray(res0, result.get(1));
	}

	public void testSortsMultiple() throws Exception {
		NPAPathPositionComparator comparator = new NPAPathPositionComparator(
				lrdp, 0);

		int[] res0 = new int[] { 0, 4, 0, 0, 0, 1, 2, 1 };
		int[] res1 = new int[] { 0, 4, 0, 0, 1, 2, 3, 2 };
		int[] res2 = new int[] { 1, 3, 1, 6, 1, 2, 3, 2 };
		int[] res3 = new int[] { 0, 4, 0, 0, 2, 3, 3, 3 };
		int[] res4 = new int[] { 1, 3, 1, 6, 2, 3, 3, 3 };
		int[] res5 = new int[] { 0, 4, 0, 0, 3, 4, 2, 5 };
		List<int[]> results = setupResult(res0, res1, res2, res3, res4, res5);
		
		Collections.sort(results, comparator);
		
		assertIntArray(res0, results.get(0));
		assertIntArray(res1, results.get(1));
		assertIntArray(res3, results.get(2));
		assertIntArray(res5, results.get(3));
		assertIntArray(res2, results.get(4));
		assertIntArray(res4, results.get(5));
		
	}

	private List<int[]> setupResult(int[]... inputs) {
		List<int[]> result = new ArrayList<int[]>();
		for (int[] r : inputs)
			result.add(r);
		return result;
	}
}
