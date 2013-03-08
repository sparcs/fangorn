package au.edu.unimelb.csse.join;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.BinaryOperator;

public class StackTreeJoinTest extends PairJoinTestCase {

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new StackTreeJoin(lrdp);
	}

	public void testNoDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(NA ND))",
				"(SS(NA ND)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8, 0, "AA", "ND");
		joinAndAssertOutput(0, 4, prev, BinaryOperator.DESCENDANT, posEnum);
		posEnum = initPrevGetNext(r, 8, 1, "AA", "ND");
		joinAndAssertOutput(0, 1, prev, BinaryOperator.DESCENDANT, posEnum);
	}

	public void testNestedTreeDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(24, 17, prev, BinaryOperator.DESCENDANT, posEnum);
	}

	public void testNestedTreeChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 17, prev, BinaryOperator.CHILD, posEnum);
	}
	
	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 13, prev, BinaryOperator.DESCENDANT, posEnum);
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 13, prev, BinaryOperator.CHILD, posEnum);
	}

}
