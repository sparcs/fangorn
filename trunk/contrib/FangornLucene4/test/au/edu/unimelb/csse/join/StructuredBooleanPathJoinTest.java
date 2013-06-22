package au.edu.unimelb.csse.join;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.BooleanJoinPipeline.Pipe;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline.MetaPipe;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredBooleanPathJoinTest extends IndexTestCase {
	public void testResults() throws Exception {

		LogicalNodePositionAware lrdp = new LRDP(
				LRDP.PhysicalPayloadFormat.BYTE1111);

		JoinBuilder[] joins = new JoinBuilder[] { StaircaseJoin.JOIN_BUILDER,
				MPMGModSingleJoin.JOIN_BUILDER };

		for (int i = 0; i < joins.length; i++) {
			JoinBuilder jb = joins[i];
			HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(lrdp, jb);
			StructuredBooleanPathJoin join = new StructuredBooleanPathJoin(
					new String[] { "A", "B", "C", "D", "E" }, new int[] { -1,
							0, 1, 0, 3 }, getDescOp(5), pipeline, lrdp);

			IndexReader r = setupIndexWithDocs("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))");
			join.setup(r);
			join.nextDoc();

			assertTrue(join.match());

			Pipe lastPipe = pipeline.root.getNext().getNext();
			if (i == 0) {
				StaircaseJoin scj = (StaircaseJoin) ((MetaPipe) lastPipe).join;
				assertPositions(new int[] { 0, 6, 0, 0, 4, 6, 1, 15 }, 4,
						scj.result);
			} else {
				MPMGModSingleJoin mpmgmods = (MPMGModSingleJoin) ((MetaPipe) lastPipe).join;
				assertPositions(new int[] { 0, 6, 0, 0, 4, 6, 1, 15 }, 4,
						mpmgmods.result);

			}
		}
	}

	// TODO: write more tests

	private Operator[] getDescOp(int num) {
		Operator[] results = new Operator[num];
		for (int i = 0; i < results.length; i++) {
			results[i] = Operator.DESCENDANT;
		}
		return results;
	}
}
