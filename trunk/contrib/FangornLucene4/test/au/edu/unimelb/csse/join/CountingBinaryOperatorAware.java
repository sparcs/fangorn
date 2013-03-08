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
	public boolean descendant(NodePositions prev, NodePositions next) {
		count++;
		return inner.descendant(prev, next);
	}

	@Override
	public boolean ancestor(NodePositions prev, NodePositions next) {
		count++;
		return inner.ancestor(prev, next);
	}

	@Override
	public boolean child(NodePositions prev, NodePositions next) {
		count++;
		return inner.child(prev, next);
	}

	@Override
	public boolean parent(NodePositions prev, NodePositions next) {
		count++;
		return inner.parent(prev, next);
	}

	@Override
	public boolean following(NodePositions prev, NodePositions next) {
		count++;
		return inner.following(prev, next);
	}

	@Override
	public boolean preceding(NodePositions prev, NodePositions next) {
		count++;
		return inner.preceding(prev, next);
	}

	@Override
	public boolean followingSibling(NodePositions prev, NodePositions next) {
		count++;
		return inner.followingSibling(prev, next);
	}

	@Override
	public boolean precedingSibling(NodePositions prev, NodePositions next) {
		count++;
		return inner.precedingSibling(prev, next);
	}

	@Override
	public boolean immediateFollowing(NodePositions prev, NodePositions next) {
		count++;
		return inner.immediateFollowing(prev, next);
	}

	@Override
	public boolean immediatePreceding(NodePositions prev, NodePositions next) {
		count++;
		return inner.immediatePreceding(prev, next);
	}

	@Override
	public boolean immediateFollowingSibling(NodePositions prev,
			NodePositions next) {
		count++;
		return inner.immediateFollowingSibling(prev, next);
	}

	@Override
	public boolean immediatePrecedingSibling(NodePositions prev,
			NodePositions next) {
		count++;
		return inner.immediatePrecedingSibling(prev, next);
	}

	@Override
	public boolean startsBefore(NodePositions prev, NodePositions next) {
		count++;
		return inner.startsBefore(prev, next);
	}

	@Override
	public boolean startsAfter(NodePositions prev, NodePositions next) {
		count++;
		return inner.startsAfter(prev, next);
	}

}
