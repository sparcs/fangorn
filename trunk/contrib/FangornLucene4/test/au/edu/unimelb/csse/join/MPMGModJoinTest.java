package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.junit.Test;

import au.edu.unimelb.csse.CountingOp;
import au.edu.unimelb.csse.IndexTestCase;

public class MPMGModJoinTest extends IndexTestCase {
	public void testSkipsPrevAAsStopsAtNextAA() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin join = new MPMGModJoin();
		int[] prev = join.getAllPositions(posEnum);
		assertEquals(12, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = join.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(24, joinOut.length); // 3 pairs
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(5, endCount - startCount); // this does not include all comparisons
	}

	// the next few tests compare MPMGMod with vanilla MPMG join

	@Test
	public void testNoResultsOneMatchDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(DD(BB AA)(BB AA))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(0, joinOut.length);
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(2, endCount - startCount); // was 1 in MPMG
	}

	@Test
	public void testNoResultsOneMatchChild() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(DD(BB AA)(BB AA))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(0, joinOut.length);
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(2, endCount - startCount); // was 1 in MPMG
	}

	@Test
	public void testOneResultThreeMatchesDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(DD(AA DD)(AA CC)(AA CC))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		assertEquals(12, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(8, joinOut.length); // one pair
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(5, endCount - startCount);
	}

	@Test
	public void testTwoExtraMatchesDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		assertEquals(8, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(48, joinOut.length); // 6 pairs
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(8, endCount - startCount);
	}

	@Test
	public void testTwoExtraMatchesChild() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		assertEquals(8, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(0, joinOut.length); // 0 pairs
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(8, endCount - startCount);
	}

	@Test
	public void testTenExtraMatchesDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		assertEquals(16, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(48, joinOut.length); // 6 pairs
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(11, endCount - startCount); // was 10 in MPMG
	}

	@Test
	public void testSevenExtraMatchesChild() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		MPMGModJoin mpmgJoin1 = new MPMGModJoin();
		int[] prev = mpmgJoin1.getAllPositions(posEnum);
		assertEquals(16, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = mpmgJoin1.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(24, joinOut.length); // 3 pairs
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(11, endCount - startCount); // was 10 in MPMG
	}
}
