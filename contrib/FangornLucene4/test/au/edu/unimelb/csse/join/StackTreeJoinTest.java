package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.Operator;

public class StackTreeJoinTest extends PairJoinTestCase {
	StackTreeJoin join;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new StackTreeJoin(lrdp);
	}

	public void testNoDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(NA ND))",
				"(SS(NA ND)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "AA", "ND");
		joinAndAssertOutput(0, 4, join, prev, Operator.DESCENDANT, posEnum);
		posEnum = initPrevGetNext(r, 8, 1, "AA", "ND");
		joinAndAssertOutput(0, 1, join, prev, Operator.DESCENDANT, posEnum);
	}

	public void testNestedTreeDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(24, 17, join, prev, Operator.DESCENDANT, posEnum);
	}

	public void testNestedTreeChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 17, join, prev, Operator.CHILD, posEnum);
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 13, join, prev, Operator.DESCENDANT, posEnum);
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 13, join, prev, Operator.CHILD, posEnum);
	}

	@Test
	public void testResultsOrderedBy1stsPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 13, join, prev, Operator.DESCENDANT, posEnum);

		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 3,
				3 }, 2, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 3, 4, 2,
				5 }, 3, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 4, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 2, 3, 3,
				3 }, 5, lrdp.getPositionLength());
	}
	
}
