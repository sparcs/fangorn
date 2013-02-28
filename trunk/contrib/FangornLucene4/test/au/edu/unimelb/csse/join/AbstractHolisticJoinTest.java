package au.edu.unimelb.csse.join;

import java.util.ArrayList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;

public class AbstractHolisticJoinTest extends IndexTestCase {
	public void testGetPathSolutionsRecursesFromLeafToRootOfQuery() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC" },
				new Operator[] { Op.DESCENDANT, Op.DESCENDANT, Op.DESCENDANT });
		IndexWriter w = setupIndex();
		// this example shows the function of parent stack pointers 
		//   stored at each position  
		w.addDocument(getDoc("(AA(AA(BB(BB(AA(BB CC))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
				
		setupPositionStacks(ps);
		
		PostingsAndFreq cpf = ps.postingsFreqs[2];
		assertEquals(2, cpf.position);//ensure it is CC's postingsFreq
		
		ArrayList<int[]> results = new ArrayList<int[]>();
		ps.getPathSolutions(results, cpf);
		
		assertEquals(7, results.size());
		assertIntArray(new int[]{0, 13, 0, 7, 
				2, 11, 2, 5, 
				6, 7, 6, 1}, results.get(0));
		assertIntArray(new int[]{1, 12, 1, 6, 
				2, 11, 2, 5, 
				6, 7, 6, 1}, results.get(1));
		assertIntArray(new int[]{0, 13, 0, 7, 
				3, 10, 3, 4, 
				6, 7, 6, 1}, results.get(2));
		assertIntArray(new int[]{1, 12, 1, 6, 
				3, 10, 3, 4, 
				6, 7, 6, 1}, results.get(3));
		assertIntArray(new int[]{0, 13, 0, 7, 
				5, 8, 5, 2, 
				6, 7, 6, 1}, results.get(4));
		assertIntArray(new int[]{1, 12, 1, 6, 
				5, 8, 5, 2, 
				6, 7, 6, 1}, results.get(5));
		assertIntArray(new int[]{4, 9, 4, 4, 
				5, 8, 5, 2, 
				6, 7, 6, 1}, results.get(6));
	}
	
	private void setupPositionStacks(PathStackJoin ps) {
		// setting up stack and stacksizes manually
		int[] aStack = new int[] {0, 13, 0, 7, -1, 
				1, 12, 1, 6, -1,
				4, 9, 4, 4, -1};
		int[] bStack = new int[] {2, 11, 2, 5, 1, 
				3, 10, 3, 4, 1, 
				5, 8, 5, 2, 2};
		int[] cStack = new int[] {6, 7, 6, 1, 2};
		System.arraycopy(aStack, 0, ps.positionStacks[0], 0, aStack.length);
		System.arraycopy(bStack, 0, ps.positionStacks[1], 0, bStack.length);
		System.arraycopy(cStack, 0, ps.positionStacks[2], 0, cStack.length);
		
		ps.positionStacksSizes[0] = 3;
		ps.positionStacksSizes[1] = 3;
		ps.positionStacksSizes[2] = 1;
	}

	public void testGetPathSolutionsTestsRootChildOp() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "AA", "BB", "CC" },
				new Operator[] { Op.CHILD, Op.DESCENDANT, Op.CHILD });
		IndexWriter w = setupIndex();
		// this example shows the function of parent stack pointers 
		//   stored at each position  
		w.addDocument(getDoc("(AA(AA(BB(BB(AA(BB CC))))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
				
		setupPositionStacks(ps);
		
		PostingsAndFreq cpf = ps.postingsFreqs[2];
		assertEquals(2, cpf.position);//ensure it is CC's postingsFreq
		
		ArrayList<int[]> results = new ArrayList<int[]>();
		ps.getPathSolutions(results, cpf);
		
		assertEquals(1, results.size());
		assertIntArray(new int[]{0, 13, 0, 7, 
				5, 8, 5, 2, 
				6, 7, 6, 1}, results.get(0));
	}
	
	public void testStackNotUpdatedIfParentStackIsEmpty()
			throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "CC", "DD" },
				new Operator[] { Op.DESCENDANT, Op.DESCENDANT, Op.DESCENDANT });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(CC(BB CC))(DD EE))")); // doc 2
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
//		ps.setupPerDoc(); called automatically
		assertIntArray(new int[] { 0, 0, 0 }, ps.positionStacksSizes);

		int minSourcePos = ps.getMinSource();
		assertEquals(1, minSourcePos); // 1st CC chosen
		assertEquals(1, ps.preorderPos[1]); // preOrder pos is 1

		ps.updateStackIfNeeded(minSourcePos); // no update happens
		assertIntArray(new int[] { 0, 0, 0 }, ps.positionStacksSizes);
	}

	public void testStackAlwaysUpdatedForQueryRoot() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "CC", "DD" },
				new Operator[] { Op.DESCENDANT, Op.DESCENDANT, Op.DESCENDANT });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB CC))(DD EE))")); // doc 2
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
//		ps.setupPerDoc(); called automatically
		assertIntArray(new int[] { 0, 0, 0 }, ps.positionStacksSizes);

		int minSourcePos = ps.getMinSource();
		assertEquals(0, minSourcePos); // 1st BB chosen
		assertEquals(1, ps.preorderPos[0]); // preOrder pos is 1

		ps.updateStackIfNeeded(minSourcePos); // update happens
		assertIntArray(new int[] { 1, 0, 0 }, ps.positionStacksSizes);
		int[] returnedStackContent = new int[5];
		System.arraycopy(ps.positionStacks[0], 0, returnedStackContent, 0, 5);
		assertIntArray(new int[] { 0, 1, 1, 4, -1 }, returnedStackContent);
	}

	public void testStackUpdatedWhenParentStackNotEmpty() throws Exception {
		PathStackJoin ps = new PathStackJoin(new String[] { "BB", "CC", "DD" },
				new Operator[] { Op.DESCENDANT, Op.DESCENDANT, Op.DESCENDANT });
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(AA(BB(BB(CC(DD EE)))))"));
		IndexReader r = commitIndexAndOpenReader(w);
		ps.setup(r);
		ps.nextDoc();
//		ps.setupPerDoc(); called automatically
		assertIntArray(new int[] { 0, 0, 0 }, ps.positionStacksSizes);

		int minSourcePos = ps.getMinSource();
		assertEquals(0, minSourcePos); // 1st BB chosen
		assertEquals(1, ps.preorderPos[0]); // preOrder pos is 1
		ps.updateStackIfNeeded(minSourcePos); // update happens
		assertIntArray(new int[] { 1, 0, 0 }, ps.positionStacksSizes);
	}

}
