package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.join.BooleanJoinPipeline.Pipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.GetAllPipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.MetaPipe;
import au.edu.unimelb.csse.join.LookaheadTermEarlyPipeline.GetAllLookaheadPipe;
import au.edu.unimelb.csse.join.LookaheadTermEarlyPipeline.LookaheadPipe;
import au.edu.unimelb.csse.join.LookaheadTermEarlyPipeline.MetaTerminateEarlyPipe;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class LookaheadTermEarlyPipelineTest extends PipelineTestCase {
	private LogicalNodePositionAware npa = new LRDP(
			LRDP.PhysicalPayloadFormat.BYTE1111);

	public void testCreatePipeline() throws Exception {
		LookaheadTermEarlyJoin join = new LookaheadTermEarlyJoin(npa);
		LookaheadTermEarlyPipeline pipeline = new LookaheadTermEarlyPipeline(
				npa, join);
		IndexReader rdr = setupIndexWithDocs("(A(B(C D)(E(F G)(H I)))(J K))");
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

		PostingsAndFreq dpf = getPf(dPosEnum, 3);
		PostingsAndFreq cpf = getPf(cPosEnum, 2, dpf);

		PostingsAndFreq gpf = getPf(gPosEnum, 6);
		PostingsAndFreq fpf = getPf(fPosEnum, 5, gpf);

		PostingsAndFreq ipf = getPf(iPosEnum, 8);
		PostingsAndFreq hpf = getPf(hPosEnum, 7, ipf);

		PostingsAndFreq epf = getPf(ePosEnum, 4, fpf, hpf);
		PostingsAndFreq bpf = getPf(bPosEnum, 1, cpf, epf);

		PostingsAndFreq kpf = getPf(kPosEnum, 10);
		PostingsAndFreq jpf = getPf(jPosEnum, 9, kpf);

		PostingsAndFreq apf = getPf(aPosEnum, 6, bpf, jpf);

		Pipe pipe = pipeline.createExecPipeline(apf, new Operator[] {
				Operator.DESCENDANT, // A
				Operator.DESCENDANT, // B
				Operator.CHILD, // C
				Operator.FOLLOWING, // D
				Operator.CHILD, // E
				Operator.PRECEDING, // F
				Operator.PRECEDING_SIBLING, // G
				Operator.PRECEDING, // H
				Operator.ANCESTOR, // I
				Operator.IMMEDIATE_FOLLOWING, // J
				Operator.PARENT // K
				});

		assertNotNull(pipe);

		assertGetAllPipe(aPosEnum, true, pipe);

		pipe = pipe.getNext();
		assertMetaPipe(Operator.ANCESTOR, true, false, pipe);

		Pipe innerpipe = ((MetaPipe) pipe).getInner();

		assertGetAllLookaheadPipe(dPosEnum, true, innerpipe);

		innerpipe = innerpipe.getNext();

		assertSimplePipe(cPosEnum, Operator.PRECEDING, innerpipe, true);

		innerpipe = innerpipe.getNext();

		assertSimplePipe(bPosEnum, Operator.PARENT, 
				innerpipe, true);

		innerpipe = innerpipe.getNext();

		assertMetaPipe(Operator.PARENT, false, true, innerpipe);

		Pipe inner2pipe = ((MetaPipe) innerpipe).getInner();

		assertGetAllPipe(gPosEnum, true, inner2pipe);

		inner2pipe = inner2pipe.getNext();

		assertLookaheadPipe(fPosEnum, Operator.FOLLOWING_SIBLING,
				Operator.FOLLOWING, true, inner2pipe);

		inner2pipe = inner2pipe.getNext();

		assertSimplePipe(ePosEnum, Operator.FOLLOWING, inner2pipe, true);

		inner2pipe = inner2pipe.getNext();

		assertMetaPipe(Operator.FOLLOWING, false, false, inner2pipe);

		Pipe inner3pipe = ((MetaPipe) inner2pipe).getInner();

		assertGetAllLookaheadPipe(iPosEnum, true, inner3pipe);

		inner3pipe = inner3pipe.getNext();

		assertLookaheadPipe(hPosEnum, Operator.DESCENDANT, Operator.FOLLOWING,
				false, inner3pipe);

		pipe = pipe.getNext();

		assertMetaPipe(Operator.IMMEDIATE_PRECEDING, false, true, pipe);

		innerpipe = ((MetaPipe) pipe).getInner();

		assertGetAllPipe(kPosEnum, true, innerpipe);

		innerpipe = innerpipe.getNext();

		assertSimplePipe(jPosEnum, Operator.CHILD, innerpipe, false);

		assertNull(pipe.getNext());
	}

	public void testPipelineExecutionReturnsNoResults() throws Exception {
		LookaheadTermEarlyJoin join = new LookaheadTermEarlyJoin(npa);
		LookaheadTermEarlyPipeline pipeline = new LookaheadTermEarlyPipeline(
				npa, join);

		String sent = "(S1 (S (NP (DT The) (NN road)) (VP (VBZ runs) (ADVP (RB mostly) (JJ parallel) (PP (TO to) (NP (DT the) (NNP Gatineau) (NNP River)))) (PP (IN on) (NP (NP (DT the) (JJ eastern) (NN side)) (PP (IN of) (NP (PRP it)))))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);
		DocsAndPositionsEnum npPosEnum = getPosEnum(r, 0, new Term("s", "NP"));
		DocsAndPositionsEnum vpPosEnum = getPosEnum(r, 0, new Term("s", "VP"));
		PostingsAndFreq vpf = getPf(vpPosEnum, 1);
		PostingsAndFreq npf = getPf(npPosEnum, 0, vpf);
		Pipe pipe = pipeline.createExecPipeline(npf, new Operator[] {
				Operator.DESCENDANT, Operator.DESCENDANT });
		pipeline.setPrevBuffer(new NodePositions());
		NodePositions results = pipe.execute();
		assertEquals(0, results.size);
	}

	public void testCannotPerformLookaheadOnBranchNodeWithVaryingChildOps()
			throws Exception {
		String sent = "(A(B(C(D W)(E W))(F W)(G W)))";
		LookaheadTermEarlyJoin join = new LookaheadTermEarlyJoin(npa);
		LookaheadTermEarlyPipeline pipeline = new LookaheadTermEarlyPipeline(
				npa, join);
		Operator[] operators = new Operator[] { Operator.DESCENDANT,
				Operator.DESCENDANT, Operator.DESCENDANT, Operator.FOLLOWING,
				Operator.FOLLOWING, Operator.CHILD, Operator.DESCENDANT };

		IndexReader rdr = setupIndexWithDocs(sent);
		DocsAndPositionsEnum aPosEnum = getPosEnum(rdr, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(rdr, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(rdr, 0, new Term("s", "C"));
		DocsAndPositionsEnum dPosEnum = getPosEnum(rdr, 0, new Term("s", "D"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(rdr, 0, new Term("s", "E"));
		DocsAndPositionsEnum fPosEnum = getPosEnum(rdr, 0, new Term("s", "F"));
		DocsAndPositionsEnum gPosEnum = getPosEnum(rdr, 0, new Term("s", "G"));

		PostingsAndFreq dpf = getPf(dPosEnum, 3);
		PostingsAndFreq epf = getPf(ePosEnum, 4);
		PostingsAndFreq cpf = getPf(cPosEnum, 2, dpf, epf);

		PostingsAndFreq fpf = getPf(fPosEnum, 5);
		PostingsAndFreq gpf = getPf(gPosEnum, 6);
		PostingsAndFreq bpf = getPf(bPosEnum, 1, cpf, fpf, gpf);

		PostingsAndFreq apf = getPf(aPosEnum, 0, bpf);

		Pipe pipe = pipeline.createExecPipeline(apf, operators);

		assertGetAllLookaheadPipe(aPosEnum, true, pipe);

		pipe = pipe.getNext();

		assertSimplePipe(bPosEnum, Operator.DESCENDANT, pipe, true);

	}

	public void testShouldLookaheadTopmostBranchNodeAndNoOtherBranchNodes()
			throws Exception {
		String sent = "(A(B(C(D W)(E W))(F W)(G W)))";
		LookaheadTermEarlyJoin join = new LookaheadTermEarlyJoin(npa);
		LookaheadTermEarlyPipeline pipeline = new LookaheadTermEarlyPipeline(
				npa, join);
		Operator[] operators = new Operator[] { Operator.DESCENDANT,
				Operator.DESCENDANT, Operator.DESCENDANT, Operator.FOLLOWING,
				Operator.FOLLOWING, Operator.DESCENDANT, Operator.DESCENDANT };

		IndexReader rdr = setupIndexWithDocs(sent);
		DocsAndPositionsEnum aPosEnum = getPosEnum(rdr, 0, new Term("s", "A"));
		DocsAndPositionsEnum bPosEnum = getPosEnum(rdr, 0, new Term("s", "B"));
		DocsAndPositionsEnum cPosEnum = getPosEnum(rdr, 0, new Term("s", "C"));
		DocsAndPositionsEnum dPosEnum = getPosEnum(rdr, 0, new Term("s", "D"));
		DocsAndPositionsEnum ePosEnum = getPosEnum(rdr, 0, new Term("s", "E"));
		DocsAndPositionsEnum fPosEnum = getPosEnum(rdr, 0, new Term("s", "F"));
		DocsAndPositionsEnum gPosEnum = getPosEnum(rdr, 0, new Term("s", "G"));

		PostingsAndFreq dpf = getPf(dPosEnum, 3);
		PostingsAndFreq epf = getPf(ePosEnum, 4);
		PostingsAndFreq cpf = getPf(cPosEnum, 2, dpf, epf);

		PostingsAndFreq fpf = getPf(fPosEnum, 5);
		PostingsAndFreq gpf = getPf(gPosEnum, 6);
		PostingsAndFreq bpf = getPf(bPosEnum, 1, cpf, fpf, gpf);

		PostingsAndFreq apf = getPf(aPosEnum, 0, bpf);

		Pipe pipe = pipeline.createExecPipeline(apf, operators);

		assertGetAllLookaheadPipe(aPosEnum, true, pipe);

		pipe = pipe.getNext();

		assertLookaheadPipe(bPosEnum, Operator.DESCENDANT, Operator.DESCENDANT, true, pipe);
		
		Pipe metapipe = pipe.getNext();
		
		assertMetaPipe(Operator.ANCESTOR, true, false, metapipe);

		Pipe inner = ((MetaPipe)metapipe).getInner();
		
		assertGetAllLookaheadPipe(dPosEnum, true, inner);
		
		inner = inner.getNext();
		
		assertSimplePipe(cPosEnum, Operator.PRECEDING, inner, true);
		
		Pipe innermeta = inner.getNext();
		
		assertMetaPipe(Operator.PRECEDING, false, false, innermeta);
		
		innermeta = ((MetaPipe)innermeta).getInner();
		
		assertGetAllLookaheadPipe(ePosEnum, false, innermeta);
		
		metapipe = metapipe.getNext();
		
		assertMetaPipe(Operator.ANCESTOR, true, false, metapipe); // CHECK: expected Parent
		
		inner = ((MetaPipe)metapipe).getInner();
		
		assertGetAllLookaheadPipe(fPosEnum, false, inner);
		
		metapipe = metapipe.getNext();
		
		assertMetaTerminateEarlyPipe(Operator.ANCESTOR, false, false, metapipe);
		
		inner = ((MetaTerminateEarlyPipe) metapipe).getInner();
		
	}

	private void assertLookaheadPipe(DocsAndPositionsEnum posEnum, Operator op,
			Operator nextOp, boolean hasNext, Pipe pipe) {
		assertTrue(pipe instanceof LookaheadPipe);
		assertEquals(posEnum, ((LookaheadPipe) pipe).node);
		assertEquals(op, ((LookaheadPipe) pipe).op);
		assertEquals(nextOp, ((LookaheadPipe) pipe).nextOp);
		assertHasNext(hasNext, pipe);
	}

	private void assertGetAllLookaheadPipe(DocsAndPositionsEnum posEnum,
			boolean hasNext, Pipe pipe) {
		assertTrue(pipe instanceof GetAllLookaheadPipe);
		assertEquals(posEnum, ((GetAllLookaheadPipe) pipe).node);
		assertHasNext(hasNext, pipe);
	}
	
	protected void assertMetaTerminateEarlyPipe(Operator expectedMetaOp, boolean metaHasNext,
			boolean innerIsGetAll, Pipe p) {
		assertNotNull(p);
		assertTrue(p instanceof MetaTerminateEarlyPipe);
		assertEquals(expectedMetaOp, ((MetaTerminateEarlyPipe) p).getOp());
		Pipe ip = ((MetaTerminateEarlyPipe) p).getInner();
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

	
	

}
