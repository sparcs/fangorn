package au.edu.unimelb.csse.join;

import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import au.edu.unimelb.csse.CountingOp;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.TwigStackJoin.MergeNode;

public class TwigStackJoinTest extends HolisticJoinTestCase {
	
	public void testGetNextPositionSetsMaxPosReachedOnEOL() throws Exception {
		TwigStackJoin ts = new TwigStackJoin(new String[] { "A", "B", "C",
				"D", "E" }, new int[] { -1, 0, 1, 0, 3 }, getDescOp(5));
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		assertPfArrayPos(new String[] { "A", "B", "C", "D", "E" },
				ts.postingsFreqs);
		ts.nextDoc();

		// there are 2 Cs
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		assertIntArray(new int[]{0, 6, 0, 0, 1, 2, 2, 3, 2, 3, 3, 6, 1, 2, 3, 2, 0, 1, 2, 3}, ts.positions);

		ts.getNextPosition(2); // next C
		assertIntArray(new int[]{0, 6, 0, 0, 1, 2, 2, 3, 4, 5, 5, 9, 1, 2, 3, 2, 0, 1, 2, 3}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		ts.getNextPosition(2); // no more Cs; no change in positions list
		assertIntArray(new int[]{0, 6, 0, 0, 1, 2, 2, 3, 4, 5, 5, 9, 1, 2, 3, 2, 0, 1, 2, 3}, ts.positions); // no change
		assertMaxPosReached(new boolean[]{false, false, true, false, false}, ts);
	}
	
	public void testGetMinPosReturnsNodesInCorrectSequence() throws Exception {
		TwigStackJoin ts = new TwigStackJoin(new String[] { "A", "B", "C",
				"D", "E" }, new int[] { -1, 0, 1, 0, 3 }, getDescOp(5));
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		assertPfArrayPos(new String[] { "A", "B", "C", "D", "E" },
				ts.postingsFreqs);
		
		int doc = ts.nextDoc();
		assertEquals(0, doc);

		/**
		 * nodes with ids:
		 * (A1(A2(E1 J1)(B1 D1))(A3(B2(C1 E2)(E3 D2)))(A4(D3(B3(A5(C2 J2)))(E4(D4 E5)))))
		 * 
		 * positionsLists:
		 * A: (5): [[0, 6, 0, 0], [0, 2, 1, 15], [2, 4, 1, 15], [4, 6, 1, 15], [4, 5, 4, 10]]
		 * B: (3): [[1, 2, 2, 3], [2, 4, 2, 7], [4, 5, 3, 13]]
		 * C: (2): [[2, 3, 3, 6], [4, 5, 5, 9]]
		 * D: (4): [[1, 2, 3, 2], [3, 4, 4, 5], [4, 6, 2, 14], [5, 6, 4, 12]]
		 * E: (5): [[0, 1, 2, 3], [2, 3, 4, 4], [3, 4, 3, 6], [5, 6, 3, 13], [5, 6, 5, 11]]
		 */
		assertIntArray(new int[]{0, 6, 0, 0, 1, 2, 2, 3, 2, 3, 3, 6, 1, 2, 3, 2, 0, 1, 2, 3}, ts.positions);
		int minPos = ts.getMinSource(ts.root);
		assertEquals(4, minPos); // 4 => E 1 ; B 1 -> B 2
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 1, 2, 3, 2, 0, 1, 2, 3}, ts.positions);
		ts.getNextPosition(4); // E 1 -> E 2
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 1, 2, 3, 2, 2, 3, 4, 4}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(4, minPos); // 4 => E 2 ; D 1 -> D 2
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 3, 4, 4, 5, 2, 3, 4, 4}, ts.positions);
		ts.getNextPosition(4); // E 2 -> E 3
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 3, 4, 4, 5, 3, 4, 3, 6}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(4, minPos); // 4 => E 3 ; 
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 3, 4, 4, 5, 3, 4, 3, 6}, ts.positions);
		ts.getNextPosition(4); // E 3 -> E 4
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 3, 4, 4, 5, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(0, minPos); // 0 => A 1 ; D 2 -> D 3 
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(0); // A 1 -> A 2
		assertIntArray(new int[]{0, 2, 1, 15, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(1, minPos); // 1 => B 2 ; A 2 -> A 4
		assertIntArray(new int[]{4, 6, 1, 15, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(1); // B 2 -> B 3		
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(2, minPos); // 2 => C 1  
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(2); // C 1 -> C 2		
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(0, minPos); // 0 => A 4  
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(0); // A 4 -> A 5		
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(3, minPos); // 3 => D 3  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(3); // D 3 -> D 4	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 3, 13}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(4, minPos); // 4 => E 4  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 3, 13}, ts.positions);
		ts.getNextPosition(4); // E 4 -> E 5	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		assertMaxPosReached(new boolean[]{false, false, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(1, minPos); // 1 => B 3  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		ts.getNextPosition(1); // do nothing	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		assertMaxPosReached(new boolean[]{true, true, false, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(2, minPos); // 2 => C 2  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		ts.getNextPosition(2); // do nothing	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		assertMaxPosReached(new boolean[]{true, true, true, false, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(3, minPos); // 3 => D 4  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		ts.getNextPosition(3); // do nothing	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		assertMaxPosReached(new boolean[]{true, true, true, true, false}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(4, minPos); // 4 => E 5  
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		ts.getNextPosition(4); // do nothing	
		assertIntArray(new int[]{4, 5, 4, 10, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11}, ts.positions);
		assertMaxPosReached(new boolean[]{true, true, true, true, true}, ts);
		
		minPos = ts.getMinSource(ts.root);
		assertEquals(-1, minPos); //end
	}
	
	/**
	 * Tree with indexes:
	 * 
	 * (A  [0,6,0,0]
	 *    (A   [0,2,1,15]
	 *       (E   [0,1,2,3]
	 *          J)   [0,1,3,1]
	 *       (B   [1,2,2,3]
	 *          D)   [1,2,3,2]
	 *    )
	 *    (A   [2,4,1,15]
	 *       (B   [2,4,2,7]
	 *          (C   [2,3,3,6]
	 *             E)   [2,3,4,4]
	 *          (E   [3,4,3,6]
	 *             D)   [3,4,4,5]
	 *       )
	 *    )
	 *    (A   [4,6,1,15]
	 *       (D   [4,6,2,14]
	 *          (B   [4,5,3,13]
	 *             (A   [4,5,4,10]
	 *                (C   [4,5,5,9]
	 *                   J)   [4,5,6,8]
	 *             )
	 *          )
	 *          (E   [5,6,3,13]
	 *             (D   [5,6,4,12]
	 *                E)   [5,6,5,11]
	 *          )
	 *       )
	 *    )
	 * )
	 * 
	 * @throws Exception
	 */
	public void testPartialResultsGeneration() throws Exception {
		TwigStackJoin ts = new TwigStackJoin(new String[] { "A", "B", "C",
				"D", "E" }, new int[] { -1, 0, 1, 0, 3 }, getDescOp(5));
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		ts.nextDoc();
		
		ts.match();
		assertEquals(2, ts.partialResultsLists.size());
		assertTrue(ts.partialResultsLists.containsKey(2)); // C's results
		assertTrue(ts.partialResultsLists.containsKey(4)); // E's results
		
		List<int[]> cList = ts.partialResultsLists.get(2);
		assertEquals(3, cList.size()); // number of paths ending with C
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 0, 0, 0, 0, 0, 0, 0, 0}, cList.get(0));
		assertIntArray(new int[]{0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 0, 0, 0, 0, 0, 0, 0, 0}, cList.get(1));
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 0, 0, 0, 0, 0, 0, 0, 0}, cList.get(2));
		// Note that int[]{2, 4, 1, 5, 1, 2, 2, 3, 2, 3, 3, 6, 0, 0, 0, 0, 0, 0, 0, 0} is not in the result set.
		// This is because, A [2, 4, 1, 5] does not have the D - E arm of the query under it
		
		List<int[]> eList = ts.partialResultsLists.get(4);
		assertEquals(6, eList.size()); // number of paths ending with E
		assertIntArray(new int[]{0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 6, 2, 14, 5, 6, 3, 13 }, eList.get(0));
		assertIntArray(new int[]{0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 6, 2, 14, 5, 6, 5, 11 }, eList.get(1));
		assertIntArray(new int[]{0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 4, 12, 5, 6, 5, 11 }, eList.get(2));
		assertIntArray(new int[]{4, 6, 1, 15, 0, 0, 0, 0, 0, 0, 0, 0, 4, 6, 2, 14, 5, 6, 3, 13 }, eList.get(3));
		assertIntArray(new int[]{4, 6, 1, 15, 0, 0, 0, 0, 0, 0, 0, 0, 4, 6, 2, 14, 5, 6, 5, 11 }, eList.get(4));
		assertIntArray(new int[]{4, 6, 1, 15, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 4, 12, 5, 6, 5, 11 }, eList.get(5));
		
	}
	
	/**
	 * Query:
	 *         A
	 *         |
	 *         B
	 *        / \
	 *       C   G
	 *      /     \
	 *     D       H
	 *    / \       \
	 *   E   F       I
	 *              / \
	 *             J   L
	 *             |   |
	 *             K   M
	 *   
	 *  As list: A B C D E F G H I J K  L  M
	 *           0 1 2 3 4 5 6 7 8 9 10 11 12
	 *   
	 *  Partial results at: E, F, K, M (as positions: 4, 5, 10, 12)
	 *  
	 *  Intermediate merge nodes: B, D, I (as positions: 1, 3, 8) 
	 *  
	 * @throws Exception
	 */
	public void testMergeNodeCreation() throws Exception {
		TwigStackJoin ts = new TwigStackJoin(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M" }, 
				new int[] { -1, 0, 1, 2, 3, 3, 1, 6, 7, 8, 9, 8, 11}, getDescOp(13));
		IndexWriter w = setupIndex();
		// just ensure that the atomic context has all the terms in the query;
		w.addDocument(getDoc("(A(B(C D)(E F))(G(H(I J)(K L)(M N))))"));
		
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		
		assertNotNull(ts.rootMergeNode);
		
		// rootMergeNode is at B node
		assertEquals(1, ts.rootMergeNode.position);
		assertIntArray(new int[] { 0, 1 }, ts.rootMergeNode.ancSelfPos);
		assertEquals(2, ts.rootMergeNode.children.size());
		assertEquals(2, ts.rootMergeNode.descPos.size());
		assertIntArray(new int[] {2, 3, 4, 5}, ts.rootMergeNode.descPos.get(0));
		assertIntArray(new int[] {6, 7, 8, 9, 10, 11, 12}, ts.rootMergeNode.descPos.get(1));
		
		// D node
		MergeNode dNode = ts.rootMergeNode.children.get(0);
		assertEquals(3, dNode.position);
		assertIntArray(new int[] { 0, 1, 2, 3 }, dNode.ancSelfPos);
		assertEquals(2, dNode.children.size());
		assertEquals(2, dNode.descPos.size());
		assertIntArray(new int[] { 4 }, dNode.descPos.get(0));
		assertIntArray(new int[] { 5 }, dNode.descPos.get(1));
		
		// E node
		MergeNode eNode = dNode.children.get(0);
		assertEquals(4, eNode.position);
		assertIntArray(new int[] { 0, 1, 2, 3, 4 }, eNode.ancSelfPos);
		assertEquals(0, eNode.children.size());
		assertEquals(0, eNode.descPos.size());

		// F node
		MergeNode fNode = dNode.children.get(1);
		assertEquals(5, fNode.position);
		assertIntArray(new int[] { 0, 1, 2, 3, 5 }, fNode.ancSelfPos);
		assertEquals(0, fNode.children.size());
		assertEquals(0, fNode.descPos.size());

		// I node
		MergeNode iNode = ts.rootMergeNode.children.get(1);
		assertEquals(8, iNode.position);
		assertIntArray(new int[] { 0, 1, 6, 7, 8 }, iNode.ancSelfPos);
		assertEquals(2, iNode.children.size());
		assertEquals(2, iNode.descPos.size());
		assertIntArray(new int[] { 9, 10 }, iNode.descPos.get(0));
		assertIntArray(new int[] { 11, 12 }, iNode.descPos.get(1));
		
		// K node
		MergeNode kNode = iNode.children.get(0);
		assertEquals(10, kNode.position);
		assertIntArray(new int[] { 0, 1, 6, 7, 8, 9, 10 }, kNode.ancSelfPos);
		assertEquals(0, kNode.children.size());
		assertEquals(0, kNode.descPos.size());

		// M node
		MergeNode mNode = iNode.children.get(1);
		assertEquals(12, mNode.position);
		assertIntArray(new int[] { 0, 1, 6, 7, 8, 11, 12 }, mNode.ancSelfPos);
		assertEquals(0, mNode.children.size());
		assertEquals(0, mNode.descPos.size());
	}
	
	public void testMergingOfPaths() throws Exception {
		TwigStackJoin ts = new TwigStackJoin(new String[] { "A", "B", "C",
				"D", "E" }, new int[] { -1, 0, 1, 0, 3 }, getDescOp(5));
		IndexWriter w = setupIndex();
		w.addDocument(getDoc("(A(A(E J)(B D))(A(B(C E)(E D)))(A(D(B(A(C J)))(E(D E)))))")); // doc 0
		IndexReader r = commitIndexAndOpenReader(w);
		ts.setup(r);
		ts.nextDoc();
		
		List<int[]> results = ts.match();
		assertEquals(9, results.size());
		
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 3, 13}, results.get(0));
		assertIntArray(new int[]{0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13 }, results.get(1));
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 4, 6, 2, 14, 5, 6, 5, 11 }, results.get(2));
		assertIntArray(new int[]{0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 5, 11 }, results.get(3));
		assertIntArray(new int[]{0, 6, 0, 0, 2, 4, 2, 7, 2, 3, 3, 6, 5, 6, 4, 12, 5, 6, 5, 11 }, results.get(4));
		assertIntArray(new int[]{0, 6, 0, 0, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11 }, results.get(5));
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 3, 13 }, results.get(6));
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 4, 6, 2, 14, 5, 6, 5, 11 }, results.get(7));
		assertIntArray(new int[]{4, 6, 1, 15, 4, 5, 3, 13, 4, 5, 5, 9, 5, 6, 4, 12, 5, 6, 5, 11 }, results.get(8));
	}
	
	//TODO: write a test for merging for query with three branches
	public void xtestMergingThreeBranchQuery() throws Exception {
		
	}
	
	private Operator[] getDescOp(int size) {
		Operator[] r = new Operator[size];
		for (int i = 0 ; i < size; i++) {
			r[i] = CountingOp.DESCENDANT;
		}
		return r;
	}
	
	private void assertMaxPosReached(boolean[] expected, TwigStackJoin ts) {
		assertEquals("Incorrect maxPosReached length", expected.length, ts.maxPosReached.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Incorrect maxPosReached value at position " + i, expected[i], ts.maxPosReached[i]);
		}
	}
}
