package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;

import au.edu.unimelb.csse.Operator;

public class StaircaseJoinTest extends PairJoinTestCase {
	JoinBuilder jb = StaircaseJoin.JOIN_BUILDER;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testPruneReturnsSamePrevWhenLengthIsLessThan4()
			throws Exception {
		Operator[] ops = new Operator[] { Operator.DESCENDANT, Operator.CHILD,
				Operator.ANCESTOR, Operator.PARENT, Operator.FOLLOWING,
				Operator.PRECEDING };
		for (Operator op : ops) {
			StaircaseJoin j = getJoin(op);
			NodePositions prev = new NodePositions();
			setValues(prev, new int[] { 1, 4, 2, 12 });
			j.prune(prev);
			assertPositions(new int[] { 1, 4, 2, 12 }, 0, prev);
		}
	}

	private void setValues(NodePositions prev, int[] values) {
		System.arraycopy(values, 0, prev.positions, 0, values.length);
		prev.size = values.length;
		prev.offset = 0;
	}

	public void testDescendantOpPruning() throws Exception {
		StaircaseJoin j = getJoin(Operator.DESCENDANT);
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		j.prune(prev); // prune two leave two
		assertPositions(new int[] { 1, 4, 2, 12, 5, 9, 2, 28 }, 0, prev);
		setValues(prev, new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		resetBuffers(j);
		j.prune(prev); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		resetBuffers(j);
		j.prune(prev); // prune all but 1
		assertPositions(new int[] { 1, 17, 2, 65 }, 0, prev);
	}

	public void testAncestorOpPruning() throws Exception {
		StaircaseJoin j = getJoin(Operator.ANCESTOR);
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		j.prune(prev); // prune two leave two
		assertPositions(new int[] { 2, 3, 3, 4, 7, 9, 5, 22 }, 0, prev);

		resetBuffers(j);
		setValues(prev, new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 });
		j.prune(prev); // no pruning
		assertPositions(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, 0, prev);

		resetBuffers(j);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 });
		j.prune(prev); // prune one
		assertPositions(new int[] { 4, 5, 3, 21, 5, 9, 4, 28, 9, 13, 5, 38 },
				0, prev);

		resetBuffers(j);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 4, 5, 4, 20 });
		j.prune(prev); // retain one (nested pruning)
		assertPositions(new int[] { 4, 5, 4, 20 }, 0, prev);
	}

	private void resetBuffers(StaircaseJoin j) {
		j.result.reset();
		j.next.reset();
		j.buffer.reset();
	}

	public void testFollowingOpPruning() throws Exception {
		StaircaseJoin j = getJoin(Operator.FOLLOWING);
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		j.prune(prev); // retain first
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);
		setValues(prev, new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7, 9,
				5, 22 });
		resetBuffers(j);
		j.prune(prev); // find descendant and stop
		assertPositions(new int[] { 2, 3, 3, 4 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(j);
		j.prune(prev); // find last descendant
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testPrecedingOpPruning() throws Exception {
		StaircaseJoin j = getJoin(Operator.PRECEDING);
		NodePositions prev = new NodePositions();
		setValues(prev, new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 });
		j.prune(prev); // retain last
		assertPositions(new int[] { 7, 9, 5, 22 }, 0, prev);
		setValues(prev, new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 });
		resetBuffers(j);
		j.prune(prev);
		assertPositions(new int[] { 4, 5, 6, 18 }, 0, prev);
	}

	public void testFollowingOpWithSimpleFollows() throws Exception {

		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 3, jb, prev, Operator.FOLLOWING, posEnum);
		assertPositions(new int[] { 1, 2, 2, 4, 2, 3, 2, 6 }, 4, bufferResult);
	}

	public void testFollowingOpWithOnePrecedingIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(8, 4, jb, prev, Operator.FOLLOWING, posEnum);
		assertPositions(new int[] { 2, 3, 2, 6, 3, 4, 2, 8 }, 4, bufferResult);
	}

	public void testFollowingOpWithNoneFollowing() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(FF WW))(PP(PP WW))(NN(JJ WW))(ZZ(JJ WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "PP", "FF");
		joinAndAssertOutput(0, 2, jb, prev, Operator.FOLLOWING, posEnum);
	}

	public void testPrecedingOpSimple() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 2, jb, prev, Operator.PRECEDING, posEnum);
		assertPositions(new int[] { 0, 1, 1, 7, 0, 1, 2, 2 }, 4, bufferResult);
	}

	public void testPrecedingOpWithNonePreceding() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(NN(FF WW))(ZZ(FF WW))(PP(PP WW)))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(0, 2, jb, prev, Operator.PRECEDING, posEnum);
	}

	public void testPrecedingOpWithOneIgnored() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(PP(PP WW)(FF WW))(ZZ(FF WW))(PP WW))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "FF", "PP");
		joinAndAssertOutput(8, 3, jb, prev, Operator.PRECEDING, posEnum);
		assertPositions(new int[] { 0, 2, 1, 7, 0, 1, 2, 3 }, 4, bufferResult);
	}

	public void testDescendantOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "AA", "DD");
		joinAndAssertOutput(8, 12, jb, prev, Operator.DESCENDANT, posEnum);
		assertPositions(new int[] { 1, 2, 3, 3, 4, 5, 2, 11 }, 4, bufferResult);
	}

	public void testChildOpWithMixedPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS"
				+ "(AA(PP WW)(AA(CC WW))(CC WW))"
				+ "(AA(PP WW)(AA(DD(CC WW)))(CC WW))" + "(ZZ(CC WW))"
				+ "(AA(FF WW))" + "(AA(CC(AA(DD(CC WW))))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 28, 0, "AA", "CC");
		joinAndAssertOutput(16, 54, jb, prev, Operator.CHILD, posEnum);
		assertPositions(new int[] { 1, 2, 3, 3, 2, 3, 2, 5, 5, 6, 2, 11, 8, 9,
				2, 20 }, 12, bufferResult);
	}

	public void testAncestorOpWithNestedDescendants() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "DD", "AA");
		joinAndAssertOutput(12, 13, jb, prev, Operator.ANCESTOR, posEnum);
		assertPositions(new int[] { 0, 2, 1, 12, 1, 2, 2, 4, 4, 5, 1, 12 }, 8,
				bufferResult);
	}

	public void testChildOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12, 0, "PP", "CC");
		joinAndAssertOutput(8, 14, jb, prev, Operator.CHILD, posEnum);
		assertPositions(new int[] { 1, 2, 2, 4, 4, 5, 3, 9 }, 4, bufferResult);
	}

	public void testParentOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16, 0, "CC", "PP");
		joinAndAssertOutput(8, 18, jb, prev, Operator.PARENT, posEnum);
		assertPositions(new int[] { 1, 3, 1, 11, 4, 6, 2, 10 }, 4, bufferResult);
	}

	public void testChildError() throws Exception {
		String sent = "(S1 (S (PP (IN In) (NP (NP (NNP September)) (, ,) (NP (CD 2008)))) (, ,) (NP (DT the) (NN organization)) (VP (VBD marked) (NP (NP (NP (DT the) (JJ 5th) ('' ') (NN anniversary) ('' ')) (PP (IN of) (NP (NP (NP (DT the) (NNP RIAA) (POS 's)) (NN litigation) (NN campaign)) (PP (IN by) (S (VP (VBG publishing) (NP (DT a) (ADJP (RB highly) (JJ critical)) (, ,) (JJ detailed) (NN report))))) (, ,) (VP (VBN entitled) ('' ') (NP (NNP RIAA)) (PP (IN v.) (NP (NNP The) (NNP People))))))) (: :) (NP (NP (CD Five) (NNS Years)) (RB Later) (POS '))) (, ,) (S (VP (VBG concluding) (SBAR (IN that) (S (NP (DT the) (NN campaign)) (VP (AUX was) (NP (DT a) (NN failure)))))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 68, 0, "NP", "VP");
		joinAndAssertOutput(4, 113, jb, prev, Operator.CHILD, posEnum);
		assertPositions(new int[] { 28, 34, 7, 52 }, 0, bufferResult);
	}

	public void testParentError() throws Exception {
		String sent = "(S1 (S (PP (IN In) (NP (NP (NNP September)) (, ,) (NP (CD 2008)))) (, ,) (NP (DT the) (NN organization)) (VP (VBD marked) (NP (NP (NP (DT the) (JJ 5th) ('' ') (NN anniversary) ('' ')) (PP (IN of) (NP (NP (NP (DT the) (NNP RIAA) (POS 's)) (NN litigation) (NN campaign)) (PP (IN by) (S (VP (VBG publishing) (NP (DT a) (ADJP (RB highly) (JJ critical)) (, ,) (JJ detailed) (NN report))))) (, ,) (VP (VBN entitled) ('' ') (NP (NNP RIAA)) (PP (IN v.) (NP (NNP The) (NNP People))))))) (: :) (NP (NP (CD Five) (NNS Years)) (RB Later) (POS '))) (, ,) (S (VP (VBG concluding) (SBAR (IN that) (S (NP (DT the) (NN campaign)) (VP (AUX was) (NP (DT a) (NN failure)))))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "VP", "NP");
		joinAndAssertOutput(4, 99, jb, prev, Operator.PARENT, posEnum);
		assertPositions(new int[] { 14, 34, 6, 53 }, 0, bufferResult);
	}

	/*-
	 *             N
	 *             |
	 *             P
	 *            / \
	 *           P   N
	 *           |   |
	 *           P   F
	 *         / |
	 *       /   | \
	 *     /   / | \ \
	 *    P   P  N  P  P
	 *   / \  |  |  |  |
	 *  N   P C  D  N  E
	 *  |   |
	 *  A   B
	 *  P (8): [[0,7,1,12], [0,6,2,11], [0,6,3,9], [0,2,4,8], [1,2,5,3], [2,3,4,8], [4,5,4,8], [5,6,4,8]]
	 *  N (5): [[0,7,0,0], [0,1,5,3], [3,4,4,8], [4,5,5,6], [6,7,2,11]]
	 *  
	 * @throws Exception
	 */
	public void testFollowingSibling() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(G C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 28, 0, "P", "N");
		joinAndAssertOutput(8, 49, jb, prev, Operator.FOLLOWING_SIBLING,
				posEnum);
		assertPositions(new int[] { 3, 4, 4, 8, 6, 7, 2, 11 }, 4, bufferResult);
	}

	public void testImmediateFollowingSibling() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(P C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 32, 0, "P", "N");
		joinAndAssertOutput(8, 50, jb, prev,
				Operator.IMMEDIATE_FOLLOWING_SIBLING, posEnum);
		assertPositions(new int[] { 3, 4, 4, 8, 6, 7, 2, 11 }, 4, bufferResult);
	}

	public void testPrecedingSibling() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(P C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "N", "P");
		joinAndAssertOutput(12, 74, jb, prev, Operator.PRECEDING_SIBLING,
				posEnum);
		assertPositions(new int[] { 0, 6, 2, 11, 0, 2, 4, 8, 2, 3, 4, 8 }, 8,
				bufferResult);
	}

	public void testImmediatePrecedingSibling() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(P C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "N", "P");
		joinAndAssertOutput(8, 76, jb, prev,
				Operator.IMMEDIATE_PRECEDING_SIBLING, posEnum);
		assertPositions(new int[] { 0, 6, 2, 11, 2, 3, 4, 8 }, 4, bufferResult);
	}

	public void testImmediateFollowing() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(P C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 32, 0, "P", "N");
		joinAndAssertOutput(8, 34, jb, prev, Operator.IMMEDIATE_FOLLOWING,
				posEnum);
		assertPositions(new int[] { 3, 4, 4, 8, 6, 7, 2, 11 }, 4, bufferResult);
	}

	public void testImmediatePreceding() throws Exception {
		String sent = "(N(P(P(P(P(N A)(P B))(P C)(N D)(P N)(P E)))(N F)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 20, 0, "N", "P");
		joinAndAssertOutput(16, 50, jb, prev, Operator.IMMEDIATE_PRECEDING,
				posEnum);
		assertPositions(new int[] { 0, 6, 2, 11, 0, 6, 3, 9, 2, 3, 4, 8, 5, 6,
				4, 8 }, 12, bufferResult);
	}
	
	public void testDescendantWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 40, 0, "A", "A");
		joinAndAssertOutput(36, 20, jb, prev, Operator.DESCENDANT,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6, 0, 1, 2, 4, 0, 1, 3, 1, 1, 2, 2, 4, 1, 2, 3, 2, 2, 3, 2, 4, 2, 3, 3, 3, 3, 4, 1, 6, 3, 4, 2, 5 }, 32, bufferResult);
	}
	
	public void testAncestorWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 40, 0, "A", "A");
		joinAndAssertOutput(24, 36, jb, prev, Operator.ANCESTOR,
				posEnum);
		assertPositions(new int[] { 0, 4, 0, 0, 0, 3, 1, 6, 0, 1, 2, 4, 1, 2, 2, 4, 2, 3, 2, 4, 3, 4, 1 ,6 }, 20, bufferResult);		
	}
	
	public void testChildWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(B A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(28, 53, jb, prev, Operator.CHILD,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6, 0, 1, 2, 4, 0, 1, 3, 1, 1, 2, 2, 4, 1, 2, 3, 2, 3, 4, 1, 6, 3, 4, 2, 5 }, 24, bufferResult);
	}
	
	public void testParentWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(B A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(20, 101, jb, prev, Operator.PARENT,
				posEnum);
		assertPositions(new int[] { 0, 4, 0, 0, 0, 3, 1, 6, 0, 1, 2, 4, 1, 2, 2, 4, 3, 4, 1 ,6 }, 16, bufferResult);
	}
	
	public void testFollowingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 40, 0, "A", "A");
		joinAndAssertOutput(24, 14, jb, prev, Operator.FOLLOWING,
				posEnum);
		assertPositions(new int[] { 1, 2, 2, 4, 1, 2, 3, 2, 2, 3, 2, 4, 2, 3, 3, 3, 3, 4, 1, 6, 3, 4, 2, 5 }, 20, bufferResult);
	}
	
	public void testPrecedingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(A A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 40, 0, "A", "A");
		joinAndAssertOutput(28, 10, jb, prev, Operator.PRECEDING,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6, 0, 1, 2, 4, 0, 1, 3, 1, 1, 2, 2, 4, 1, 2, 3, 2, 2, 3, 2, 4, 2, 3, 3, 3 }, 24, bufferResult);
	}
	
	public void testFollowingSiblingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(8, 99, jb, prev, Operator.FOLLOWING_SIBLING,
				posEnum);
		assertPositions(new int[] { 2, 3, 2, 4, 3, 4, 1, 6 }, 4, bufferResult);	
	}
	
	public void testPrecedingSiblingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(8, 123, jb, prev, Operator.PRECEDING_SIBLING,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6, 0, 1, 2, 4 }, 4, bufferResult);	
	}
	
	public void testImmediatelyFollowingSiblingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(4, 102, jb, prev, Operator.IMMEDIATE_FOLLOWING_SIBLING,
				posEnum);
		assertPositions(new int[] { 3, 4, 1, 6 }, 0, bufferResult);	
	}
	
	public void testImmediatelyPrecedingSiblingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(4, 125, jb, prev, Operator.IMMEDIATE_PRECEDING_SIBLING,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6 }, 0, bufferResult);	
	}
	
	public void testImmediatelyFollowingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(20, 52, jb, prev, Operator.IMMEDIATE_FOLLOWING,
				posEnum);
		assertPositions(new int[] { 1, 2, 3, 2, 2, 3, 2, 4, 2, 3, 3, 3, 3, 4, 1, 6, 3, 4, 2, 5 }, 16, bufferResult);	
	}
	
	public void testImmediatelyPrecedingWithSameNode() throws Exception {
		String sent = "(A(A(A A)(B A)(A A))(A A))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 36, 0, "A", "A");
		joinAndAssertOutput(24, 70, jb, prev, Operator.IMMEDIATE_PRECEDING,
				posEnum);
		assertPositions(new int[] { 0, 3, 1, 6, 0, 1, 2, 4, 0, 1, 3, 1, 1, 2, 3, 2, 2, 3, 2, 4, 2, 3, 3, 3 }, 20, bufferResult);	
	}
	
	StaircaseJoin getJoin(Operator op) {
		return (StaircaseJoin) StaircaseJoin.JOIN_BUILDER.getHalfPairJoin(op, lrdp);
	}
}
