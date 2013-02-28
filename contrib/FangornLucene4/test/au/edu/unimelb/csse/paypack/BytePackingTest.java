package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.IndexTestCase;

public class BytePackingTest extends IndexTestCase {
	private BytePacking bp = new BytePacking();
	
	public void testDecode() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x01, (byte) 0xFE, 0x05, (byte) 0xFF});
		int[] payloads = new int[4];
		payloads = bp.decode(bref, payloads, 0);
		assertIntArray(new int[]{1, 254, 5, 255}, payloads);
	}
	
	public void testDecodeWithOffset() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x00, 0x00, 0x01, 0x00, 0x01, (byte) 0xFE, 0x05, (byte) 0xFF}, 4, 4);
		int[] payloads = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		payloads = bp.decode(bref, payloads, 8);
		assertIntArray(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 254, 5, 255}, payloads);
	}
	
	public void testDecodeWithNewReturnBuffer() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x00, 0x00, 0x01, 0x00, 0x01, (byte) 0xFE, 0x05, (byte) 0xFF}, 4, 4);
		int[] payloads = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		payloads = bp.decode(bref, payloads, 8);
		int[] expectedReturned = new int[8 + 128];
		for (int i = 16; i < 8 + 128; i++) {
			expectedReturned[i] = 0;
		}
		expectedReturned[8] = 1;
		expectedReturned[9] = 254;
		expectedReturned[10] = 5;
		expectedReturned[11] = 255;
		assertIntArray(expectedReturned, payloads);
	}
	
	public void testEncode() throws Exception {
		BytesRef[] brs = bp.encode(new int[]{1, 254, 5, 255}, 1);
		assertEquals(1, brs.length);
		BytesRef br = brs[0];
		assertEquals(1, br.bytes[br.offset]);
		assertEquals(254, br.bytes[br.offset + 1] & 255);
		assertEquals(5, br.bytes[br.offset + 2]);
		assertEquals(255, br.bytes[br.offset + 3] & 255);
	}
	
	public void testEncodeMany() throws Exception {
		BytesRef[] brs = bp.encode(new int[]{1, 254, 5, 255, 2, 220, 6, 240}, 2);
		assertEquals(2, brs.length);
		BytesRef br = brs[0];
		assertEquals(1, br.bytes[br.offset]);
		assertEquals(254, br.bytes[br.offset + 1] & 255);
		assertEquals(5, br.bytes[br.offset + 2]);
		assertEquals(255, br.bytes[br.offset + 3] & 255);
		br = brs[1];
		assertEquals(2, br.bytes[br.offset]);
		assertEquals(220, br.bytes[br.offset + 1] & 255);
		assertEquals(6, br.bytes[br.offset + 2]);
		assertEquals(240, br.bytes[br.offset + 3] & 255);
		
	}
}
