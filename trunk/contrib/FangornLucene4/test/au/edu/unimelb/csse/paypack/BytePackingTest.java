package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.join.NodePositions;

public class BytePackingTest extends IndexTestCase {
	private BytePacking bp = new BytePacking(4);
	
	public void testDecode() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x01, (byte) 0xFE, 0x05, (byte) 0xFF});
		NodePositions payloads = new NodePositions();
		bp.decode(bref, payloads);
		assertPositions(new int[]{1, 254, 5, 255}, 0, payloads);
	}
	
	public void testDecodeWithOffset() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x00, 0x00, 0x01, 0x00, 0x01, (byte) 0xFE, 0x05, (byte) 0xFF}, 4, 4);
		NodePositions payloads = new NodePositions();
		payloads.size = 8;
		bp.decode(bref, payloads);
		assertPositions(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 254, 5, 255}, 0, payloads);
	}
	
	public void testDecodeWithExpandedBuffer() throws Exception {
		BytesRef bref = new BytesRef(new byte[]{0x00, 0x00, 0x01, 0x00, 0x01, (byte) 0xFE, 0x05, (byte) 0xFF}, 4, 4);
		NodePositions payloads = new NodePositions();
		assertEquals(128, payloads.positions.length); // default size
		payloads.size = 124; 
		bp.decode(bref, payloads);
		int[] expectedReturned = new int[4 + 124];
		expectedReturned[124] = 1;
		expectedReturned[125] = 254;
		expectedReturned[126] = 5;
		expectedReturned[127] = 255;
		assertPositions(expectedReturned, 0, payloads);
		assertEquals(256, payloads.positions.length);
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
	
	public void testEncodeAndDecode() throws Exception {
		BytesRef[] brs = bp.encode(new int[]{1, 254, 5, 255}, 1);
		assertEquals(1, brs.length);
		BytesRef br = brs[0];
		assertEquals(1, br.bytes[br.offset]);
		assertEquals(254, br.bytes[br.offset + 1] & 255);
		assertEquals(5, br.bytes[br.offset + 2]);
		assertEquals(255, br.bytes[br.offset + 3] & 255);
		
		NodePositions buffer = new NodePositions();
		bp.decode(br, buffer);
		assertEquals(4, buffer.size);
		int[] expectedPos = new int[] {1, 254, 5, 255};
		for (int i = 0; i < 4; i++) {
			assertEquals(expectedPos[i], buffer.positions[i]);
		}
		assertEquals(0, buffer.offset);
		
		brs = bp.encode(new int[] {100, 121, 36, 210}, 1);
		bp.decode(brs[0], buffer);
		
		assertEquals(8, buffer.size);
		expectedPos = new int[] {100, 121, 36, 210};
		for (int i = 4; i < 8; i++) {
			assertEquals(expectedPos[i - 4], buffer.positions[i]);
		}
		assertEquals(0, buffer.offset);
	}
	
	public void testEncodeNumLargerThan255ThrowsException() throws Exception {
		try {
			bp.encode(new int[]{1, 300, 5, 315}, 1);
			fail("expected exception");
		} catch (PayloadFormatException pfe) {
			//do nothing
		}
	}
}
