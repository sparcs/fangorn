package au.edu.unimelb.csse.paypack;

import org.apache.lucene.util.BytesRef;

public interface PayloadFormatAware {
	public BytesRef[] encode(int[] positionEncodings, int size) throws PayloadFormatException;
	
	public int[] decode(BytesRef bytesRef, int[] buffer, int pos);
	
}
