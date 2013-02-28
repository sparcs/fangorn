package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

public class BytePacking implements PayloadFormatAware {

	@Override
	public BytesRef[] encode(int[] positionEncodings, int size) throws PayloadFormatException {
		byte[] bytes = new byte[size * 4];
		BytesRef[] brs = new BytesRef[size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < 4; j++) {
				if (positionEncodings[i * 4 + j] > 255) {
					throw new PayloadFormatException(
							"Error encoding tree position. Value exceeds payload limit in the BytePacking mode.");
				}
				bytes[i * 4 + j] = (byte) (positionEncodings[i * 4 + j] & 255);
			}
			brs[i] = new BytesRef(bytes, i * 4, 4);
		}
		return brs;
	}

	@Override
	public int[] decode(BytesRef bytesRef, int[] buffer, int offset) {
		try {
			buffer[offset + 3] = 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] newBuffer = new int[buffer.length + 128];
			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
			buffer = newBuffer;
		}
		for (int i = 0; i < 4; i++) {
			buffer[offset + i] = bytesRef.bytes[bytesRef.offset + i] & 0xFF;
		}
		return buffer;
	}

}
