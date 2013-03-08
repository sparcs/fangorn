package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.BinaryOperatorAware;

public class CountingBinaryOperatorAware implements BinaryOperatorAware {
	private BinaryOperatorAware inner;
	private int count;

	public CountingBinaryOperatorAware(BinaryOperatorAware inner) {
		this.inner = inner;
		count = 0;
	}

	public int getCount() {
		return count;
	}

	@Override
	public boolean descendant(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.descendant(prev, poff, next, noff);
	}

	@Override
	public boolean ancestor(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.ancestor(prev, poff, next, noff);
	}

	@Override
	public boolean child(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.child(prev, poff, next, noff);
	}

	@Override
	public boolean parent(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.parent(prev, poff, next, noff);
	}

	@Override
	public boolean following(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.following(prev, poff, next, noff);
	}

	@Override
	public boolean preceding(int[] prev, int poff, int[] next,
			int noff) {
		count++;
		return inner.preceding(prev, poff, next, noff);
	}

	@Override
	public boolean followingSibling(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.followingSibling(prev, poff, next, noff);
	}

	@Override
	public boolean precedingSibling(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.precedingSibling(prev, poff, next, noff);
	}

	@Override
	public boolean immediateFollowing(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.immediateFollowing(prev, poff, next, noff);
	}

	@Override
	public boolean immediatePreceding(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.immediatePreceding(prev, poff, next, noff);
	}

	@Override
	public boolean immediateFollowingSibling(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.immediateFollowingSibling(prev, poff, next, noff);
	}

	@Override
	public boolean immediatePrecedingSibling(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.immediatePrecedingSibling(prev, poff, next, noff);
	}

	@Override
	public boolean startsBefore(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.startsBefore(prev, poff, next, noff);
	}

	@Override
	public boolean startsAfter(int[] prev, int poff,
			int[] next, int noff) {
		count++;
		return inner.startsAfter(prev, poff, next, noff);
	}

}
