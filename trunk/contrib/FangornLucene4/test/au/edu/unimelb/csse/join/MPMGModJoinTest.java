package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.Operator;

public class MPMGModJoinTest extends PairJoinTestCase {

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new MPMGModJoin(lrdp);
	}

	public void testSkipsPrevAAsStopsAtNextAA() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(12, 12, prev, Operator.CHILD, posEnum);
	}

	// the next few tests compare MPMGMod with vanilla MPMG join

	@Test
	public void testNoResultsDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, prev, Operator.DESCENDANT, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testNoResultsChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, prev, Operator.CHILD, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testTree1Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(AA DD)(AA CC)(AA CC))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(4, 11, prev, Operator.DESCENDANT, posEnum);
		// 5 in MPMG
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 14, prev, Operator.DESCENDANT, posEnum);
		// 10 in MPMG
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 20, prev, Operator.CHILD, posEnum);
		// 23 in MPMG
	}

	@Test
	public void testTree3Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(24, 19, prev, Operator.DESCENDANT, posEnum);
		// 14 in MPMG
	}

	@Test
	public void testTree3Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 22, prev, Operator.CHILD, posEnum);
		// 22 in MPMG
	}
	
	public void testResultsOrderedBy1stsPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 14, prev, Operator.DESCENDANT, posEnum);
		
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
