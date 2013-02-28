package au.edu.unimelb.csse;

import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;

public class OpTest extends TestCase {

	@Test
	public void testChild() {
		Operator op = Op.CHILD;
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
		Operator o = Op.DESCENDANT;
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
		Operator o = Op.FOLLOWING;
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
		Operator o = Op.PRECEDING;
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
		Operator o = Op.IMMEDIATE_FOLLOWING;
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
		Operator o = Op.IMMEDIATE_PRECEDING;
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
		Operator o = Op.FOLLOWING_SIBLING;
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
		Operator o = Op.PRECEDING_SIBLING;
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
		Operator o = Op.IMMEDIATE_FOLLOWING_SIBLING;
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
		Operator o = Op.IMMEDIATE_PRECEDING_SIBLING;
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
		assertTrue("Expected " + subArrStr(next, noff) + " as " + op.getName()
				+ " of " + subArrStr(prev, poff),
				op.match(prev, poff, next, noff));
	}

	// pre does not match next for operator op
	private void assertNoMat(Operator op, int[] prev, int poff, int[] next,
			int noff) {
		assertFalse(
				"Expected " + subArrStr(next, noff) + " as not " + op.getName()
						+ " of " + subArrStr(prev, poff),
				op.match(prev, poff, next, noff));
	}

	private String subArrStr(int[] a, int off) {
		return Arrays.toString(Arrays.copyOfRange(a, off, off + 4));
	}

}
