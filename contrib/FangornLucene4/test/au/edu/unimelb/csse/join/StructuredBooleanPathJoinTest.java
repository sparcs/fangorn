package au.edu.unimelb.csse.join;

import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredBooleanPathJoinTest extends IndexTestCase {
	public void testResults() throws Exception {

		LogicalNodePositionAware lrdp = new LRDP(
				LRDP.PhysicalPayloadFormat.BYTE1111);

		HalfPairJoin[] joins = new HalfPairJoin[] { new StaircaseJoin(lrdp),
				new MPMGModSingleJoin(lrdp) };

		for (int i = 0; i < joins.length; i++) {
			HalfPairJoin hpj = joins[i];
			StructuredBooleanPathJoin join = new StructuredBooleanPathJoin(
					new String[] { "A", "B", "C", "D", "E" }, new int[] { -1,
							0, 1, 0, 3 }, getDescOp(5), hpj, lrdp);

			IndexReader r = setupIndexWithDocs("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))");
			join.setup(r);
			join.nextDoc();

			assertTrue(join.match());
			if (i == 0) {
				assertPositions(new int[] { 0, 6, 0, 0, 4, 6, 1, 15 }, 4,
						((StaircaseJoin) hpj).buffers[0]);
			} else {
				assertPositions(new int[] { 0, 6, 0, 0, 4, 6, 1, 15 }, 4,
						((MPMGModSingleJoin) hpj).buffers[0]);

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
