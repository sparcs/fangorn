package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Operator;

public class AbstractPairwiseJoinTest extends IndexTestCase {
	private AbstractPairwiseJoin join;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new AbstractPairwiseJoin() {

			@Override
			public boolean validOper(Operator op) {
				return false;
			}

			@Override
			public int[] join(int[] prev, Operator op, DocsAndPositionsEnum node)
					throws IOException {
				return null;
			}
		};
	}
	
	@Test
	public void testGetAllPositionsInDoc() throws Exception {
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(AA DD)(AA DD)(AA DD))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		DocsAndPositionsEnum posEnum = getPosEnum(r, 0, new Term("s", "AA"));
		int[] positions = join.getAllPositions(posEnum);
		assertIntArray(new int[] { 0, 3, 0, 0, 0, 1, 1, 4, 1, 2, 1, 4, 2, 3,
				1, 4 }, positions);

		posEnum = getPosEnum(r, 0, new Term("s", "DD"));
		positions = new MPMGJoin().getAllPositions(posEnum);
		assertIntArray(new int[] { 0, 1, 2, 1, 1, 2, 2, 2, 2, 3, 2, 3 },
				positions);
	}
}
