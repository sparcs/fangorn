package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.Before;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.LRDP;
import au.edu.unimelb.csse.paypack.BytePacking;

public abstract class PairJoinTestCase extends IndexTestCase {
	protected PairJoin join;
	protected LogicalNodePositionDecorator lrdp;
	private CountingBinaryOperatorAware countingOperatorAware;
	private NodePairPositions result;
	protected NodePositions buffer;
	protected NodePositions prev;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		lrdp = new LogicalNodePositionDecorator(new LRDP(new BytePacking(
				LRDP.POSITION_LENGTH)));
		countingOperatorAware = lrdp.getCountingBinaryOperator();
		result = new NodePairPositions();
		buffer = new NodePositions();
		prev = new NodePositions();
	}

	protected void joinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, NodePositions prev,
			BinaryOperator operator, DocsAndPositionsEnum posEnum)
			throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;
		final int numBuffers = join.numBuffers(operator);
		NodePositions[] buffers = new NodePositions[numBuffers];
		buffers[0] = buffer;
		for (int i = 1; i < numBuffers; i++) {
			buffers[i] = new NodePositions();
		}
		// Uhh! ugly instanceof check
		if (join instanceof FullPairJoin) {
			FullPairJoin j = (FullPairJoin) join;
			j.match(prev, operator, posEnum, result, buffers);
			resultSize = result.size;
		} else if (join instanceof HalfPairJoin) {
			HalfPairJoin j = (HalfPairJoin) join;
			j.match(prev, operator, posEnum, buffers);
			resultSize = buffer.size;
		}
		assertEquals("Incorrect number of results", expectedNumResults,
				resultSize);
		int endCount = countingOperatorAware.getCount();
		assertEquals("Incorrect number of comparisons", expectedComparisons,
				endCount - startCount);
	}

	protected DocsAndPositionsEnum initPrevGetNext(IndexReader r,
			final int expectedPrevLength) throws IOException {
		return initPrevGetNext(r, expectedPrevLength, 0, "AA", "DD");
	}

	protected DocsAndPositionsEnum initPrevGetNext(IndexReader r,
			final int expectedPrevLength, final int docid,
			final String prevTerm, final String nextTerm) throws IOException {
		DocsAndPositionsEnum posEnum = getPosEnum(r, docid, new Term("s",
				prevTerm));
		lrdp.getAllPositions(prev, posEnum);
		assertEquals(expectedPrevLength, prev.size);
		posEnum = getPosEnum(r, docid, new Term("s", nextTerm));
		return posEnum;
	}

}
