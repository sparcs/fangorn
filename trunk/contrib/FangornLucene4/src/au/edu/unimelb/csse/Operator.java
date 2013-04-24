package au.edu.unimelb.csse;

import au.edu.unimelb.csse.join.NodePositions;

public enum Operator {

	DESCENDANT(Position.BELOW) {
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.descendant(prev, poff, next, noff);
		}
	},
	ANCESTOR(Position.ABOVE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.ancestor(prev, poff, next, noff);
		}
	},
	CHILD(Position.BELOW) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.child(prev, poff, next, noff);
		}
	},
	PARENT(Position.ABOVE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.parent(prev, poff, next, noff);
		}
	},
	FOLLOWING(Position.AFTER) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.following(prev, poff, next, noff);
		}
	},
	PRECEDING(Position.BEFORE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.preceding(prev, poff, next, noff);
		}
	},
	FOLLOWING_SIBLING(Position.AFTER) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.followingSibling(prev, poff, next, noff);
		}
	},
	PRECEDING_SIBLING(Position.BEFORE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.precedingSibling(prev, poff, next, noff);
		}
	},
	IMMEDIATE_FOLLOWING(Position.AFTER) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediateFollowing(prev, poff, next, noff);
		}
	},
	IMMEDIATE_PRECEDING(Position.BEFORE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediatePreceding(prev, poff, next, noff);
		}
	},
	IMMEDIATE_FOLLOWING_SIBLING(Position.AFTER) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediateFollowingSibling(prev, poff, next, noff);
		}
	},
	IMMEDIATE_PRECEDING_SIBLING(Position.BEFORE) {
		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware opAware) {
			return opAware.immediatePrecedingSibling(prev, poff, next, noff);
		}
	}, SAME(Position.SAME) {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff,
				OperatorAware aware) {
			return aware.same(prev, poff, next, noff);
		}
		
	};

	private Position position;

	Operator(Position position) {
		this.position = position;
	}

	public boolean match(NodePositions prev, NodePositions next,
			OperatorAware aware) {
		return this.match(prev.positions, prev.offset, next.positions,
				next.offset, aware);
	}

	public Position getPosition() {
		return position;
	}

	public abstract boolean match(int[] prev, int poff, int[] next, int noff,
			OperatorAware aware);

}