package au.edu.unimelb.csse.join;

abstract class FullSolutionPairwiseJoin extends AbstractPairwiseJoin {

	protected int[] addToResult(int[] result, int resultSize, int[] prev,
			int poff, int[] nextPays, int noff) {
		try {
			return addToResultChecked(result, resultSize, prev, poff, nextPays,
					noff);
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] newResult = new int[result.length + 128];
			System.arraycopy(result, 0, newResult, 0, result.length);
			result = newResult;
			return addToResultChecked(result, resultSize, prev, poff, nextPays,
					noff);
		}
	}

	private int[] addToResultChecked(int[] result, int resultSize, int[] prev,
			int poff, int[] nextPays, int noff) {
		System.arraycopy(prev, poff, result, resultSize * 8, 4);
		System.arraycopy(nextPays, noff, result, resultSize * 8 + 4, 4);
		return result;
	}
}
