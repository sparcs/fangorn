package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionDecorator;
import au.edu.unimelb.csse.paypack.LRDP.PhysicalPayloadFormat;

public class MPMGModJoinTest extends PairJoinTestCase {
	MPMGModJoin join;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		join = new MPMGModJoin(lrdp);
	}

	public void testSkipsPrevAAsStopsAtNextAA() throws Exception {
		IndexReader r = setupIndexWithDocs("(SS(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(12, 12, join, prev, Operator.CHILD, posEnum);
	}

	// the next few tests compare MPMGMod with vanilla MPMG join

	@Test
	public void testNoResultsDesc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, join, prev, Operator.DESCENDANT, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testNoResultsChild() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(BB AA)(BB AA))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 5, join, prev, Operator.CHILD, posEnum);
		// 2 comparisons in MPMG
	}

	@Test
	public void testTree1Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(DD(AA DD)(AA CC)(AA CC))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 12);
		joinAndAssertOutput(4, 11, join, prev, Operator.DESCENDANT, posEnum);
		// 5 in MPMG
	}

	@Test
	public void testTree2Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 14, join, prev, Operator.DESCENDANT, posEnum);
		// 10 in MPMG
	}

	@Test
	public void testTree2Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(0, 20, join, prev, Operator.CHILD, posEnum);
		// 23 in MPMG
	}

	@Test
	public void testTree3Desc() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(24, 19, join, prev, Operator.DESCENDANT, posEnum);
		// 14 in MPMG
	}

	@Test
	public void testTree3Child() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(AA DD)(AA DD)(AA DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 16);
		joinAndAssertOutput(12, 22, join, prev, Operator.CHILD, posEnum);
		// 22 in MPMG
	}

	public void testResultsOrderedBy1stsPositions() throws Exception {
		IndexReader r = setupIndexWithDocs("(AA(CC DD)(AA(CC DD)(CC DD))(CC DD))");
		DocsAndPositionsEnum posEnum = initPrevGetNext(r, 8);
		joinAndAssertOutput(24, 14, join, prev, Operator.DESCENDANT, posEnum);

		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 0, 1, 2,
				1 }, 0, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 1, 2, 3,
				2 }, 1, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 2, 3, 3,
				3 }, 2, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 0, 4, 0, 0 }, new int[] { 3, 4, 2,
				5 }, 3, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 1, 2, 3,
				2 }, 4, lrdp.getPositionLength());
		assertNodePairPositions(new int[] { 1, 3, 1, 6 }, new int[] { 2, 3, 3,
				3 }, 5, lrdp.getPositionLength());
	}

	public void testLargeListsDoNotCauseOverflow() throws Exception {
		NodePositions prev = new NodePositions(new int[] { 0, 125, 2, 241, 0,
				2, 3, 236, 3, 4, 4, 8, 6, 14, 3, 236, 6, 9, 4, 24, 10, 14, 5,
				23, 10, 12, 6, 22, 13, 14, 7, 21, 15, 125, 3, 236, 15, 123, 4,
				235, 19, 22, 10, 35, 25, 29, 8, 52, 25, 26, 9, 51, 27, 29, 10,
				50, 31, 38, 8, 227, 31, 34, 9, 65, 35, 36, 10, 62, 39, 40, 10,
				225, 42, 106, 12, 190, 42, 44, 13, 189, 46, 49, 16, 90, 52, 54,
				19, 87, 58, 59, 16, 102, 59, 61, 16, 102, 66, 68, 17, 185, 69,
				70, 18, 116, 71, 104, 18, 180, 71, 73, 19, 179, 74, 104, 20,
				178, 74, 76, 21, 177, 77, 78, 23, 175, 80, 82, 25, 134, 85, 86,
				26, 171, 88, 91, 29, 156, 92, 93, 30, 150, 94, 96, 30, 155, 99,
				100, 29, 167, 101, 102, 30, 166, 105, 106, 18, 184, 107, 110,
				11, 224, 110, 117, 11, 224, 110, 112, 12, 210, 113, 117, 13,
				209, 113, 115, 14, 208, 116, 117, 15, 207, 119, 120, 14, 221,
				122, 123, 16, 219, 124, 125, 5, 234 });

		List<NodePositions> nodePositions = setupPositionsList();
		List<Integer> positions = new ArrayList<Integer>();
		positions.add(1);// dummy
		MPMGModJoin join = new MPMGModJoin(new LRDPMockPositions(LRDP.PhysicalPayloadFormat.BYTE1111, nodePositions.iterator(),
				positions.iterator()));
		join.match(prev, Operator.CHILD, new DocsAndPositionsEnumStub(25),
				result);

	}

	class DocsAndPositionsEnumStub extends DocsAndPositionsEnum {

		private int freq;

		public DocsAndPositionsEnumStub(int freq) {
			this.freq = freq;
		}

		@Override
		public int nextPosition() throws IOException {
			return 0;
		}

		@Override
		public int startOffset() throws IOException {
			return 0;
		}

		@Override
		public int endOffset() throws IOException {
			return 0;
		}

		@Override
		public BytesRef getPayload() throws IOException {
			return null;
		}

		@Override
		public int freq() throws IOException {
			return freq;
		}

		@Override
		public int docID() {
			return 0;
		}

		@Override
		public int nextDoc() throws IOException {
			return 0;
		}

		@Override
		public int advance(int target) throws IOException {
			return 0;
		}

	}

	private List<NodePositions> setupPositionsList() {
		List<NodePositions> list = new ArrayList<NodePositions>();
		int[][] pos = new int[][] { new int[] { 16, 22, 7, 38 },
				new int[] { 17, 22, 8, 37 }, new int[] { 24, 123, 6, 229 },
				new int[] { 24, 29, 7, 228 }, new int[] { 30, 123, 7, 228 },
				new int[] { 45, 62, 13, 189 }, new int[] { 45, 54, 14, 104 },
				new int[] { 49, 54, 16, 90 }, new int[] { 50, 54, 17, 89 },
				new int[] { 51, 54, 18, 88 }, new int[] { 56, 62, 14, 104 },
				new int[] { 57, 62, 15, 103 }, new int[] { 64, 106, 15, 187 },
				new int[] { 65, 106, 16, 186 }, new int[] { 78, 82, 23, 175 },
				new int[] { 83, 103, 23, 175 }, new int[] { 84, 103, 24, 173 },
				new int[] { 86, 102, 26, 171 }, new int[] { 86, 96, 27, 169 },
				new int[] { 87, 96, 28, 157 }, new int[] { 97, 102, 27, 169 },
				new int[] { 98, 102, 28, 168 },
				new int[] { 106, 123, 10, 225 },
				new int[] { 118, 123, 13, 222 }, new int[] { 125, 127, 2, 241 } };

		for (int i = 0; i < pos.length; i++) {
			list.add(new NodePositions(pos[i]));
		}
		return list;
	}

	class LRDPMockPositions extends LogicalNodePositionDecorator {
		Iterator<NodePositions> nodePositionIterator;
		Iterator<Integer> positionsIterator;

		public LRDPMockPositions(PhysicalPayloadFormat ppf,
				Iterator<NodePositions> nodePositionIterator,
				Iterator<Integer> positionsIterator) {
			super(new LRDP(ppf));
			this.nodePositionIterator = nodePositionIterator;
			this.positionsIterator = positionsIterator;
		}

		@Override
		public int getNextPosition(NodePositions buffer,
				DocsAndPositionsEnum node) throws IOException {
			if (nodePositionIterator.hasNext()) {
				NodePositions np = nodePositionIterator.next();
				buffer.push(np, LRDP.POSITION_LENGTH);
				if (positionsIterator.hasNext()) {
					return positionsIterator.next();
				}
				return 0;
			}
			return -1;
		}

	}
}
