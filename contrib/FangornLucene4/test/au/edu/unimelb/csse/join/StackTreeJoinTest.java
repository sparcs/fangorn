package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.CountingOp;
import au.edu.unimelb.csse.IndexTestCase;

public class StackTreeJoinTest extends IndexTestCase {

	public void testNoDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(SS(AA DD)(AA DD)(NA ND))")); // doc 0
		w.addDocument(getDoc("(SS(NA ND)(AA DD)(AA DD))")); // doc 1
		IndexReader r = commitIndexAndOpenReader(w);
		StackTreeJoin join = new StackTreeJoin();
		for (int doc = 0; doc < 2; doc++) {
			DocsAndPositionsEnum posEnum = getPosEnum(r, doc, new Term("s",
					"AA"));
			int[] prev = join.getAllPositions(posEnum);
			assertEquals(8, prev.length);
			posEnum = getPosEnum(r, doc, new Term("s", "ND"));
			long startCount = CountingOp.DESCENDANT.getCount();
			int[] joinOut = join.join(prev, CountingOp.DESCENDANT, posEnum);
			assertEquals(0, joinOut.length); // 0 pairs
			long endCount = CountingOp.DESCENDANT.getCount();
			assertEquals(0, endCount - startCount); // doesnt count all comps
		}
	}

	public void testNestedTreeDesc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		StackTreeJoin join = new StackTreeJoin();
		int[] prev = join.getAllPositions(posEnum);
		assertEquals(16, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.DESCENDANT.getCount();
		int[] joinOut = join.join(prev, CountingOp.DESCENDANT, posEnum);
		assertEquals(48, joinOut.length); // 6 pairs
		long endCount = CountingOp.DESCENDANT.getCount();
		assertEquals(3, endCount - startCount); // count doesnt include all
												// comparisons
	}

	public void testNestedTreeChild() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		StackTreeJoin join = new StackTreeJoin();
		int[] prev = join.getAllPositions(posEnum);
		assertEquals(16, prev.length);
		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		long startCount = CountingOp.CHILD.getCount();
		int[] joinOut = join.join(prev, CountingOp.CHILD, posEnum);
		assertEquals(24, joinOut.length); // 3 pairs
		long endCount = CountingOp.CHILD.getCount();
		assertEquals(3, endCount - startCount);
		// count doesnt include all comparisons
	}
}
