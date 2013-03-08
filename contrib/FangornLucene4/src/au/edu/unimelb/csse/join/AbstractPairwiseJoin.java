package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.Constants;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.PhysicalPayloadFormatAware;

abstract class AbstractPairwiseJoin implements OperatorCompatibilityAware {

	static final int DEFAULT_BUF_SIZE = 256;
	
	protected PhysicalPayloadFormatAware payloadFormat = Constants.DEFAULT_PAYLOAD_FORMAT;
	
	public int[] getAllPositions(DocsAndPositionsEnum node) throws IOException {
		int freq = node.freq();
		int[] positions = new int[freq * 4];
		int posIndex = 0;
		while (posIndex < freq) {
			node.nextPosition();
			int[] payArr = positions;
			payArr = payloadFormat.decode(node.getPayload(), payArr, posIndex * 4);
			posIndex++;
		}
		return positions;
	}

	protected int[] addNextToResult(int[] result, int resultSize, int[] next, int noff) {
		if (!(resultSize * 4 + 4 < result.length)) {
			int[] newresult = new int[result.length + DEFAULT_BUF_SIZE];
			System.arraycopy(result, 0, newresult, 0, result.length);
			result = newresult;
		}
		System.arraycopy(next, noff, result, resultSize * 4, 4);
		return result;
	}
	
	@Override
	public boolean check(BinaryOperator op) {
		return op.equals(BinaryOperator.CHILD)
				|| op.equals(BinaryOperator.DESCENDANT);
	}
}
