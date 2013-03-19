package au.edu.unimelb.csse;

import java.util.HashMap;
import java.util.Map;


public class OperatorInverse {
	private static final Map<Operator, Operator> inv = new HashMap<Operator, Operator>();
	static {
		inv.put(Operator.ANCESTOR, Operator.DESCENDANT);
		inv.put(Operator.PARENT, Operator.CHILD);
		inv.put(Operator.DESCENDANT, Operator.ANCESTOR);
		inv.put(Operator.CHILD, Operator.PARENT);
		inv.put(Operator.FOLLOWING, Operator.PRECEDING);
		inv.put(Operator.FOLLOWING_SIBLING, Operator.PRECEDING_SIBLING);
		inv.put(Operator.IMMEDIATE_FOLLOWING, Operator.IMMEDIATE_PRECEDING);
		inv.put(Operator.IMMEDIATE_FOLLOWING_SIBLING, Operator.IMMEDIATE_PRECEDING_SIBLING);
		inv.put(Operator.PRECEDING, Operator.FOLLOWING);
		inv.put(Operator.PRECEDING_SIBLING, Operator.FOLLOWING_SIBLING);
		inv.put(Operator.IMMEDIATE_PRECEDING, Operator.IMMEDIATE_FOLLOWING);
		inv.put(Operator.IMMEDIATE_PRECEDING_SIBLING, Operator.IMMEDIATE_FOLLOWING_SIBLING);
	}
	
	public static Operator get(Operator op) {
		return inv.get(op);
	}
}
