package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.Operator;

public class MPMGMRRJoinTest extends PairJoinTestCase {

	private JoinBuilder jb = MPMGMRRJoin.JOIN_BUILDER;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testSkipsPrevAAsStopsAtNextAA() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(12, 5, jb, prev, Operator.CHILD, posEnum);
	}

	// the next few tests compare MPMGModSingle with vanilla MPMG join

	@Test
	public void testTree1Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(AA DD)(AA CC)(AA CC))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(4, 4, jb, prev, Operator.DESCENDANT, posEnum);
		// was 5 comparisons in MPMG
	}

	@Test
	public void testNoResultsDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 2, jb, prev, Operator.DESCENDANT, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testNoResultsChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 2, jb, prev, Operator.CHILD, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(16, 4, jb, prev, Operator.DESCENDANT, posEnum);
		// was 10 in MPMG
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 8, jb, prev, Operator.CHILD, posEnum);
		// was 23 in MPMG
	}

	@Test
	public void testTree3Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 3, jb, prev, Operator.DESCENDANT, posEnum);
		// was 14 in MPMG
	}

	@Test
	public void testTree3Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 9, jb, prev, Operator.CHILD, posEnum);
		// was 22 in MPMG
	}
	
	public void testAncestorOpWithNestedDescendants() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "DD", "AA");
		joinAndAssertOutput(12, 7, jb, prev, Operator.ANCESTOR, posEnum);
	}
	
	public void testParentOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16, 0, "CC", "PP");
		joinAndAssertOutput(8, 7, jb, prev, Operator.PARENT, posEnum);
	}
	
	public void testDescendantOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "AA", "DD");
		joinAndAssertOutput(8, 6, jb, prev, Operator.DESCENDANT, posEnum);
	}
	
	public void testChildOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "PP", "CC");
		joinAndAssertOutput(8, 7, jb, prev, Operator.CHILD, posEnum);
	}
	
	public void testChildOp2() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B A)(A C))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "A", "C");
		joinAndAssertOutput(4, 3, jb, prev, Operator.CHILD, posEnum);
		assertPositions(new int[] {1, 2, 2, 2}, 0, bufferResult);
	}

}
