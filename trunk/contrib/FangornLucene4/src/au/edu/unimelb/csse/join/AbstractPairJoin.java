package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.LogicalNodePositionAware;

abstract class AbstractPairJoin implements OperatorCompatibilityAware {

	protected final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final BinaryOperatorAware operatorAware;

	AbstractPairJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		this.positionLength = nodePositionAware.getPositionLength();
		this.operatorAware = nodePositionAware.getBinaryOperatorHandler();
	}
	
	@Override
	public boolean check(BinaryOperator op) {
		return op.equals(BinaryOperator.CHILD)
				|| op.equals(BinaryOperator.DESCENDANT);
	}
}
