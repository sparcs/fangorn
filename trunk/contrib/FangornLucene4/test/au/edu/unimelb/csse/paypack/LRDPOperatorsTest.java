package au.edu.unimelb.csse.paypack;

import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.join.NodePositions;

public class LRDPOperatorsTest extends TestCase {
	private LRDP lrdp = new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111);
	private OperatorAware opAware = lrdp.getBinaryOperatorHandler();

	@Test
	public void testChild() {
		Operator op = Operator.CHILD;
		int poff = 0;
		int noff = 0;
		int[] next = new int[] { 0, 1, 1, 5 };
		int[] prev = new int[] { 0, 4, 0, 0, 2, 3, 2, 4 };
		assertMatch(op, prev, poff, next, noff);
		// non-zero prev off
		assertNoMat(op, prev, 4, next, 0);
		next = new int[] { 0, 1, 0, 3, 0, 1, 1, 5 };
		// non-zero next off
		assertMatch(op, prev, poff, next, 4);
	}

	@Test
	public void testDescendant() throws Exception {
		Operator o = Operator.DESCENDANT;
		// child
		assertMatch(o, new int[] { 0, 2, 0, 0 }, 0, new int[] { 1, 2, 1, 4 }, 0);
		// desc
		assertMatch(o, new int[] { 0, 2, 0, 0 }, 0, new int[] { 1, 2, 2, 3 }, 0);
		// non-zero next off
		assertMatch(o, new int[] { 0, 2, 0, 0 }, 0, new int[] { 0, 1, 1, 2, 1,
				2, 2, 3 }, 4);
		// non-zero prev off
		assertMatch(o, new int[] { 0, 2, 0, 0, 0, 1, 1, 2 }, 4, new int[] { 0,
				1, 3, 3, 1, 2, 2, 4 }, 0);
		// after
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// before
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);

	}

	public void testFollowing() throws Exception {
		Operator o = Operator.FOLLOWING;
		// immediately after
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// after
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 4, 5, 2, 9 }, 0);
		// before
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 1, 2, 1, 2 }, 4, new int[] { 4,
				5, 2, 9 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 4,
				5, 2, 9 }, 4);
	}

	public void testPreceding() throws Exception {
		Operator o = Operator.PRECEDING;
		// after
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// before
		assertMatch(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 2, 3, 2, 4 }, 4, new int[] { 1,
				2, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 0, 0, 0, 0, 1,
				2, 1, 2 }, 4);
	}

	public void testImmediateFollowing() throws Exception {
		Operator o = Operator.IMMEDIATE_FOLLOWING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertNoMat(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertNoMat(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 1, 2, 1, 2 }, 4, new int[] { 2,
				3, 2, 4 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 2,
				3, 2, 4 }, 4);
	}

	public void testImmediatePreceding() throws Exception {
		Operator o = Operator.IMMEDIATE_PRECEDING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertNoMat(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertMatch(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertMatch(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 2, 3, 2, 4 }, 4, new int[] { 1,
				2, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 0, 0, 0, 0, 1,
				2, 1, 2 }, 4);
	}

	public void testFollowingSibling() throws Exception {
		Operator o = Operator.FOLLOWING_SIBLING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertNoMat(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertNoMat(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 1, 2, 1, 2 }, 4, new int[] { 3,
				4, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 3,
				4, 1, 2 }, 4);
	}

	public void testPrecedingSibling() throws Exception {
		Operator o = Operator.PRECEDING_SIBLING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertMatch(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertMatch(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 3, 4, 1, 2 }, 4, new int[] { 1,
				2, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 1,
				2, 1, 2 }, 4);
	}

	public void testImmediateFollowingSibling() throws Exception {
		Operator o = Operator.IMMEDIATE_FOLLOWING_SIBLING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertNoMat(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertNoMat(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 1, 2, 1, 2 }, 4, new int[] { 2,
				3, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 2,
				3, 1, 2 }, 4);
	}

	public void testImmediatePrecedingSibling() throws Exception {
		Operator o = Operator.IMMEDIATE_PRECEDING_SIBLING;
		// following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 2, 4 }, 0);
		// following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 3, 4, 1, 2 }, 0);
		// immediately following
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 2, 4 }, 0);
		// immediately following sibling
		assertNoMat(o, new int[] { 1, 2, 1, 2 }, 0, new int[] { 2, 3, 1, 2 }, 0);
		// preceding
		assertNoMat(o, new int[] { 3, 4, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// preceding sibling
		assertNoMat(o, new int[] { 3, 4, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding
		assertNoMat(o, new int[] { 2, 3, 2, 4 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// immediate preceding sibling
		assertMatch(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 1, 2, 1, 2 }, 0);
		// ancestor
		assertNoMat(o, new int[] { 1, 2, 2, 3 }, 0, new int[] { 0, 2, 0, 0 }, 0);
		// descendant
		assertNoMat(o, new int[] { 1, 4, 1, 2 }, 0, new int[] { 2, 4, 3, 7 }, 0);
		// non-zero prev offset
		assertMatch(o, new int[] { 0, 0, 0, 0, 2, 3, 1, 2 }, 4, new int[] { 1,
				2, 1, 2 }, 0);
		// non-zero next offset
		assertMatch(o, new int[] { 2, 3, 1, 2 }, 0, new int[] { 0, 0, 0, 0, 1,
				2, 1, 2 }, 4);
	}

	// pre matches next for operator op
	private void assertMatch(Operator op, int[] prev, int poff, int[] next,
			int noff) {
		assertTrue("Expected " + subArrStr(next, noff) + " as " + op.name()
				+ " of " + subArrStr(prev, poff),
				op.match(prev, poff, next, noff, opAware));
		
		NodePositions p = getNodePositions(prev, poff);
		NodePositions n = getNodePositions(next, noff);
		assertTrue("Expected " + subArrStr(next, noff) + " as " + op.name()
				+ " of " + subArrStr(prev, poff),
				op.match(p, n, opAware));
	}

	private NodePositions getNodePositions(int[] prev, int poff) {
		NodePositions p = new NodePositions();
		System.arraycopy(prev, 0, p.positions, 0, prev.length);
		p.size = prev.length;
		p.offset = poff;
		return p;
	}

	// pre does not match next for operator op
	private void assertNoMat(Operator op, int[] prev, int poff, int[] next,
			int noff) {
		assertFalse(
				"Expected " + subArrStr(next, noff) + " as not " + op.name()
						+ " of " + subArrStr(prev, poff),
				op.match(prev, poff, next, noff, opAware));
		
		NodePositions p = getNodePositions(prev, poff);
		NodePositions n = getNodePositions(next, noff);
		
		assertFalse(
				"Expected " + subArrStr(next, noff) + " as not " + op.name()
						+ " of " + subArrStr(prev, poff),
				op.match(p, n, opAware));
		
	}

	private String subArrStr(int[] a, int off) {
		return Arrays.toString(Arrays.copyOfRange(a, off, off + 4));
	}

}
