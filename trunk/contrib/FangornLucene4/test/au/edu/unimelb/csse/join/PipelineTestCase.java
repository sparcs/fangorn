package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.join.BooleanJoinPipeline.Pipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.GetAllPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.MetaPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.SimplePipe;
import au.edu.unimelb.csse.join.LookaheadTermEarlyPipeline.GetAllLookaheadPipe;

public abstract class PipelineTestCase extends IndexTestCase {
	protected void assertMetaPipe(Operator expectedMetaOp, boolean metaHasNext,
			boolean innerIsGetAll, Pipe p) {
		assertNotNull(p);
		assertTrue(p instanceof MetaPipe);
		assertEquals(expectedMetaOp, ((MetaPipe) p).getOp());
		Pipe ip = ((MetaPipe) p).getInner();
		if (innerIsGetAll) {
			assertTrue("Expected inner pipe to be an instance of GetAllPipe",
					ip instanceof GetAllPipe);
		} else {
			assertTrue(
					"Expected inner pipe to be an instance of GetAllLookaheadPipe",
					ip instanceof GetAllLookaheadPipe);
		}
		assertHasNext(metaHasNext, p);
	}

	protected void assertGetAllPipe(DocsAndPositionsEnum expectedPosEnum,
			boolean hasNext, Pipe p) {
		assertTrue(p instanceof GetAllPipe);
		assertEquals(expectedPosEnum, ((GetAllPipe) p).node);
		assertHasNext(hasNext, p);
	}

	protected void assertHasNext(boolean hasNext, Pipe p) {
		if (hasNext) {
			assertNotNull(p.getNext());
		} else {
			assertNull(p.getNext());
		}
	}

	protected void assertSimplePipe(DocsAndPositionsEnum expectedPosEnum,
			Operator expectedOp, Pipe p, boolean hasNext) {
		assertNotNull(p);
		assertTrue(p instanceof SimplePipe);
		assertEquals(expectedOp, ((SimplePipe) p).getOp());
		assertEquals(expectedPosEnum, ((SimplePipe) p).node);
		assertHasNext(hasNext, p);
	}

	protected PostingsAndFreq getPf(DocsAndPositionsEnum aPosEnum, int pos,
			PostingsAndFreq... children) {
		PostingsAndFreq pf = new PostingsAndFreq(aPosEnum, 1, pos, null);
		pf.children = new PostingsAndFreq[children.length];
		for (int i = 0; i < children.length; i++) {
			pf.children[i] = children[i];
		}
		return pf;
	}
}
