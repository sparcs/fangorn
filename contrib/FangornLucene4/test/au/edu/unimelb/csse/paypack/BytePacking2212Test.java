package au.edu.unimelb.csse.paypack;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.join.NodePositions;

public class BytePacking2212Test extends TestCase {
	public void testEncode() throws Exception {
		BytePacking2212 packing = new BytePacking2212();
		BytesRef[] encoded = packing.encode(new int[] { 0, 10, 2, 34 }, 1);
		assertEquals(1, encoded.length);

		assertTrue(
				"Expected byte array to be equal",
				Arrays.equals(new byte[] { 0x00, 0x00, 0x00, 0x0A, 0x02, 0x00,
						0x22 }, encoded[0].bytes));

		encoded = packing.encode(new int[] { 1027, 32769, 200, 25770 }, 1);
		assertEquals(1, encoded.length);

		assertTrue(
				"Expected byte array to be equal",
				Arrays.equals(new byte[] { 0x04, 0x03, (byte) 0x80, 0x01,
						(byte) 0xC8, 0x64, (byte) 0xAA }, encoded[0].bytes));

		encoded = packing.encode(new int[] { 0, 10, 2, 34, 1027, 32769, 200,
				25770 }, 2);
		assertEquals(2, encoded.length);

		byte[] returned = new byte[encoded[0].length];
		System.arraycopy(encoded[0].bytes, encoded[0].offset, returned, 0,
				encoded[0].length);
		assertTrue(
				"Expected byte array to be equal",
				Arrays.equals(new byte[] { 0x00, 0x00, 0x00, 0x0A, 0x02, 0x00,
						0x22 }, returned));

		returned = new byte[encoded[1].length];
		System.arraycopy(encoded[1].bytes, encoded[1].offset, returned, 0,
				encoded[1].length);
		assertTrue(
				"Expected byte array to be equal",
				Arrays.equals(new byte[] { 0x04, 0x03, (byte) 0x80, 0x01,
						(byte) 0xC8, 0x64, (byte) 0xAA }, returned));
	}

	public void testEncodingThrowsErrorWhenPositionLengthIsExceeded()
			throws Exception {
		BytePacking2212 packing = new BytePacking2212();
		try {
			packing.encode(new int[] { 0, 65281, 2, 34 }, 1);
			fail("should throw error when right is greater than 65280");
		} catch (PayloadFormatException pfe) {

		}
		
		try {
			packing.encode(new int[] { 65281, 65282, 2, 34 }, 1);
			fail("should throw error when left or right is greater than 65280");
		} catch (PayloadFormatException pfe) {

		}
		
		try {
			packing.encode(new int[] { 0, 400, 256, 34 }, 1);
			fail("should throw error when depth is greater than 255");
		} catch (PayloadFormatException pfe) {

		}
		
		try {
			packing.encode(new int[] { 3000, 6000, 200, 65281 }, 1);
			fail("should throw error when parent is greater than 65280");
		} catch (PayloadFormatException pfe) {

		}
	}
	
	public void testDecode() throws Exception {
		BytesRef payload = new BytesRef(new byte[] { 0x00, 0x00, 0x00, 0x0A, 0x02, 0x00,
						0x22 });
		BytePacking2212 packing = new BytePacking2212();
		NodePositions nodePos = new NodePositions();
		packing.decode(payload, nodePos);
		assertEquals(0, nodePos.offset);
		assertEquals(4, nodePos.size);
		int[] returned = new int[4];
		System.arraycopy(nodePos.positions, 0, returned, 0, 4);
		assertTrue(Arrays.equals(new int[] {0, 10, 2, 34}, returned));
		
		payload = packing.encode(new int[] { 0, 10, 2, 34, 1027, 32769, 200,
				25770 }, 2)[1];
		nodePos = new NodePositions();
		packing.decode(payload, nodePos);
		assertEquals(0, nodePos.offset);
		assertEquals(4, nodePos.size);
		returned = new int[4];
		System.arraycopy(nodePos.positions, 0, returned, 0, 4);
		assertTrue(Arrays.equals(new int[] {1027, 32769, 200,
				25770}, returned));
	}
}
