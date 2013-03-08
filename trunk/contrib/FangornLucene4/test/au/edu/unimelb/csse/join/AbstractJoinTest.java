package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.DocIdSetIterator;

import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;

public class AbstractJoinTest extends HolisticJoinTestCase {
	
	private class AbstractJoinTestStub extends AbstractJoin {

		public AbstractJoinTestStub(String[] labels) {
			super(labels, new int[] { -1, 0, 1 }, getDescOp(3));
		}

		public int getCurrentContextPos() {
			return currentContextPos;
		}

		public int getAtomicContextsCount() {
			return atomicContextsCount;
		}

		public PostingsAndFreq[] getPostingsAndFreqs() {
			return postingsFreqs;
		}

		@Override
		public void setupPerAtomicContext() {
			// do nothing 
		}

		@Override
		public void setupPerDoc() throws IOException {
			// do nothing
		}

	}

	public void testSetupInitsVariables() throws Exception {
		AbstractJoinTestStub aj = new AbstractJoinTestStub(new String[] { "AA",
				"BB", "DD" });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA BB)(AA DD))"));
		w.addDocument(getDoc("(AA(AA BB)(AA BB))"));
		w.addDocument(getDoc("(AA(AA DD))"));
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))"));
		w.addDocument(getDoc("(AA(AA JJ)(AA KK)(AA LL))"));
		IndexReader r = commitIndexAndOpenReader(w);
		boolean setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);

		assertEquals(0, aj.getCurrentContextPos());
		assertEquals(1, aj.getAtomicContextsCount());

		PostingsAndFreq[] postingsFreqs = aj.getPostingsAndFreqs();
		assertEquals(3, postingsFreqs.length);

		// postingsFreqs should be ordered by doc freq
		assertEquals("BB", postingsFreqs[0].term.text());
		assertEquals("DD", postingsFreqs[1].term.text());
		assertEquals("AA", postingsFreqs[2].term.text());
	}

	public void testDocFreqTiePostingsAndFreqSorting() throws Exception {
		AbstractJoinTestStub aj = new AbstractJoinTestStub(new String[] { "AA",
				"BB", "DD" });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA BB)(AA DD))"));
		w.addDocument(getDoc("(AA(AA BB)(AA BB))"));
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))"));
		IndexReader r = commitIndexAndOpenReader(w);
		boolean setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);

		PostingsAndFreq[] postingsFreqs = aj.getPostingsAndFreqs();
		assertEquals(3, postingsFreqs.length);
		assertEquals("BB", postingsFreqs[0].term.text());
		assertEquals("DD", postingsFreqs[1].term.text());
		assertEquals("AA", postingsFreqs[2].term.text());

		aj = new AbstractJoinTestStub(new String[] { "DD", "AA", "BB" });
		setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);

		postingsFreqs = aj.getPostingsAndFreqs();
		assertEquals(3, postingsFreqs.length);
		assertEquals("DD", postingsFreqs[0].term.text());
		assertEquals("BB", postingsFreqs[1].term.text());
		assertEquals("AA", postingsFreqs[2].term.text());
	}

	public void testNextDocAtomicContext() throws Exception {
		AbstractJoin aj = new AbstractJoinTestStub(new String[] { "AA", "BB",
				"DD" });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0; some terms
															// in query
		w.addDocument(getDoc("(AA(AA BB)(AA BB)(AA BB))")); // doc 1; some terms
															// in query
		w.addDocument(getDoc("(BB(BB DD)(KK DD)(BB DD))")); // doc 2; some terms
															// in query
		w.addDocument(getDoc("(KK(KK PP)(FF JJ))")); // doc 3; no term in query
		w.addDocument(getDoc("(AA(AA DD)(AA BB)(AA DD))")); // doc 4; all terms
															// in query
		w.addDocument(getDoc("(KK(KK QQ)(FF JJ))")); // doc 5; no term in query
		w.addDocument(getDoc("(AA(AA BB)(AA BB)(AA DD))")); // doc 6; all terms
		w.addDocument(getDoc("(KK(KK MM)(NN JJ))")); // doc 7; no term in query

		IndexReader r = commitIndexAndOpenReader(w);
		boolean setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);

		int doc = aj.nextDocAtomicContext();
		assertEquals(4, doc);

		doc = aj.nextDocAtomicContext();
		assertEquals(6, doc);

		doc = aj.nextDocAtomicContext();
		assertEquals(DocIdSetIterator.NO_MORE_DOCS, doc);
	}

	public void testNextDocAtomicContextWhenFirstDocMatches() throws Exception {
		AbstractJoin aj = new AbstractJoinTestStub(new String[] { "AA", "BB",
				"DD" });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA BB)(AA DD))")); // all terms
		IndexReader r = commitIndexAndOpenReader(w);
		boolean setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);

		int doc = aj.nextDocAtomicContext();
		assertEquals(0, doc);

		doc = aj.nextDocAtomicContext();
		assertEquals(DocIdSetIterator.NO_MORE_DOCS, doc);
	}

	public void testMaxReached() throws Exception {
		AbstractJoin aj = new AbstractJoinTestStub(new String[] { "AA", "BB",
				"DD" });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA BB)(AA DD))")); // all terms
		IndexReader r = commitIndexAndOpenReader(w);
		boolean setupSuccess = aj.setup(r);
		assertTrue(setupSuccess);
		int doc = aj.nextDocAtomicContext();
		assertEquals(0, doc);
		doc = aj.nextDocAtomicContext();
		assertEquals(DocIdSetIterator.NO_MORE_DOCS, doc);
		assertTrue(aj.isAtLastContext());
	}

	public void testIteratesOverMultipleAtomicContexts() throws Exception {
		AbstractJoin aj = new AbstractJoinTestStub(new String[] { "AA", "BB",
				"DD" });
		IndexWriter w = setupIndex();
		for (int i = 0; i < 250000; i++) {
			w.addDocument(getDoc("(AA(BB DD))"));
			w.addDocument(getDoc("(KK(LL MM)(NN OO)(PP QQ))"));
			w.addDocument(getDoc("(RR(SS TT)(ZZ(UU VV))(WW YY))"));
		}
		IndexReader r = commitIndexAndOpenReader(w);
		assertTrue(r.getContext().leaves().size() > 1);
		aj.setup(r);
		int expectedAtomicContextNum = 0;
		int doc;
		for (int i = 0; i < 250000; i++) {
			doc = aj.nextDocAtomicContext();
			if (doc == DocIdSetIterator.NO_MORE_DOCS) {
				aj.initAtomicContextPositionsFreqs();
				i--;
				expectedAtomicContextNum++;
				continue;
			}
			assertEquals("expected i " + (i + 1), expectedAtomicContextNum, aj.currentContextPos);
		}
		doc = aj.nextDocAtomicContext();
		assertEquals(DocIdSetIterator.NO_MORE_DOCS, doc);
	}
	
	public void testNextDocSeamlesslyIteratesOverMultipleAtomicContexts() throws Exception {
		AbstractJoin aj = new AbstractJoinTestStub(new String[] { "AA", "BB",
				"DD" });
		IndexWriter w = setupIndex();
		for (int i = 0; i < 250000; i++) {
			w.addDocument(getDoc("(AA(BB DD))"));
			w.addDocument(getDoc("(KK(LL MM)(NN OO)(PP QQ))"));
			w.addDocument(getDoc("(RR(SS TT)(ZZ(UU VV))(WW YY))"));
		}
		IndexReader r = commitIndexAndOpenReader(w);
		assertTrue(r.getContext().leaves().size() > 1);
		aj.setup(r);
		assertFalse("Should not be at last context on start", aj.isAtLastContext());
		int doc;
		for (int i = 0; i < 250000; i++) {
			doc = aj.nextDoc();
			assertTrue("Expected 250000 docs with reqd terms but found " + (i + 1), doc != DocIdSetIterator.NO_MORE_DOCS);
		}
		doc = aj.nextDocAtomicContext();
		assertEquals(DocIdSetIterator.NO_MORE_DOCS, doc);
	}
	
	public void testGetQueryRoot() throws Exception {
		AbstractJoin ts = new TwigStackJoin(new String[] { "A", "B", "C",
				"D", "E" }, new int[] { -1, 0, 1, 0, 4 }, getDescOp(5), lrdp);
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		
		// call to setup or nextDoc is required to find the queryRoot
		PostingsAndFreq queryRoot = ts.root;
		assertEquals("A", queryRoot.term.text());
	}

}
