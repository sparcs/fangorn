package au.edu.unimelb.csse.paypack;

import junit.framework.TestCase;

public class LRDPTest extends TestCase {
	LRDP lrdp = new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111);

	public void testCompare() throws Exception {
		assertEquals(
				0,
				lrdp.compare(new int[] { 0, 4, 0, 0 }, 0, new int[] { 0, 4, 0,
						0 }, 0));

		assertEquals(
				-7,
				lrdp.compare(new int[] { 0, 8, 0, 0 }, 0, new int[] { 0, 1, 1,
						4 }, 0));

		assertEquals(
				-1,
				lrdp.compare(new int[] { 3, 4, 5, 6 }, 0, new int[] { 4, 5, 5,
						6 }, 0));
		
		assertEquals(
				1,
				lrdp.compare(new int[] { 3, 4, 5, 6 }, 0, new int[] { 3, 4, 4,
						7 }, 0));
	}
}
