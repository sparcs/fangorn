package au.edu.unimelb.csse.exp;

import java.util.Arrays;

import au.edu.unimelb.csse.Operator;

class TreeQuery {

	private String[] labels;
	private int[] parents;
	private Operator[] operators;

	public void setLabels(String... labels) {
		this.labels = labels;
	}

	public String[] labels() {
		return labels;
	}

	public void setParents(int... parents) {
		this.parents = parents;
	}

	public int[] parents() {
		return parents;
	}

	public void setOperators(Operator... operators) {
		this.operators = operators;
	}

	public Operator[] operators() {
		return operators;
	}

	boolean hasBranches() {
		int[] parents = this.parents.clone();
		Arrays.sort(parents);
		int prev = -1;
		if (parents.length > 0) {
			prev = parents[0];
		}
		for (int i = 1; i < parents.length; i++) {
			if (prev == parents[i]) {
				return true;
			}
			prev = parents[i];
		}
		return false;
	}
	
	boolean hasHorizontalOps() {
		for (Operator op: operators) {
			if (!(Operator.DESCENDANT.equals(op) || Operator.CHILD.equals(op) || Operator.ANCESTOR.equals(op) || Operator.PARENT.equals(op))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "[ labels:" + Arrays.toString(labels) + ", parentids:"
				+ Arrays.toString(parents) + ", operators:"
				+ Arrays.toString(operators) + "]";
	}
}
