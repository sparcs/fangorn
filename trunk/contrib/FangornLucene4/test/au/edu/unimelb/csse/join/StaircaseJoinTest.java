package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;

import au.edu.unimelb.csse.BinaryOperator;

public class StaircaseJoinTest extends PairJoinTestCase {
	
	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new StaircaseJoin(lrdp);
	}
	
	public void testPruneReturnsSamePrevWhenLengthIsLessThan4()
			throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		BinaryOperator[] ops = new BinaryOperator[] { BinaryOperator.DESCENDANT, BinaryOperator.CHILD, BinaryOperator.ANCESTOR,
				BinaryOperator.PARENT, BinaryOperator.FOLLOWING, BinaryOperator.PRECEDING };
		for (BinaryOperator op : ops) {
			NodePositions prev = new NodePositions();
			prev.setValues(new int[] { 1, 4, 2, 12 });
			j.prune(prev, op, null);
			assertPositions(new int[] { 1, 4, 2, 12 }, 0, prev);
		}
	}

	public void testDescendantOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		prev.setValues(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2,
				28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneDescendant(prev, buffers); // prune two leave two
		assertPositions(new int[] { 1, 4, 2, 12, 5, 9, 2, 28 }, 0, prev);

		prev.setValues(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		resetBuffers(buffers);
		j.pruneDescendant(prev, buffers); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);

		prev.setValues(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		resetBuffers(buffers);
		j.pruneDescendant(prev, buffers); // prune all but 1
		assertPositions(new int[] { 1, 17, 2, 65 }, 0, prev);
	}

	public void testAncestorOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		prev.setValues(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2,
				28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneAncestor(prev, buffers); // prune two leave two
		assertPositions(new int[] { 2, 3, 3, 4, 7, 9, 5, 22 }, 0, prev);

		resetBuffers(buffers);
		prev.setValues(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		j.pruneAncestor(prev, buffers); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);

		resetBuffers(buffers);
		prev.setValues(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		j.pruneAncestor(prev, buffers); // prune one
		assertPositions(new int[] { 4, 5, 3, 21, 5, 9, 4, 28, 9, 13, 5, 38 },
				0, prev);

		resetBuffers(buffers);
		prev.setValues(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 4, 5, 4, 20 });
		j.pruneAncestor(prev, buffers); // retain one (nested pruning)
		assertPositions(new int[] { 4, 5, 4, 20 }, 0, prev);
	}

	private NodePositions[] setupBuffers() {
		NodePositions[] buffers = new NodePositions[3];
		buffers[0] = new NodePositions();
		buffers[1] = new NodePositions();
		buffers[2] = new NodePositions();
		return buffers;
	}

	private void resetBuffers(NodePositions[] buffers) {
		for (int i = 0; i < 3; i++) {
			buffers[i].reset();
		}
	}

	public void testFollowingOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		prev.setValues(new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneFollowing(prev, buffers); // retain first
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);

		prev.setValues(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7,
				9, 5, 22 });
		resetBuffers(buffers);
		j.pruneFollowing(prev, buffers); // find descendant and stop
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);

		prev.setValues(new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(buffers);
		j.pruneFollowing(prev, buffers); // find last descendant
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testPrecedingOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		prev.setValues(new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.prunePreceding(prev, buffers); // retain last
		assertPositions(new int[] { 7, 9, 5, 22 }, 0, prev);

		prev.setValues(new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(buffers);
		j.prunePreceding(prev, buffers);
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testFollowingOpWithSimpleFollows() throws Exception {
		
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 3, prev, BinaryOperator.FOLLOWING, posEnum);
	}

	public void testFollowingOpWithOnePrecedingIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 4, prev, BinaryOperator.FOLLOWING, posEnum);
	}

	public void testFollowingOpWithNoneFollowing() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(JJ WW))(ZZ(JJ WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(0, 2, prev, BinaryOperator.FOLLOWING, posEnum);
	}

	public void testPrecedingOpSimple() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 2, prev, BinaryOperator.PRECEDING, posEnum);
	}

	public void testPrecedingOpWithNonePreceding() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(NN(FF WW))(ZZ(FF WW))(PP(PP WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(0, 2, prev, BinaryOperator.PRECEDING, posEnum);
	}

	public void testPrecedingOpWithOneIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW)(FF WW))(ZZ(FF WW))(PP WW))"); 																			
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 3, prev, BinaryOperator.PRECEDING, posEnum);
	}

	public void testDescendantOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "AA", "DD");
		joinAndAssertOutput(8, 12, prev, BinaryOperator.DESCENDANT, posEnum);
	}

	public void testChildOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS" + "(AA(PP WW)(AA(CC WW))(CC WW))"
				+ "(AA(PP WW)(AA(DD(CC WW)))(CC WW))" + "(ZZ(CC WW))"
				+ "(AA(FF WW))" + "(AA(CC(AA(DD(CC WW))))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 28, 0, "AA", "CC");
		joinAndAssertOutput(16, 35, prev, BinaryOperator.CHILD, posEnum);
		assertPositions(new int[] { 1, 2, 3, 3, 2, 3, 2, 5, 5, 6, 2, 11, 8, 9,
				2, 20 }, 12, buffer);
	}
	
	public void testAncestorOpWithNestedDescendants() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "DD", "AA");
		joinAndAssertOutput(12, 13, prev, BinaryOperator.ANCESTOR, posEnum);
	}
	
	public void testChildOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "PP", "CC");
		joinAndAssertOutput(8, 16, prev, BinaryOperator.CHILD, posEnum);
	}
	
	public void testParentOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))"); 
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16, 0, "CC", "PP");
		joinAndAssertOutput(8, 11, prev, BinaryOperator.PARENT, posEnum);
	}
	

}
