package au.edu.unimelb.csse;

import au.edu.unimelb.csse.join.NodePositions;

public interface BinaryOperator {
	boolean match(NodePositions prev, NodePositions next,
			BinaryOperatorAware aware);
	
	String name();

	BinaryOperator DESCENDANT = new AbstractBinaryOperator("descendant") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.descendant(prev, next);
		}

	};

	BinaryOperator ANCESTOR = new AbstractBinaryOperator("ancestor") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.ancestor(prev, next);
		}

	};

	BinaryOperator CHILD = new AbstractBinaryOperator("child") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.child(prev, next);
		}

	};

	BinaryOperator PARENT = new AbstractBinaryOperator("parent") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.parent(prev, next);
		}

	};

	BinaryOperator FOLLOWING = new AbstractBinaryOperator("following") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.following(prev, next);
		}

	};

	BinaryOperator PRECEDING = new AbstractBinaryOperator("preceding") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.preceding(prev, next);
		}

	};

	BinaryOperator FOLLOWING_SIBLING = new AbstractBinaryOperator("following-sibling") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.followingSibling(prev, next);
		}

	};

	BinaryOperator PRECEDING_SIBLING = new AbstractBinaryOperator("preceding-sibling") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.precedingSibling(prev, next);
		}

	};

	BinaryOperator IMMEDIATE_FOLLOWING = new AbstractBinaryOperator("immediate-following") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.immediateFollowing(prev, next);
		}

	};

	BinaryOperator IMMEDIATE_PRECEDING = new AbstractBinaryOperator("immediate-preceding") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.immediatePreceding(prev, next);
		}

	};

	BinaryOperator IMMEDIATE_FOLLOWING_SIBLING = new AbstractBinaryOperator("immediate-following-sibling") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.immediateFollowingSibling(prev, next);
		}

	};

	BinaryOperator IMMEDIATE_PRECEDING_SIBLING = new AbstractBinaryOperator("immediate-preceding-sibling") {

		@Override
		public boolean match(NodePositions prev, NodePositions next,
				BinaryOperatorAware opAware) {
			return opAware.immediatePrecedingSibling(prev, next);
		}

	};

}

abstract class AbstractBinaryOperator implements BinaryOperator {
	
	private String name;

	public AbstractBinaryOperator(String name) {
		this.name = name;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BinaryOperator) {
			BinaryOperator o = (BinaryOperator) obj;
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