package au.edu.unimelb.csse.join;

import java.util.HashMap;
import java.util.Map;

import au.edu.unimelb.csse.BinaryOperator;

class BinaryOperatorInverse {
	private static final Map<BinaryOperator, BinaryOperator> inv = new HashMap<BinaryOperator, BinaryOperator>();
	static {
		inv.put(BinaryOperator.ANCESTOR, BinaryOperator.DESCENDANT);
		inv.put(BinaryOperator.PARENT, BinaryOperator.CHILD);
		inv.put(BinaryOperator.DESCENDANT, BinaryOperator.ANCESTOR);
		inv.put(BinaryOperator.CHILD, BinaryOperator.PARENT);
		inv.put(BinaryOperator.FOLLOWING, BinaryOperator.PRECEDING);
		inv.put(BinaryOperator.FOLLOWING_SIBLING, BinaryOperator.PRECEDING_SIBLING);
		inv.put(BinaryOperator.IMMEDIATE_FOLLOWING, BinaryOperator.IMMEDIATE_PRECEDING);
		inv.put(BinaryOperator.IMMEDIATE_FOLLOWING_SIBLING, BinaryOperator.IMMEDIATE_PRECEDING_SIBLING);
		inv.put(BinaryOperator.PRECEDING, BinaryOperator.FOLLOWING);
		inv.put(BinaryOperator.PRECEDING_SIBLING, BinaryOperator.FOLLOWING_SIBLING);
		inv.put(BinaryOperator.IMMEDIATE_PRECEDING, BinaryOperator.IMMEDIATE_FOLLOWING);
		inv.put(BinaryOperator.IMMEDIATE_PRECEDING_SIBLING, BinaryOperator.IMMEDIATE_FOLLOWING_SIBLING);
	}
	
	static BinaryOperator get(BinaryOperator op) {
		return inv.get(op);
	}
}
