package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.AbstractPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.GetAllPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.GetRootNodePipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.MetaPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.Pipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.SimplePipe;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class HalfPairJoinPipelineTest extends IndexTestCase {
	private LogicalNodePositionAware npa = new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111);

	public void testReturnsSingleGetAllPipeForDescendant1stOp()
			throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B C))");
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				new StaircaseJoin(npa));
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		PostingsAndFreq pfRoot = getPf(aPosEnum, 0);
		Pipe p = pipeline.createExecPipeline(pfRoot,
				new Operator[] { Operator.DESCENDANT });
		assertGetAllPipe(aPosEnum, false, p);
	}

	public void testReturnsSingleGetRootPipeForChild1stOp() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B C))");
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				new StaircaseJoin(npa));
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		PostingsAndFreq pfRoot = getPf(aPosEnum, 0);
		Pipe p = pipeline.createExecPipeline(pfRoot,
				new Operator[] { Operator.CHILD });
		assertTrue(p instanceof GetRootNodePipe);
		assertNull(p.getNext());
	}

	public void testReturnsSimplePipelineConstruction() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B C))");
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				new StaircaseJoin(npa));
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(r, 0, new Term("s", "C"));

		PostingsAndFreq childpf = getPf(cPosEnum, 1);
		PostingsAndFreq pfRoot = getPf(aPosEnum, 0, childpf);

		Pipe p = pipeline.createExecPipeline(pfRoot, new Operator[] {
				Operator.DESCENDANT, Operator.DESCENDANT });
		assertTrue(p instanceof GetAllPipe);
		assertEquals(aPosEnum, ((AbstractPipe) p).node);
		Pipe next = p.getNext();
		assertSimplePipe(cPosEnum, Operator.DESCENDANT, next, false);

		p = pipeline.createExecPipeline(pfRoot, new Operator[] {
				Operator.CHILD, Operator.CHILD });
		assertTrue(p instanceof GetRootNodePipe);
		assertEquals(aPosEnum, ((AbstractPipe) p).node);
		next = p.getNext();
		assertSimplePipe(cPosEnum, Operator.CHILD, next, false);
	}

	public void testTopLevelBranch() throws Exception {
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				new StaircaseJoin(npa));
		IndexReader r = setupIndexWithDocs("(A(B C)(D E))");
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(r, 0, new Term("s", "C"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(r, 0, new Term("s", "E"));

		PostingsAndFreq cpf = getPf(cPosEnum, 1);
		PostingsAndFreq epf = getPf(ePosEnum, 2);
		PostingsAndFreq apf = getPf(aPosEnum, 0, cpf, epf);

		Pipe a = pipeline
				.createExecPipeline(apf, new Operator[] {
						Operator.DESCENDANT, Operator.DESCENDANT,
						Operator.DESCENDANT });
		assertTrue(a instanceof GetAllPipe);
		assertEquals(aPosEnum, ((AbstractPipe) a).node);
		Pipe c = a.getNext();
		assertMetaPipe(Operator.ANCESTOR, true, c);
		Pipe ci = ((MetaPipe) c).getInner();
		assertGetAllPipe(cPosEnum, false, ci);

		Pipe e = c.getNext();
		assertMetaPipe(Operator.ANCESTOR, false, e);
		Pipe ei = ((MetaPipe) e).getInner();
		assertGetAllPipe(ePosEnum, false, ei);
	}

	public void testLongQueryConstruction() throws Exception {
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				new StaircaseJoin(npa));
		IndexReader rdr = setupIndexWithDocs("(A(B(C D)(E(F(G H)(I J))(K(L(M N))(O P))(Q R))))");
		DocsAndPositionsEnum aPosEnum = getPosEnum(rdr, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(rdr, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(rdr, 0, new Term("s", "C"));
		DocsAndPositionsEnum dPosEnum = getPosEnum(rdr, 0, new Term("s", "D"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(rdr, 0, new Term("s", "E"));
		DocsAndPositionsEnum fPosEnum = getPosEnum(rdr, 0, new Term("s", "F"));
		DocsAndPositionsEnum gPosEnum = getPosEnum(rdr, 0, new Term("s", "G"));
		DocsAndPositionsEnum hPosEnum = getPosEnum(rdr, 0, new Term("s", "H"));
		DocsAndPositionsEnum iPosEnum = getPosEnum(rdr, 0, new Term("s", "I"));
		DocsAndPositionsEnum jPosEnum = getPosEnum(rdr, 0, new Term("s", "J"));
		DocsAndPositionsEnum kPosEnum = getPosEnum(rdr, 0, new Term("s", "K"));
		DocsAndPositionsEnum lPosEnum = getPosEnum(rdr, 0, new Term("s", "L"));
		DocsAndPositionsEnum mPosEnum = getPosEnum(rdr, 0, new Term("s", "M"));
		DocsAndPositionsEnum nPosEnum = getPosEnum(rdr, 0, new Term("s", "N"));
		DocsAndPositionsEnum oPosEnum = getPosEnum(rdr, 0, new Term("s", "O"));
		DocsAndPositionsEnum pPosEnum = getPosEnum(rdr, 0, new Term("s", "P"));
		DocsAndPositionsEnum qPosEnum = getPosEnum(rdr, 0, new Term("s", "Q"));
		DocsAndPositionsEnum rPosEnum = getPosEnum(rdr, 0, new Term("s", "R"));

		PostingsAndFreq dpf = getPf(dPosEnum, 3);
		PostingsAndFreq cpf = getPf(cPosEnum, 2, dpf);

		PostingsAndFreq hpf = getPf(hPosEnum, 7);
		PostingsAndFreq gpf = getPf(gPosEnum, 6, hpf);

		PostingsAndFreq jpf = getPf(jPosEnum, 9);
		PostingsAndFreq ipf = getPf(iPosEnum, 8, jpf);

		PostingsAndFreq fpf = getPf(fPosEnum, 5, gpf, ipf);

		PostingsAndFreq npf = getPf(nPosEnum, 13);
		PostingsAndFreq mpf = getPf(mPosEnum, 12, npf);
		PostingsAndFreq lpf = getPf(lPosEnum, 11, mpf);

		PostingsAndFreq ppf = getPf(pPosEnum, 15);
		PostingsAndFreq opf = getPf(oPosEnum, 14, ppf);
		PostingsAndFreq kpf = getPf(kPosEnum, 10, lpf, opf);

		PostingsAndFreq rpf = getPf(rPosEnum, 17);
		PostingsAndFreq qpf = getPf(qPosEnum, 16, rpf);

		PostingsAndFreq epf = getPf(ePosEnum, 4, fpf, kpf, qpf);
		PostingsAndFreq bpf = getPf(bPosEnum, 1, cpf, epf);
		PostingsAndFreq apf = getPf(aPosEnum, 0, bpf);

		Pipe a = pipeline.createExecPipeline(apf, new Operator[] {
				Operator.DESCENDANT, // A
				Operator.DESCENDANT, // B
				Operator.CHILD, // C
				Operator.FOLLOWING, // D
				Operator.IMMEDIATE_FOLLOWING, // E
				Operator.DESCENDANT, // F
				Operator.ANCESTOR, // G
				Operator.PARENT, // H
				Operator.IMMEDIATE_FOLLOWING_SIBLING, // I
				Operator.CHILD, // J
				Operator.PARENT, // K
				Operator.PRECEDING_SIBLING, // L
				Operator.PARENT, // M
				Operator.FOLLOWING, // N
				Operator.IMMEDIATE_PRECEDING_SIBLING, // O
				Operator.CHILD, // P
				Operator.CHILD, // Q
				Operator.DESCENDANT // R
				});
		assertGetAllPipe(aPosEnum, true, a);

		Pipe b = a.getNext();
		assertSimplePipe(bPosEnum, Operator.DESCENDANT, b, true);

		Pipe bm = b.getNext();
		assertMetaPipe(Operator.PARENT, true, bm);

		Pipe d = ((MetaPipe) bm).getInner();
		assertGetAllPipe(dPosEnum, true, d);

		Pipe c = d.getNext();
		assertSimplePipe(cPosEnum, Operator.PRECEDING, c, false);

		Pipe bmm = bm.getNext();
		assertMetaPipe(Operator.IMMEDIATE_PRECEDING, false, bmm);

		Pipe h = ((MetaPipe) bmm).getInner();
		assertGetAllPipe(hPosEnum, true, h);

		Pipe g = h.getNext();
		assertSimplePipe(gPosEnum, Operator.CHILD, g, true);

		Pipe f = g.getNext();
		assertSimplePipe(fPosEnum, Operator.DESCENDANT, f, true);

		Pipe fm = f.getNext();
		assertMetaPipe(Operator.IMMEDIATE_PRECEDING_SIBLING, true, fm);

		Pipe j = ((MetaPipe) fm).getInner();
		assertGetAllPipe(jPosEnum, true, j);

		Pipe i = j.getNext();
		assertSimplePipe(iPosEnum, Operator.PARENT, i, false);

		Pipe e = fm.getNext();
		assertSimplePipe(ePosEnum, Operator.ANCESTOR, e, true);

		Pipe em = e.getNext();
		assertMetaPipe(Operator.CHILD, true, em);

		Pipe n = ((MetaPipe) em).getInner();
		assertGetAllPipe(nPosEnum, true, n);

		Pipe m = n.getNext();
		assertSimplePipe(mPosEnum, Operator.PRECEDING, m, true);

		Pipe l = m.getNext();
		assertSimplePipe(lPosEnum, Operator.CHILD, l, true);

		Pipe k = l.getNext();
		assertSimplePipe(kPosEnum, Operator.FOLLOWING_SIBLING, k, true);

		Pipe km = k.getNext();
		assertMetaPipe(Operator.IMMEDIATE_FOLLOWING_SIBLING, false, km);

		Pipe emm = em.getNext();
		assertMetaPipe(Operator.PARENT, false, emm);

		Pipe r = ((MetaPipe) emm).getInner();
		assertGetAllPipe(rPosEnum, true, r);

		Pipe q = r.getNext();
		assertSimplePipe(qPosEnum, Operator.ANCESTOR, q, false);
	}

	public void testSimplePipeExecuteCallsNextAfterItself() throws Exception {
		StaircaseJoin join = new StaircaseJoin(npa);
		HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(npa,
				join);
		IndexReader rdr = setupIndexWithDocs("(S(A(B C))(B(A C)))");
		DocsAndPositionsEnum aPosEnum = getPosEnum(rdr, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(rdr, 0, new Term("s", "B"));

		GetAllPipe a = pipeline.new GetAllPipe(aPosEnum);
		SimplePipe b = pipeline.new SimplePipe(bPosEnum,
				Operator.DESCENDANT, a);
		a.setNext(b);
		NodePositions prev = new NodePositions();
		
		pipeline.setPrevBuffer(prev);
		
		NodePositions out = a.execute();
		assertPositions(new int[] { 0, 1, 2, 2 }, 0, out);

		assertPositions(new int[] { 0, 1, 2, 2 }, 0, join.buffers[0]);
		assertPositions(new int[] { 0, 1, 1, 5, 1, 2, 2, 4 }, 4, prev);
		assertFalse(prev == join.buffers[0]); // prev and firstBuffer are distinct

		assertPositions(new int[] { 0, 1, 2, 2, 1, 2, 1, 5 }, 8, join.buffers[1]);
	}

	private void assertMetaPipe(Operator expectedMetaOp,
			boolean metaHasNext, Pipe p) {
		assertNotNull(p);
		assertTrue(p instanceof MetaPipe);
		assertEquals(expectedMetaOp, ((MetaPipe) p).getOp());
		Pipe ip = ((MetaPipe) p).getInner();
		assertTrue("Expected inner pipe to be an instance of GetAllPipe",
				ip instanceof GetAllPipe);
		if (metaHasNext) {
			assertNotNull(p.getNext());
		} else {
			assertNull(p.getNext());
		}
	}

	private void assertGetAllPipe(DocsAndPositionsEnum expectedPosEnum,
			boolean hasNext, Pipe p) {
		assertTrue(p instanceof GetAllPipe);
		assertEquals(expectedPosEnum, ((GetAllPipe) p).node);
		if (hasNext) {
			assertNotNull(p.getNext());
		} else {
			assertNull(p.getNext());
		}
	}

	private void assertSimplePipe(DocsAndPositionsEnum expectedPosEnum,
			Operator expectedOp, Pipe p, boolean hasNext) {
		assertNotNull(p);
		assertTrue(p instanceof SimplePipe);
		assertEquals(expectedOp, ((SimplePipe) p).getOp());
		assertEquals(expectedPosEnum, ((SimplePipe) p).node);
		if (hasNext) {
			assertNotNull(p.getNext());
		} else {
			assertNull(p.getNext());
		}
	}

	private PostingsAndFreq getPf(DocsAndPositionsEnum aPosEnum, int pos,
			PostingsAndFreq... children) {
		PostingsAndFreq pf = new PostingsAndFreq(aPosEnum, 1, pos, null);
		pf.children = new PostingsAndFreq[children.length];
		for (int i = 0; i < children.length; i++) {
			pf.children[i] = children[i];
		}
		return pf;
	}
}
