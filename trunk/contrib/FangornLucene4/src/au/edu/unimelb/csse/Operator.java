package au.edu.unimelb.csse;

import au.edu.unimelb.csse.join.NodePositions;

public enum Operator {

	DESCENDANT {
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.descendant(prev, poff, next, noff);
		}
	},
	ANCESTOR {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.ancestor(prev, poff, next, noff);
		}
	},
	CHILD {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.child(prev, poff, next, noff);
		}
	},
	PARENT {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.parent(prev, poff, next, noff);
		}
	},
	FOLLOWING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.following(prev, poff, next, noff);
		}
	},
	PRECEDING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.preceding(prev, poff, next, noff);
		}
	},
	FOLLOWING_SIBLING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.followingSibling(prev, poff, next, noff);
		}
	},
	PRECEDING_SIBLING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.precedingSibling(prev, poff, next, noff);
		}
	},
	IMMEDIATE_FOLLOWING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediateFollowing(prev, poff, next, noff);
		}
	},
	IMMEDIATE_PRECEDING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediatePreceding(prev, poff, next, noff);
		}
	},
	IMMEDIATE_FOLLOWING_SIBLING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediateFollowingSibling(prev, poff, next, noff);
		}
	},
	IMMEDIATE_PRECEDING_SIBLING {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediatePrecedingSibling(prev, poff, next, noff);
		}
	};

	public boolean match(NodePositions prev, NodePositions next, OperatorAware aware) {
		return this.match(prev.positions, prev.offset, next.positions,
				next.offset, aware);
	}

	public abstract boolean match(int[] prev, int poff, int[] next, int noff,
			OperatorAware aware);

}