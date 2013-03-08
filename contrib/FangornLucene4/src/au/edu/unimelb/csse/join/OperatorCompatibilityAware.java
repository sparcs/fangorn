package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.BinaryOperator;

public interface OperatorCompatibilityAware {
	boolean check(BinaryOperator op);
}
