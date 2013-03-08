package au.edu.unimelb.csse.join;

import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import au.edu.unimelb.csse.BinaryOperator;

public class PathStackJoinTest extends HolisticJoinTestCase {

	public void testLeafPositions() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC",
				"DD", "EE" }, getDescOp(5), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA CC)(AA CC)(AA CC))")); // doc 0
		w.addDocument(getDoc("(AA(BB CC)(DD EE)(AA BB))")); // doc 1
		w.addDocument(getDoc("(AA(BB(CC(DD EE))))")); // doc 2
		w.addDocument(getDoc("(AA(KK PP)(FF JJ))")); // doc 3
		w.addDocument(getDoc("(AA(AA BB))")); // doc 4
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		// postings freqs are reverse sorted by docFreq and position
		// AA:5 BB:3 CC:3 DD:2 EE:2

		assertPfArrayPos(new String[] { "DD", "EE", "BB", "CC", "AA" },
				ps.postingsFreqs);

		int leafCount = 0;
		for (int i = 0; i < ps.postingsFreqs.length; i++) {
			if (ps.postingsFreqs[i].isLeaf) {
				leafCount++;
			}
		}
		assertEquals(1, leafCount);
		assertTrue("EE should be a leaf", ps.postingsFreqs[1].isLeaf); // EE
	}

	public void testSetupPerDocInitsVariables() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC",
				"DD", "EE" }, getDescOp(5), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA CC)(AA CC)(AA CC))")); // doc 0
		w.addDocument(getDoc("(AA(BB CC)(DD EE)(AA BB))")); // doc 1
		w.addDocument(getDoc("(AA(BB CC)(DD EE))")); // doc 2
		w.addDocument(getDoc("(AA(KK PP)(FF JJ))")); // doc 3
		w.addDocument(getDoc("(AA(AA BB))")); // doc 4
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		assertPfArrayPos(new String[] { "DD", "EE", "BB", "CC", "AA" },
				ps.postingsFreqs);
		int doc = ps.nextDoc();
		assertEquals(1, doc);
		// ps.setupPerDoc(); called automatically

		// assert PreorderPos
		// DD:3 EE:4 BB:1 CC:2 1stAA:0
		assertIntArray(new int[] { 3, 4, 1, 2, 0 }, ps.preorderPos);

		// assert Freq
		// DD:1 EE:1 BB:2 CC:1 AA:2
		assertIntArray(new int[] { 1, 1, 2, 1, 2 }, ps.freqs);

		// assert Payload
		assertIntArray(new int[] { 1, 2, 1, 4, 1, 2, 2, 2, 0, 1, 1, 4, 0, 1, 2,
				1, 0, 3, 0, 0 }, ps.positions);

		// assert NextPosCalledCount
		assertIntArray(new int[] { 1, 1, 1, 1, 1 }, ps.nextPosCalledCount);

		// next Doc
		doc = ps.nextDoc();
		assertEquals(2, doc);
		// ps.setupPerDoc(); called automatically

		// assert PreorderPos
		// DD:3 EE:4 BB:1 CC:2 1stAA:0
		assertIntArray(new int[] { 3, 4, 1, 2, 0 }, ps.preorderPos);

		// assert Freq
		// DD:1 EE:1 BB:2 CC:1 AA:2
		assertIntArray(new int[] { 1, 1, 1, 1, 1 }, ps.freqs);

		// assert Payload
		assertIntArray(new int[] { 1, 2, 1, 3, 1, 2, 2, 2, 0, 1, 1, 3, 0, 1, 2,
				1, 0, 2, 0, 0 }, ps.positions);

		// assert NextPosCalledCount
		assertIntArray(new int[] { 1, 1, 1, 1, 1 }, ps.nextPosCalledCount);
	}

	public void testGetMinSource() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC",
				"DD", "EE" }, getDescOp(5), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA CC)(AA CC)(AA CC))")); // doc 0
		w.addDocument(getDoc("(KK(DD EE)(AA(BB CC)(DD EE)))")); // doc 1
		w.addDocument(getDoc("(AA(BB(CC(DD EE))))")); // doc 2
		w.addDocument(getDoc("(AA(AA BB))")); // doc 4
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		assertPfArrayPos(new String[] { "DD", "EE", "BB", "CC", "AA" },
				ps.postingsFreqs);
		int doc = ps.nextDoc();
		assertEquals(1, doc);
		// ps.setupPerDoc(); called automatically

		int minSourcePos = ps.getMinSource();
		// 0 represents DD's postingsFreq position
		assertEquals(0, minSourcePos);

		doc = ps.nextDoc();
		assertEquals(2, doc);
		// ps.setupPerDoc(); called automatically

		minSourcePos = ps.getMinSource();
		// 4 represents AA's postingsFreq position
		assertEquals(4, minSourcePos);
	}

	public void testGetMinSourceWhenSameTermsPresentInPath() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "CC", "AA",
				"AA" }, getDescOp(4), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA CC)(AA CC)(AA CC))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		assertPfArrayPos(new String[] { "AA", "CC", "AA", "AA" },
				ps.postingsFreqs);
		int doc = ps.nextDoc();
		assertEquals(0, doc);

		// ps.setupPerDoc(); called automatically

		int minSourcePos = ps.getMinSource();
		// 3rd AA is chosen; ie. postingsFreq[3]
		assertEquals(3, minSourcePos);
	}

	public void testStacksAreClearedOfPrecedingEntries() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC" },
				getDescOp(3), lrdp);

		// setting up stacks
		System.arraycopy(new int[] { 1, 7, 1, 18, -1 }, 0,
				ps.positionStacks[0], 0, 5);
		System.arraycopy(new int[] { 2, 6, 4, 9, 0 }, 0, ps.positionStacks[1],
				0, 5);
		System.arraycopy(new int[] { 3, 4, 5, 7, 0 }, 0, ps.positionStacks[2],
				0, 5);
		System.arraycopy(new int[] { 1, 1, 1 }, 0, ps.positionStacksSizes, 0, 3);

		ps.clearPrecedingStackEntries(new int[] { 0, 0, 0, 0, 5, 6, 5, 8 }, 4);

		assertIntArray(new int[] { 1, 1, 0 }, ps.positionStacksSizes);
	}

	public void testSimpleMatch() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "CC", "DD" },
				getDescOp(3), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB(CC(DD EE)))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		// ps.setupPerDoc(); called automatically
		List<int[]> matches = ps.match();
		assertEquals(2, matches.size());
		assertIntArray(new int[] { 0, 1, 1, 5, 0, 1, 3, 3, 0, 1, 4, 2 },
				matches.get(0));
		assertIntArray(new int[] { 0, 1, 2, 4, 0, 1, 3, 3, 0, 1, 4, 2 },
				matches.get(1));
	}

	public void testNoMatch() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "CC", "DD" },
				getDescOp(3), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(DD(DD(CC(BB EE)))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		List<int[]> matches = ps.match();
		assertNull(matches);
	}

	public void testRepeatMatch() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "BB" },
				getDescOp(2), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB(CC(DD EE)))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		List<int[]> matches = ps.match();
		assertEquals(1, matches.size());
		assertIntArray(new int[] { 0, 1, 1, 5, 0, 1, 2, 4 }, matches.get(0));
	}

	public void testIdenticalLabelNestedWithDescOp() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "BB" },
				getDescOp(2), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB(BB(BB(CC(DD EE)))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		List<int[]> matches = ps.match();
		assertEquals(6, matches.size());
		assertIntArray(new int[] { 0, 1, 1, 7, 0, 1, 2, 6 }, matches.get(0));
		assertIntArray(new int[] { 0, 1, 1, 7, 0, 1, 3, 5 }, matches.get(1));
		assertIntArray(new int[] { 0, 1, 2, 6, 0, 1, 3, 5 }, matches.get(2));
		assertIntArray(new int[] { 0, 1, 1, 7, 0, 1, 4, 4 }, matches.get(3));
		assertIntArray(new int[] { 0, 1, 2, 6, 0, 1, 4, 4 }, matches.get(4));
		assertIntArray(new int[] { 0, 1, 3, 5, 0, 1, 4, 4 }, matches.get(5));
	}

	public void testIdenticalLabelNestedWithChildOp() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "BB" },
				new BinaryOperator[] { BinaryOperator.DESCENDANT, BinaryOperator.CHILD }, lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB(BB(BB(CC(DD EE)))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		List<int[]> matches = ps.match();
		assertEquals(3, matches.size());
		assertIntArray(new int[] { 0, 1, 1, 7, 0, 1, 2, 6 }, matches.get(0));
		assertIntArray(new int[] { 0, 1, 2, 6, 0, 1, 3, 5 }, matches.get(1));
		assertIntArray(new int[] { 0, 1, 3, 5, 0, 1, 4, 4 }, matches.get(2));
	}

	public void testRootChildOpQuery() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "BB" },
				new BinaryOperator[] { BinaryOperator.CHILD, BinaryOperator.CHILD }, lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(BB(BB(BB(BB(BB(CC(DD EE)))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
		List<int[]> matches = ps.match();
		assertEquals(1, matches.size());
		assertIntArray(new int[] { 0, 1, 0, 0, 0, 1, 1, 7 }, matches.get(0));
	}

}
