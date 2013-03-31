package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.join.NodePositions;

public class BytePacking2212 implements PhysicalPayloadFormatAware {
	private static final int _65280 = 0x0000FF00;
	private static final int _255 = 0x000000FF;
	private static final int BYTES_PER_POS = 7;
	private static final int INTS_PER_POS = 4;

	@Override
	public BytesRef[] encode(int[] posEncodings, int size)
			throws PayloadFormatException {
		BytesRef[] encoded = new BytesRef[size];
		byte[] bytes = new byte[size * BYTES_PER_POS];
		for (int i = 0; i < size; i++) {
			int byteoff = i * BYTES_PER_POS;
			int intoff = i * INTS_PER_POS;
			if (posEncodings[intoff + 0] > _65280
					|| posEncodings[intoff + 1] > _65280
					|| posEncodings[intoff + 2] > _255
					|| posEncodings[intoff + 3] > _65280) {
				throw new PayloadFormatException(
						"Error encoding tree position. Value exceeds payload limit in the BytePacking2212 mode.");
			}
			bytes[byteoff] = (byte) ((posEncodings[intoff + 0] & _65280) >> 8);
			bytes[byteoff + 1] = (byte) (posEncodings[intoff + 0] & _255);
			bytes[byteoff + 2] = (byte) ((posEncodings[intoff + 1] & _65280) >> 8);
			bytes[byteoff + 3] = (byte) (posEncodings[intoff + 1] & _255);
			bytes[byteoff + 4] = (byte) (posEncodings[intoff + 2] & _255);
			bytes[byteoff + 5] = (byte) ((posEncodings[intoff + 3] & _65280) >> 8);
			bytes[byteoff + 6] = (byte) (posEncodings[intoff + 3] & _255);
			encoded[i] = new BytesRef(bytes, byteoff, BYTES_PER_POS);
		}
		return encoded;
	}

	@Override
	public void decode(BytesRef payload, NodePositions buffer) {
		while (buffer.size + INTS_PER_POS >= buffer.positions.length) {
			buffer.expand();
		}
		int payoff = payload.offset;
		buffer.positions[buffer.size] = payload.bytes[payoff] & _255;
		buffer.positions[buffer.size] <<= 8;
		buffer.positions[buffer.size] |= payload.bytes[payoff + 1] & _255;
		buffer.size++;
		buffer.positions[buffer.size] = payload.bytes[payoff + 2] & _255;
		buffer.positions[buffer.size] <<= 8;
		buffer.positions[buffer.size] |= payload.bytes[payoff + 3] & _255;
		buffer.size++;
		buffer.positions[buffer.size] = payload.bytes[payoff + 4] & _255;
		buffer.size++;
		buffer.positions[buffer.size] = payload.bytes[payoff + 5] & _255;
		buffer.positions[buffer.size] <<= 8;
		buffer.positions[buffer.size] |= payload.bytes[payoff + 6] & _255;
		buffer.size++;
	}

}
