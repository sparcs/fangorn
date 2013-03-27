package au.edu.unimelb.csse.join;

import java.util.List;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class StructuredFullPathJoinTest extends IndexTestCase {
	public void testReturnsResults() throws Exception {
		LogicalNodePositionAware lrdp = new LRDP(new BytePacking(4));

		FullPairJoin[] joins = new FullPairJoin[] { new MPMGJoin(lrdp),
				new MPMGModJoin(lrdp), new StackTreeJoin(lrdp) };

		for (FullPairJoin join : joins) {
			StructuredFullPathJoin sfpj = new StructuredFullPathJoin(
					new String[] { "A", "B", "C", "D", "E" }, new int[] { -1,
							0, 1, 0, 3 }, new Operator[] { Operator.DESCENDANT,
							Operator.DESCENDANT, Operator.DESCENDANT,
							Operator.DESCENDANT, Operator.DESCENDANT }, join,
					lrdp);

			IndexReader r = setupIndexWithDocs("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))");

			sfpj.setup(r);
			int docid = sfpj.nextDoc();

			assertEquals(0, docid);

			List<int[]> results = sfpj.match();

			assertEquals(9, results.size());

			assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4,
					6, 2, 14, 5, 6, 3, 13 }, results.get(0));
			assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4,
					6, 2, 14, 5, 6, 3, 13 }, results.get(1));
			assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4,
					6, 2, 14, 5, 6, 3, 13 }, results.get(2));
			assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4,
					6, 2, 14, 5, 6, 5, 11 }, results.get(3));
			assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4,
					6, 2, 14, 5, 6, 5, 11 }, results.get(4));
			assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4,
					6, 2, 14, 5, 6, 5, 11 }, results.get(5));
			assertIntArray(new int[] { 0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 5,
					6, 4, 12, 5, 6, 5, 11 }, results.get(6));
			assertIntArray(new int[] { 0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 5,
					6, 4, 12, 5, 6, 5, 11 }, results.get(7));
			assertIntArray(new int[] { 4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 5,
					6, 4, 12, 5, 6, 5, 11 }, results.get(8));
		}
	}

	public void testReturnsNoResults() throws Exception {
		LogicalNodePositionAware lrdp = new LRDP(new BytePacking(4));

		FullPairJoin[] joins = new FullPairJoin[] { new MPMGJoin(lrdp),
				new MPMGModJoin(lrdp), new StackTreeJoin(lrdp) };

		for (FullPairJoin join : joins) {
			StructuredFullPathJoin sfpj = new StructuredFullPathJoin(
					new String[] { "A", "B", "C", "D", "E" }, new int[] { -1,
							0, 1, 0, 3 }, new Operator[] { Operator.DESCENDANT,
							Operator.DESCENDANT, Operator.DESCENDANT,
							Operator.DESCENDANT, Operator.DESCENDANT }, join,
					lrdp);

			IndexReader r = setupIndexWithDocs("(A(A(E J)(C D))(A(B(G E)(E D)))(A(D(B(A(G J)))(E(D E)))))");

			sfpj.setup(r);
			int docid = sfpj.nextDoc();

			assertEquals(0, docid);

			List<int[]> results = sfpj.match();
			assertEquals(0, results.size());
		}
	}

	public void testBranchedQuery() throws Exception {
		LogicalNodePositionAware lrdp = new LRDP(new BytePacking(4));
		StructuredFullPathJoin join = new StructuredFullPathJoin(new String[] {
				"S", "VP", "PP", "IN", "NP", "VBN" }, new int[] { -1, 0, 1, 2,
				2, 4 },
				new Operator[] { Operator.DESCENDANT, Operator.CHILD,
						Operator.CHILD, Operator.CHILD, Operator.CHILD,
						Operator.CHILD }, new StackTreeJoin(lrdp), lrdp);

		String sent = "(S1 (S (NP (PRP He)) (ADVP (RB first)) (VP (VBZ appears) (PP (IN in) (NP (DT the) (VBN animated) (NN series))) (PP (IN in) (NP (DT the) (DT A) (NNP Real) (NNP American) (NN Hero) (NNS mini-series)))) (. .)))";
		IndexReader r = setupIndexWithDocs(sent);

		join.setup(r);
		int docid = join.nextDoc();
		assertEquals(0, docid);

		List<int[]> results = join.match();
		assertEquals(1, results.size());
	}

}
