package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.BinaryOperator;

public class MPMGModSingleJoinTest extends PairJoinTestCase {

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new MPMGModSingleJoin(lrdp);
	}

	public void testSkipsPrevAAsStopsAtNextAA() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(12, 6, prev, BinaryOperator.CHILD, posEnum);
	}

	// the next few tests compare MPMGModSingle with vanilla MPMG join

	@Test
	public void testTree1Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(AA DD)(AA CC)(AA CC))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(4, 9, prev, BinaryOperator.DESCENDANT, posEnum);
		// was 5 comparisons in MPMG
	}

	@Test
	public void testNoResultsDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, prev, BinaryOperator.DESCENDANT, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testNoResultsChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, prev, BinaryOperator.CHILD, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(16, 8, prev, BinaryOperator.DESCENDANT, posEnum);
		// was 10 in MPMG
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 20, prev, BinaryOperator.CHILD, posEnum);
		// was 23 in MPMG
	}

	@Test
	public void testTree3Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 6, prev, BinaryOperator.DESCENDANT, posEnum);
		// was 14 in MPMG
	}

	@Test
	public void testTree3Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 12, prev, BinaryOperator.CHILD, posEnum);
		// was 22 in MPMG
	}
}
