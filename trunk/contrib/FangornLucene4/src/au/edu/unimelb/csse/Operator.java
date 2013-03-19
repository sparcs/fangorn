package au.edu.unimelb.csse;

import au.edu.unimelb.csse.join.NodePositions;

public interface Operator {
	boolean match(NodePositions prev, NodePositions next,
			OperatorAware aware);

	boolean match(int[] prev, int poff, int[] next, int noff,
			OperatorAware aware);

	String name();

	Operator DESCENDANT = new AbstractBinaryOperator("descendant") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.descendant(prev, poff, next, noff);
		}

	};

	Operator ANCESTOR = new AbstractBinaryOperator("ancestor") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.ancestor(prev, poff, next, noff);
		}

	};

	Operator CHILD = new AbstractBinaryOperator("child") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.child(prev, poff, next, noff);
		}

	};

	Operator PARENT = new AbstractBinaryOperator("parent") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.parent(prev, poff, next, noff);
		}

	};

	Operator FOLLOWING = new AbstractBinaryOperator("following") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.following(prev, poff, next, noff);
		}

	};

	Operator PRECEDING = new AbstractBinaryOperator("preceding") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.preceding(prev, poff, next, noff);
		}

	};

	Operator FOLLOWING_SIBLING = new AbstractBinaryOperator(
			"following-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.followingSibling(prev, poff, next, noff);
		}

	};

	Operator PRECEDING_SIBLING = new AbstractBinaryOperator(
			"preceding-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.precedingSibling(prev, poff, next, noff);
		}

	};

	Operator IMMEDIATE_FOLLOWING = new AbstractBinaryOperator(
			"immediate-following") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.immediateFollowing(prev, poff, next, noff);
		}

	};

	Operator IMMEDIATE_PRECEDING = new AbstractBinaryOperator(
			"immediate-preceding") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.immediatePreceding(prev, poff, next, noff);
		}

	};

	Operator IMMEDIATE_FOLLOWING_SIBLING = new AbstractBinaryOperator(
			"immediate-following-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.immediateFollowingSibling(prev, poff, next, noff);
		}

	};

	Operator IMMEDIATE_PRECEDING_SIBLING = new AbstractBinaryOperator(
			"immediate-preceding-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next,
				int noff, OperatorAware opAware) {
			return opAware.immediatePrecedingSibling(prev, poff, next, noff);
		}

	};

}

abstract class AbstractBinaryOperator implements Operator {

	private String name;

	public AbstractBinaryOperator(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public boolean match(NodePositions prev, NodePositions next,
			OperatorAware aware) {
		return match(prev.positions, prev.offset, next.positions, next.offset, aware);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Operator) {
			Operator o = (Operator) obj;
			return this.name == o.name();
		}
		return false;
	};

	@Override
	public int hashCode() {
		return this.name.hashCode();
	};

	@Override
	public String toString() {
		return name;
	}
}