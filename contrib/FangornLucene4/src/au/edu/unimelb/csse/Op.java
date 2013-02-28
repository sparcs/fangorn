package au.edu.unimelb.csse;

/**
 * Default implementation of Operator
 * 
 * @author sumukh
 * 
 */
public abstract class Op implements Operator {
	private final String name;

	public Op(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Operator:" + name;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Op) {
			Op o = (Op) obj;
			return this.name == o.name;
		} else if (obj instanceof CountingOp) {
			CountingOp o = (CountingOp) obj;
			return this.name == o.getName();
		}
		return false;
	};

	public int hashCode() {
		return this.name.hashCode();
	};

	public static Operator CHILD = new Op("child") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] <= next[noff + PayOff.LEFT]
					&& prev[poff + PayOff.RIGHT] >= next[noff + PayOff.RIGHT]
					&& prev[poff + PayOff.DEPTH] + 1 == next[noff
							+ PayOff.DEPTH];
		}

	};

	public static Operator DESCENDANT = new Op("descendant") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] <= next[noff + PayOff.LEFT]
					&& prev[poff + PayOff.RIGHT] >= next[noff + PayOff.RIGHT]
					&& prev[poff + PayOff.DEPTH] < next[noff + PayOff.DEPTH];
		}

	};

	public static Operator PARENT = new Op("parent") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return CHILD.match(next, noff, prev, poff);
		}

	};

	public static Operator ANCESTOR = new Op("ancestor") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return DESCENDANT.match(next, noff, prev, poff);
		}

	};

	public static Operator FOLLOWING = new Op("following") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.RIGHT] <= next[noff + PayOff.LEFT];
		}

	};

	public static Op PRECEDING = new Op("preceding") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] >= next[noff + PayOff.RIGHT];
		}

	};

	public static Operator IMMEDIATE_FOLLOWING = new Op("immediate-following") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.RIGHT] == next[noff + PayOff.LEFT];
		}

	};

	public static Op IMMEDIATE_PRECEDING = new Op("immediate-preceding") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] == next[noff + PayOff.RIGHT];
		}

	};

	public static Operator FOLLOWING_SIBLING = new Op("following-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.RIGHT] <= next[noff + PayOff.LEFT]
					&& prev[poff + PayOff.PARENT] == next[noff + PayOff.PARENT];
		}

	};

	public static Op PRECEDING_SIBLING = new Op("preceding-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] >= next[noff + PayOff.RIGHT]
					&& prev[poff + PayOff.PARENT] == next[noff + PayOff.PARENT];
		}

	};

	public static Operator IMMEDIATE_FOLLOWING_SIBLING = new Op(
			"immediate-following-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.RIGHT] == next[noff + PayOff.LEFT]
					&& prev[poff + PayOff.PARENT] == next[noff + PayOff.PARENT];
		}

	};

	public static Op IMMEDIATE_PRECEDING_SIBLING = new Op(
			"immediate-preceding-sibling") {

		@Override
		public boolean match(int[] prev, int poff, int[] next, int noff) {
			return prev[poff + PayOff.LEFT] == next[noff + PayOff.RIGHT]
					&& prev[poff + PayOff.PARENT] == next[noff + PayOff.PARENT];
		}

	};
}
