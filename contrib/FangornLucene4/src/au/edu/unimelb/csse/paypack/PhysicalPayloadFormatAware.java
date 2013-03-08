package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

import au.edu.unimelb.csse.join.NodePositions;

public interface PhysicalPayloadFormatAware {
	public BytesRef[] encode(int[] positionEncodings, int size) throws PayloadFormatException;
	
	public int[] decode(BytesRef bytesRef, int[] buffer, int pos);
	
	public void decode(BytesRef payload, NodePositions buffer);
	
}
