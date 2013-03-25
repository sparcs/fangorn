package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.join.NodePositions;

public class BytePacking implements PhysicalPayloadFormatAware {
	private final int intsPerPos;

	public BytePacking(int nodePositionLength) {
		intsPerPos = nodePositionLength;
	}

	@Override
	public BytesRef[] encode(int[] positionEncodings, int size)
			throws PayloadFormatException {
		byte[] bytes = new byte[size * intsPerPos];
		BytesRef[] brs = new BytesRef[size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < intsPerPos; j++) {
				if (positionEncodings[i * intsPerPos + j] > 255) {
					throw new PayloadFormatException(
							"Error encoding tree position. Value exceeds payload limit in the BytePacking mode.");
				}
				bytes[i * intsPerPos + j] = (byte) (positionEncodings[i
						* intsPerPos + j] & 255);
			}
			brs[i] = new BytesRef(bytes, i * intsPerPos, intsPerPos);
		}
		return brs;
	}

	@Override
	public void decode(BytesRef payload, NodePositions buffer) {
		while (buffer.size + intsPerPos >= buffer.positions.length) {
			buffer.expand();
		}
		for (int i = 0; i < intsPerPos; i++) {
			final int value = payload.bytes[payload.offset + i] & 0xFF;
			buffer.positions[buffer.size] = value;
			buffer.size += 1;
		}
//		buffer.offset = buffer.size - intsPerPos;
	}

}
