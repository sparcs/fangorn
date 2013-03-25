package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.Operator;

public class MPMGJoinTest extends PairJoinTestCase {

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new MPMGJoin(lrdp);
	}

	@Test
	public void testTree1Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(AA DD)(AA CC)(AA CC))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(4, 5, prev, Operator.DESCENDANT, posEnum);
	}

	// next few tests compare the performance of the join for descendant and
	// child operators

	@Test
	public void testNoResultsDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		// 1 comparison at desc opr; 2 others in the elseif block
		joinAndAssertOutput(0, 2, prev, Operator.DESCENDANT, posEnum);
	}

	@Test
	public void testNoResultsChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		// 1 comparison at child opr; 2 others in the elseif block
		joinAndAssertOutput(0, 2, prev, Operator.CHILD, posEnum);
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		// 8 comparisons at desc opr; 3 at others
		joinAndAssertOutput(24, 10, prev, Operator.DESCENDANT, posEnum);
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 23, prev, Operator.CHILD, posEnum);
	}

	@Test
	public void testTree3Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(24, 14, prev, Operator.DESCENDANT, posEnum);
	}

	@Test
	public void testTree3Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 22, prev, Operator.CHILD, posEnum);
	}
	
	public void testResultsOrderedBy1stsPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 10, prev, Operator.DESCENDANT, posEnum);
		
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
	
	public void testDoesNotMatch() throws Exception {
		String sent = "(S1 (S (S (NP (NNP Chubb)) (VP (VBP &amp))) (: ;) (S (NP (NNP Co) (NN insurance) (NN company)) (VP (VP (AUX was) (RB not) (VP (VBN convinced) (PP (IN of) (NP (NP (DT the) (NN claim)) (PP (IN of) (NP (VBN stolen) (NN jewelry))))))) (CC and) (VP ( VBD accused) (NP (NNP Millard)) (PP (IN of) (NP (NN insurance) (NN fraud)))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum vpPosEnum = initPrevGetNext(r, 28, 0, "NP", "VP");
		
		joinAndAssertOutput(0, 20, prev, Operator.DESCENDANT, vpPosEnum);
	}
}
