package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;

import au.edu.unimelb.csse.Operator;

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
		Operator[] ops = new Operator[] { Operator.DESCENDANT, Operator.CHILD,
				Operator.ANCESTOR, Operator.PARENT, Operator.FOLLOWING,
				Operator.PRECEDING };
		for (Operator op : ops) {
			NodePositions prev = new NodePositions();
			setValues(prev, new int[] { 1, 4, 2, 12 });
			j.prune(prev, op, null);
			assertPositions(new int[] { 1, 4, 2, 12 }, 0, prev);
		}
	}

	private void setValues(NodePositions prev, int[] values) {
		System.arraycopy(values, 0, prev.positions, 0, values.length);
		prev.size = values.length;
		prev.offset = 0;
	}

	public void testDescendantOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneDescendant(prev, buffers); // prune two leave two
		assertPositions(new int[] { 1, 4, 2, 12, 5, 9, 2, 28 }, 0, prev);
		setValues(prev, new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		resetBuffers(buffers);
		j.pruneDescendant(prev, buffers); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		resetBuffers(buffers);
		j.pruneDescendant(prev, buffers); // prune all but 1
		assertPositions(new int[] { 1, 17, 2, 65 }, 0, prev);
	}

	public void testAncestorOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneAncestor(prev, buffers); // prune two leave two
		assertPositions(new int[] { 2, 3, 3, 4, 7, 9, 5, 22 }, 0, prev);

		resetBuffers(buffers);
		setValues(prev, new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		j.pruneAncestor(prev, buffers); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);

		resetBuffers(buffers);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		j.pruneAncestor(prev, buffers); // prune one
		assertPositions(new int[] { 4, 5, 3, 21, 5, 9, 4, 28, 9, 13, 5, 38 },
				0, prev);

		resetBuffers(buffers);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 4, 5, 4, 20 });
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
		setValues(prev, new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.pruneFollowing(prev, buffers); // retain first
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		resetBuffers(buffers);
		j.pruneFollowing(prev, buffers); // find descendant and stop
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(buffers);
		j.pruneFollowing(prev, buffers); // find last descendant
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testPrecedingOpPruning() throws Exception {
		StaircaseJoin j = (StaircaseJoin) join;
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		NodePositions[] buffers = setupBuffers();
		j.prunePreceding(prev, buffers); // retain last
		assertPositions(new int[] { 7, 9, 5, 22 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(buffers);
		j.prunePreceding(prev, buffers);
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testFollowingOpWithSimpleFollows() throws Exception {

		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 3, prev, Operator.FOLLOWING, posEnum);
	}

	public void testFollowingOpWithOnePrecedingIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 4, prev, Operator.FOLLOWING, posEnum);
	}

	public void testFollowingOpWithNoneFollowing() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(JJ WW))(ZZ(JJ WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(0, 2, prev, Operator.FOLLOWING, posEnum);
	}

	public void testPrecedingOpSimple() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 2, prev, Operator.PRECEDING, posEnum);
	}

	public void testPrecedingOpWithNonePreceding() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(NN(FF WW))(ZZ(FF WW))(PP(PP WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(0, 2, prev, Operator.PRECEDING, posEnum);
	}

	public void testPrecedingOpWithOneIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW)(FF WW))(ZZ(FF WW))(PP WW))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 3, prev, Operator.PRECEDING, posEnum);
	}

	public void testDescendantOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "AA", "DD");
		joinAndAssertOutput(8, 12, prev, Operator.DESCENDANT, posEnum);
	}

	public void testChildOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS"
				+ "(AA(PP WW)(AA(CC WW))(CC WW))"
				+ "(AA(PP WW)(AA(DD(CC WW)))(CC WW))" + "(ZZ(CC WW))"
				+ "(AA(FF WW))" + "(AA(CC(AA(DD(CC WW))))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 28, 0, "AA", "CC");
		joinAndAssertOutput(16, 54, prev, Operator.CHILD, posEnum);
		assertPositions(new int[] { 1, 2, 3, 3, 2, 3, 2, 5, 5, 6, 2, 11, 8, 9,
				2, 20 }, 12, buffer);
	}

	public void testAncestorOpWithNestedDescendants() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "DD", "AA");
		joinAndAssertOutput(12, 13, prev, Operator.ANCESTOR, posEnum);
	}

	public void testChildOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "PP", "CC");
		joinAndAssertOutput(8, 14, prev, Operator.CHILD, posEnum);
	}

	public void testParentOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16, 0, "CC", "PP");
		joinAndAssertOutput(8, 18, prev, Operator.PARENT, posEnum);
	}
	
	public void testChildError() throws Exception {
		String sent = "(S1 (S (PP (IN In) (NP (NP (NNP September)) (, ,) (NP (CD 2008)))) (, ,) (NP (DT the) (NN organization)) (VP (VBD marked) (NP (NP (NP (DT the) (JJ 5th) ('' ') (NN anniversary) ('' ')) (PP (IN of) (NP (NP (NP (DT the) (NNP RIAA) (POS 's)) (NN litigation) (NN campaign)) (PP (IN by) (S (VP (VBG publishing) (NP (DT a) (ADJP (RB highly) (JJ critical)) (, ,) (JJ detailed) (NN report))))) (, ,) (VP (VBN entitled) ('' ') (NP (NNP RIAA)) (PP (IN v.) (NP (NNP The) (NNP People))))))) (: :) (NP (NP (CD Five) (NNS Years)) (RB Later) (POS '))) (, ,) (S (VP (VBG concluding) (SBAR (IN that) (S (NP (DT the) (NN campaign)) (VP (AUX was) (NP (DT a) (NN failure)))))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 68, 0, "NP", "VP");
		joinAndAssertOutput(4, 113, prev, Operator.CHILD, posEnum);
	}
	
	public void testParentError() throws Exception {
		String sent = "(S1 (S (PP (IN In) (NP (NP (NNP September)) (, ,) (NP (CD 2008)))) (, ,) (NP (DT the) (NN organization)) (VP (VBD marked) (NP (NP (NP (DT the) (JJ 5th) ('' ') (NN anniversary) ('' ')) (PP (IN of) (NP (NP (NP (DT the) (NNP RIAA) (POS 's)) (NN litigation) (NN campaign)) (PP (IN by) (S (VP (VBG publishing) (NP (DT a) (ADJP (RB highly) (JJ critical)) (, ,) (JJ detailed) (NN report))))) (, ,) (VP (VBN entitled) ('' ') (NP (NNP RIAA)) (PP (IN v.) (NP (NNP The) (NNP People))))))) (: :) (NP (NP (CD Five) (NNS Years)) (RB Later) (POS '))) (, ,) (S (VP (VBG concluding) (SBAR (IN that) (S (NP (DT the) (NN campaign)) (VP (AUX was) (NP (DT a) (NN failure)))))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "VP", "NP");
		joinAndAssertOutput(4, 99, prev, Operator.PARENT, posEnum);
	}

}
