package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.CountingOp;
import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;

public class StaircaseJoinTest extends IndexTestCase {
	public void testPruneReturnsSamePrevWhenLengthIsLessThan4()
			throws Exception {
		StaircaseJoin j = new StaircaseJoin();
		// the zero case should ideally never happen
		Operator[] ops = new Operator[] { Op.DESCENDANT, Op.CHILD, Op.ANCESTOR,
				Op.PARENT, Op.FOLLOWING, Op.PRECEDING };
		for (Operator op : ops) {
			int[] pruned = j.prune(new int[] {}, op);
			assertEquals(0, pruned.length);
			pruned = j.prune(new int[] { 1, 2, 3, 4 }, op);
			assertIntArray(new int[] { 1, 2, 3, 4 }, pruned);
		}
	}

	public void testPushToStackStoresTreePositionAndPrevIndex()
			throws Exception {
		StaircaseJoin j = new StaircaseJoin();

		int[] stack = new int[10];
		stack[0] = 1;
		stack[1] = 4;
		stack[2] = 2;
		stack[3] = 12;
		stack[4] = 0;
		int stackSize = 1;

		int[] prev = new int[] { 1, 4, 2, 12, 2, 3, 3, 4 };
		int pIdx = 1;

		j.pushToStack(prev, pIdx, stack, stackSize);
		assertIntArray(new int[] { 1, 4, 2, 12, 0, 2, 3, 3, 4, 1 }, stack);

		// when stack is empty
		stack = new int[5];
		stackSize = 0;
		j.pushToStack(prev, pIdx, stack, stackSize);
		assertIntArray(new int[] { 2, 3, 3, 4, 1 }, stack);
	}

	public void testDescendantOpPruning() throws Exception {
		StaircaseJoin j = new StaircaseJoin();
		Operator o = Op.DESCENDANT;
		int[] pruned = j.prune(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2,
				28, 7, 9, 5, 22 }, o); // prune two leave two
		assertIntArray(new int[] { 1, 4, 2, 12, 5, 9, 2, 28 }, pruned);

		pruned = j.prune(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, o); // no pruning
		assertIntArray(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, pruned);

		pruned = j.prune(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 }, o); // prune all but 1
		assertIntArray(new int[] { 1, 17, 2, 65 }, pruned);
	}

	public void testAncestorOpPruning() throws Exception {
		StaircaseJoin j = new StaircaseJoin();
		Operator o = Op.ANCESTOR;
		int[] pruned = j.prune(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2,
				28, 7, 9, 5, 22 }, o); // prune two leave two
		assertIntArray(new int[] { 2, 3, 3, 4, 7, 9, 5, 22 }, pruned);

		pruned = j.prune(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, o); // no pruning
		assertIntArray(new int[] { 1, 4, 2, 12, 4, 5, 2, 21, 5, 9, 2, 28, 9,
				13, 5, 38 }, pruned);

		pruned = j.prune(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 5, 9, 4, 28, 9,
				13, 5, 38 }, o); // prune one
		assertIntArray(new int[] { 4, 5, 3, 21, 5, 9, 4, 28, 9, 13, 5, 38 },
				pruned);

		pruned = j.prune(new int[] { 1, 17, 2, 65, 4, 5, 3, 21, 4, 5, 4, 20 },
				o); // retain one (nested pruning)
		assertIntArray(new int[] { 4, 5, 4, 20 }, pruned);
	}

	public void testFollowingOpPruning() throws Exception {
		StaircaseJoin j = new StaircaseJoin();
		Operator o = Op.FOLLOWING;

		int[] pruned = j.prune(
				new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 }, o); // retain
																		// first
		assertIntArray(new int[] { 2, 3, 3, 4 }, pruned);

		pruned = j.prune(new int[] { 1, 4, 2, 12, 2, 3, 3, 4, 5, 9, 2, 28, 7,
				9, 5, 22 }, o); // find descendant and stop
		assertIntArray(new int[] { 2, 3, 3, 4 }, pruned);

		pruned = j.prune(new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 }, o); // find last descendant
		assertIntArray(new int[] { 4, 5, 6, 18 }, pruned);
	}

	public void testPrecedingOpPruning() throws Exception {
		StaircaseJoin j = new StaircaseJoin();

		Operator o = Op.PRECEDING;

		int[] pruned = j.prune(
				new int[] { 2, 3, 3, 4, 5, 9, 2, 28, 7, 9, 5, 22 }, o); // retain
																		// last
		assertIntArray(new int[] { 7, 9, 5, 22 }, pruned);

		pruned = j.prune(new int[] { 1, 17, 2, 65, 4, 7, 3, 28, 4, 6, 4, 20, 4,
				5, 6, 18 }, o);
		assertIntArray(new int[] { 4, 5, 6, 18 }, pruned);
	}

	public void testFollowingOpWithSimpleFollows() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		long startCount = CountingOp.FOLLOWING.getCount();
		int[] joinOut = join.join(prev, CountingOp.FOLLOWING, posEnum);
		assertEquals(8, joinOut.length); // 2 FFs
		long endCount = CountingOp.FOLLOWING.getCount();
		assertEquals(2, endCount - startCount);
	}

	public void testFollowingOpWithOnePrecedingIgnored() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA(FF WW))(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		long startCount = CountingOp.FOLLOWING.getCount();
		int[] joinOut = join.join(prev, CountingOp.FOLLOWING, posEnum);
		assertEquals(8, joinOut.length); // 2 FFs
		long endCount = CountingOp.FOLLOWING.getCount();
		assertEquals(3, endCount - startCount);
	}

	public void testFollowingOpWithNoneFollowing() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA(FF WW))(PP(PP WW))(NN(JJ WW))(ZZ(JJ WW)))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		long startCount = CountingOp.FOLLOWING.getCount();
		int[] joinOut = join.join(prev, CountingOp.FOLLOWING, posEnum);
		assertEquals(0, joinOut.length); // 0 FFs
		long endCount = CountingOp.FOLLOWING.getCount();
		assertEquals(1, endCount - startCount);
	}

	public void testPrecedingOpSimple() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(PP(PP WW))(NN(FF WW))(ZZ(FF WW)))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		long startCount = CountingOp.PRECEDING.getCount();
		int[] joinOut = join.join(prev, CountingOp.PRECEDING, posEnum);
		assertEquals(8, joinOut.length); // 2 PPs
		long endCount = CountingOp.PRECEDING.getCount();
		assertEquals(2, endCount - startCount);
	}

	public void testPrecedingOpWithNonePreceding() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(NN(FF WW))(ZZ(FF WW))(PP(PP WW)))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		long startCount = CountingOp.PRECEDING.getCount();
		int[] joinOut = join.join(prev, CountingOp.PRECEDING, posEnum);
		assertEquals(0, joinOut.length); // 0 PPs
		long endCount = CountingOp.PRECEDING.getCount();
		assertEquals(2, endCount - startCount);
	}

	public void testPrecedingOpWithOneIgnored() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(PP(PP WW)(FF WW))(ZZ(FF WW))(PP WW))")); 																			
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "FF"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		long startCount = CountingOp.PRECEDING.getCount();
		int[] joinOut = join.join(prev, CountingOp.PRECEDING, posEnum);
		assertEquals(8, joinOut.length); // 2 PPs
		long endCount = CountingOp.PRECEDING.getCount();
		assertEquals(3, endCount - startCount);
	}

	public void testDescendantOpWithMixedPositions() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))")); // doc
																									// 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = join.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(8, joinOut.length); // 2 DDs
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(5, endCount - startCount);
	}

	public void testChildOpWithMixedPositions() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS" + "(AA(PP WW)(AA(CC WW))(CC WW))"
				+ "(AA(PP WW)(AA(DD(CC WW)))(CC WW))" + "(ZZ(CC WW))"
				+ "(AA(FF WW))" + "(AA(CC(AA(DD(CC WW))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "CC"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = join.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(16, joinOut.length); // 4 CCs
		assertIntArray(new int[] { 1, 2, 3, 3, 2, 3, 2, 5, 5, 6, 2, 11, 8, 9,
				2, 20 }, joinOut);
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(16, endCount - startCount);
	}
	
	public void testAncestorOpWithMixedPositions() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD WW))(AA(FF WW))(AA(DD(AA WW))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		long startCount = CountingOp.ANCESTOR.getCount();
		int[] joinOut = join.join(prev, CountingOp.ANCESTOR, posEnum);
		assertEquals(12, joinOut.length); // 3 AAs
		long endCount = CountingOp.ANCESTOR.getCount();
		assertEquals(7, endCount - startCount);
	}
	
	public void testAncestorOpWithNestedDescendants() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA(PP WW)(AA(DD WW)))(ZZ(DD ww))(AA(FF WW))(AA(DD(AA WW))))")); 
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		long startCount = CountingOp.ANCESTOR.getCount();
		int[] joinOut = join.join(prev, CountingOp.ANCESTOR, posEnum);
		assertEquals(12, joinOut.length); // 3 AAs
		long endCount = CountingOp.ANCESTOR.getCount();
		assertEquals(7, endCount - startCount);
	}
	
	public void testChildOp() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))")); 
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "CC"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = join.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(8, joinOut.length); // 2 CCs
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(7, endCount - startCount);
	}
	
	public void testParentOp() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(CC WW)(PP(CC WW)(JJ WW))(PP(QQ(CC WW))(PP(CC WW)(ZZ WW))))")); 
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "CC"));
		StaircaseJoin join = new StaircaseJoin();
		int[] prev = join.getAllPositions(posEnum);

		posEnum = getPosEnum(r, 0, new Term("s", "PP"));
		long startCount = CountingOp.PARENT.getCount();
		int[] joinOut = join.join(prev, CountingOp.PARENT, posEnum);
		assertEquals(8, joinOut.length); // 2 PPs
		long endCount = CountingOp.PARENT.getCount();
		assertEquals(6, endCount - startCount);
	}
	

}
