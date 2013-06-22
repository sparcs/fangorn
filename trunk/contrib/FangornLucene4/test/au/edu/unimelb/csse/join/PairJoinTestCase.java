package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.junit.Before;

import au.edu.unimelb.csse.CountingOperatorAware;
import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionDecorator;

public abstract class PairJoinTestCase extends IndexTestCase {
	protected LogicalNodePositionDecorator lrdp;
	private CountingOperatorAware countingOperatorAware;
	public NodePairPositions result;
	protected NodePositions bufferResult;
	protected NodePositions prev;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		lrdp = new LogicalNodePositionDecorator(new LRDP(
				LRDP.PhysicalPayloadFormat.BYTE1111));
		countingOperatorAware = lrdp.getCountingOperatorAware();
		result = new NodePairPositions();
		bufferResult = new NodePositions();
		prev = new NodePositions();
	}

	protected void joinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, FullPairJoin join, NodePositions prev,
			Operator operator, DocsAndPositionsEnum posEnum) throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;

		join.match(prev, operator, posEnum, result);
		resultSize = result.size;

		assertEquals("Incorrect number of results", expectedNumResults,
				resultSize);
		int endCount = countingOperatorAware.getCount();
		assertEquals("Incorrect number of comparisons", expectedComparisons,
				endCount - startCount);
	}

	protected void joinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, HalfPairJoin join, NodePositions prev,
			Operator operator, DocsAndPositionsEnum posEnum) throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;

		bufferResult = join.match(prev, operator, posEnum);
		resultSize = bufferResult.size;
		assertEquals("Incorrect number of results", expectedNumResults,
				resultSize);
		int endCount = countingOperatorAware.getCount();
		assertEquals("Incorrect number of comparisons", expectedComparisons,
				endCount - startCount);
	}
	
	protected void joinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, JoinBuilder joinBuilder, NodePositions prev,
			Operator operator, DocsAndPositionsEnum posEnum) throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;

		HalfPairJoin join = joinBuilder.getHalfPairJoin(operator, lrdp);
		bufferResult = join.match(prev, operator, posEnum);
		resultSize = bufferResult.size;
		assertEquals("Incorrect number of results", expectedNumResults,
				resultSize);
		int endCount = countingOperatorAware.getCount();
		assertEquals("Incorrect number of comparisons", expectedComparisons,
				endCount - startCount);
	}

	protected void lookaheadJoinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, JoinBuilder jb, NodePositions prev,
			Operator op, Operator nextOp, DocsAndPositionsEnum posEnum, int i)
			throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;

		HalfPairLATEJoin join = (HalfPairLATEJoin) jb.getHalfPairJoin(op, lrdp);
		bufferResult = join.matchWithLookahead(prev, op, posEnum, nextOp);
		resultSize = bufferResult.size;
		assertEquals("Incorrect number of results at pos " + i,
				expectedNumResults, resultSize);
		int endCount = countingOperatorAware.getCount();
		assertEquals("Incorrect number of comparisons at pos " + i,
				expectedComparisons, endCount - startCount);
	}

	protected void termEarlyJoinAndAssertOutput(int expectedNumResults,
			int expectedComparisons, JoinBuilder jb, NodePositions prev,
			Operator op, DocsAndPositionsEnum posEnum) throws IOException {
		int startCount = countingOperatorAware.getCount();
		int resultSize = 0;

		HalfPairLATEJoin join = (HalfPairLATEJoin) jb.getHalfPairJoin(op, lrdp);
		bufferResult = join.matchTerminateEarly(prev, op, posEnum);
		resultSize = bufferResult.size;
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

	protected void assertNodePairPositions(int[] expectedPos1,
			int[] expectedPos2, int idx, int posLength) {
		int arrOff = idx * posLength;
		for (int i = 0; i < posLength; i++) {
			assertEquals("Incorrect value in result node1 at position "
					+ (arrOff + i), expectedPos1[i], result.node1[arrOff + i]);
			assertEquals("Incorrect value in result node2 at position "
					+ (arrOff + i), expectedPos2[i], result.node2[arrOff + i]);
		}
	}

}
