package au.edu.unimelb.csse.join;

import java.util.ArrayList;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.join.FullPairJoinPipeline.FirstPipe;
import au.edu.unimelb.csse.join.FullPairJoinPipeline.Pipe;
import au.edu.unimelb.csse.join.FullPairJoinPipeline.SimplePipe;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class FullPairJoinPipelineTest extends IndexTestCase {
	private LogicalNodePositionAware lrdp = new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111);
	private FullPairJoin join = new MPMGJoin(lrdp);

	public void testCreatesOneNodeDescPipeline() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B C))");

		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		PostingsAndFreq a = getPf(aPosEnum, 0);

		Pipe p = pipeline.createExecPipeline(a,
				new Operator[] { Operator.DESCENDANT });
		assertFirstPipe(a, Operator.DESCENDANT, false, p);
	}

	public void testCreatesOneNodeChildPipeline() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B C))");

		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		PostingsAndFreq a = getPf(aPosEnum, 0);

		Pipe p = pipeline.createExecPipeline(a,
				new Operator[] { Operator.CHILD });
		assertFirstPipe(a, Operator.CHILD, false, p);
	}

	public void testSimplePipelineConstruction() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B(C D)))");

		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(r, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(r, 0, new Term("s", "C"));
		PostingsAndFreq c = getPf(cPosEnum, 2);
		PostingsAndFreq b = getPf(bPosEnum, 1, c);
		PostingsAndFreq a = getPf(aPosEnum, 0, b);

		Pipe pipeA = pipeline.createExecPipeline(a, new Operator[] {
				Operator.DESCENDANT, Operator.FOLLOWING,
				Operator.PRECEDING_SIBLING });
		assertFirstPipe(a, Operator.DESCENDANT, true, pipeA);

		Pipe pipeB = pipeA.getNext();
		assertSimplePipe(b, Operator.FOLLOWING, 0, true, pipeB);

		Pipe pipeC = pipeB.getNext();
		assertSimplePipe(c, Operator.PRECEDING_SIBLING, 1, false, pipeC);
	}

	public void testBranchedQueryConstruction() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(B(C D)(E F)))");

		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);
		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(r, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(r, 0, new Term("s", "C"));
		DocsAndPositionsEnum dPosEnum = getPosEnum(r, 0, new Term("s", "D"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(r, 0, new Term("s", "E"));
		DocsAndPositionsEnum fPosEnum = getPosEnum(r, 0, new Term("s", "F"));

		PostingsAndFreq c = getPf(cPosEnum, 2);
		PostingsAndFreq d = getPf(dPosEnum, 3);
		PostingsAndFreq b = getPf(bPosEnum, 1, c, d);
		PostingsAndFreq f = getPf(fPosEnum, 5);
		PostingsAndFreq e = getPf(ePosEnum, 4, f);
		PostingsAndFreq a = getPf(aPosEnum, 0, b, e);

		Pipe pipeA = pipeline.createExecPipeline(a, new Operator[] {
				Operator.CHILD, Operator.DESCENDANT,
				Operator.IMMEDIATE_FOLLOWING, Operator.PRECEDING_SIBLING,
				Operator.PRECEDING, Operator.ANCESTOR });
		assertFirstPipe(a, Operator.CHILD, true, pipeA);

		Pipe pipeB = pipeA.getNext();
		assertSimplePipe(b, Operator.DESCENDANT, 0, true, pipeB);

		Pipe pipeC = pipeB.getNext();
		assertSimplePipe(c, Operator.IMMEDIATE_FOLLOWING, 1, true, pipeC);

		Pipe pipeD = pipeC.getNext();
		assertSimplePipe(d, Operator.PRECEDING_SIBLING, 1, true, pipeD);

		Pipe pipeE = pipeD.getNext();
		assertSimplePipe(e, Operator.PRECEDING, 0, true, pipeE);

		Pipe pipeF = pipeE.getNext();
		assertSimplePipe(f, Operator.ANCESTOR, 4, false, pipeF);
	}

	public void testSetPrevPostionsFromResults() throws Exception {
		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);

		int[] res0 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 0, 1, 2, 1 };
		int[] res1 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 2 };
		int[] res2 = new int[] { 1, 3, 1, 6, 0, 0, 0, 0, 1, 2, 3, 2 };
		int[] res3 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 2, 3, 3, 3 };
		int[] res4 = new int[] { 1, 3, 1, 6, 0, 0, 0, 0, 2, 3, 3, 3 };
		int[] res5 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 3, 4, 2, 5 };
		pipeline.results = new ArrayList<int[]>();
		pipeline.results.add(res0);
		pipeline.results.add(res1);
		pipeline.results.add(res2);
		pipeline.results.add(res3);
		pipeline.results.add(res4);
		pipeline.results.add(res5);

		pipeline.prevPositions.reset();
		assertEquals(0, pipeline.prevPositions.size);
		assertEquals(0, pipeline.prevPositions.offset);

		NPAPathPositionComparator comparator0 = new NPAPathPositionComparator(
				lrdp, 0);
		pipeline.setPrevPositionsFromResults(0, comparator0);
		assertEquals(8, pipeline.prevPositions.size);
		assertPositions(new int[] { 0, 4, 0, 0, 1, 3, 1, 6 }, 4,
				pipeline.prevPositions);

		NPAPathPositionComparator comparator2 = new NPAPathPositionComparator(
				lrdp, 2);
		pipeline.setPrevPositionsFromResults(2, comparator2);
		assertEquals(16, pipeline.prevPositions.size);
		assertPositions(new int[] { 0, 1, 2, 1, 1, 2, 3, 2, 2, 3, 3, 3, 3, 4,
				2, 5 }, 12, pipeline.prevPositions);
	}

	public void testMergeNodePairPositionsWithResults() throws Exception {
		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);

		int[] res0 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 0, 1, 2, 1 };
		int[] res1 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 2 };
		int[] res2 = new int[] { 1, 3, 1, 6, 0, 0, 0, 0, 1, 2, 3, 2 };
		int[] res3 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 2, 3, 3, 3 };
		int[] res4 = new int[] { 1, 3, 1, 6, 0, 0, 0, 0, 2, 3, 3, 3 };
		int[] res5 = new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 3, 4, 2, 5 };
		pipeline.results = new ArrayList<int[]>();
		pipeline.results.add(res0);
		pipeline.results.add(res1);
		pipeline.results.add(res2);
		pipeline.results.add(res3);
		pipeline.results.add(res4);
		pipeline.results.add(res5);

		int[] node1 = new int[] { 1, 2, 3, 2, 3, 4, 2, 5, 3, 4, 2, 5 };
		int[] node2 = new int[] { 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17 };
		pipeline.nodePairPositions.size = 12;
		System.arraycopy(node1, 0, pipeline.nodePairPositions.node1, 0,
				node1.length);
		System.arraycopy(node2, 0, pipeline.nodePairPositions.node2, 0,
				node2.length);

		NPAPathPositionComparator comparator2 = new NPAPathPositionComparator(
				lrdp, 2);
		pipeline.setPrevPositionsFromResults(2, comparator2);
		assertEquals(16, pipeline.prevPositions.size);
		assertPositions(new int[] { 0, 1, 2, 1, 1, 2, 3, 2, 2, 3, 3, 3, 3, 4,
				2, 5 }, 12, pipeline.prevPositions);

		pipeline.mergeNodePairPositionsWithResults(2, 1);
		assertEquals(4, pipeline.results.size());
		assertIntArray(new int[] { 0, 4, 0, 0, 5, 6, 7, 8, 1, 2, 3, 2 },
				pipeline.results.get(0));
		assertIntArray(new int[] { 1, 3, 1, 6, 5, 6, 7, 8, 1, 2, 3, 2 },
				pipeline.results.get(1));
		assertIntArray(new int[] { 0, 4, 0, 0, 10, 11, 12, 13, 3, 4, 2, 5 },
				pipeline.results.get(2));
		assertIntArray(new int[] { 0, 4, 0, 0, 14, 15, 16, 17, 3, 4, 2, 5 },
				pipeline.results.get(3));
	}

	public void testExecuteReturnsResultsWhenFound() throws Exception {
		IndexReader r = setupIndexWithDocs("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))");

		DocsAndPositionsEnum aPosEnum = getPosEnum(r, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(r, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(r, 0, new Term("s", "C"));
		DocsAndPositionsEnum dPosEnum = getPosEnum(r, 0, new Term("s", "D"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(r, 0, new Term("s", "E"));

		PostingsAndFreq c = getPf(cPosEnum, 2);
		PostingsAndFreq e = getPf(ePosEnum, 4);
		PostingsAndFreq b = getPf(bPosEnum, 1, c);
		PostingsAndFreq d = getPf(dPosEnum, 3, e);
		PostingsAndFreq a = getPf(aPosEnum, 0, b, d);
		FullPairJoinPipeline pipeline = new FullPairJoinPipeline(lrdp, join);

		Pipe p = pipeline.createExecPipeline(a, new Operator[] {
				Operator.DESCENDANT, Operator.DESCENDANT, Operator.DESCENDANT,
				Operator.DESCENDANT, Operator.DESCENDANT });

		pipeline.setPrevAndBuffers(new NodePositions(), new NodePositions[] {
				new NodePositions(), new NodePositions() });
		p.execute();
		assertEquals(9, pipeline.results.size());

		assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2,
				14, 5, 6, 3, 13 }, pipeline.results.get(0));
		assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6,
				2, 14, 5, 6, 3, 13 }, pipeline.results.get(1));
		assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6,
				2, 14, 5, 6, 3, 13 }, pipeline.results.get(2));
		assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2,
				14, 5, 6, 5, 11 }, pipeline.results.get(3));
		assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6,
				2, 14, 5, 6, 5, 11 }, pipeline.results.get(4));
		assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6,
				2, 14, 5, 6, 5, 11 }, pipeline.results.get(5));
		assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 5, 6, 4,
				12, 5, 6, 5, 11 }, pipeline.results.get(6));
		assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6,
				4, 12, 5, 6, 5, 11 }, pipeline.results.get(7));
		assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6,
				4, 12, 5, 6, 5, 11 }, pipeline.results.get(8));
	}

	private void assertSimplePipe(PostingsAndFreq expectedNode,
			Operator expectedOp, int expectedParentPos, boolean hasNext,
			Pipe pipe) {
		assertNotNull(pipe);
		assertTrue(pipe instanceof SimplePipe);
		assertEquals(expectedNode, ((SimplePipe) pipe).node);
		assertEquals(expectedOp, ((SimplePipe) pipe).op);
		assertEquals(expectedParentPos, ((SimplePipe) pipe).parentPos);
		if (hasNext) {
			assertNotNull(pipe.getNext());
		} else {
			assertNull(pipe.getNext());
		}
	}

	private void assertFirstPipe(PostingsAndFreq expectedNode,
			Operator expectedOp, boolean hasNext, Pipe pipe) {
		assertNotNull(pipe);
		assertTrue(pipe instanceof FirstPipe);
		assertEquals(expectedNode, ((FirstPipe) pipe).node);
		assertEquals(expectedOp, ((FirstPipe) pipe).op);
		if (hasNext) {
			assertNotNull(pipe.getNext());
		} else {
			assertNull(pipe.getNext());
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
