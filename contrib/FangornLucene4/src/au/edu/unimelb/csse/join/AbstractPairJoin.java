package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.OperatorCompatibilityAware;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

abstract class AbstractPairJoin implements OperatorCompatibilityAware {

	protected final LogicalNodePositionAware nodePositionAware;
	protected final int positionLength;
	protected final OperatorAware operatorAware;

	AbstractPairJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		this.positionLength = nodePositionAware.getPositionLength();
		this.operatorAware = nodePositionAware.getOperatorHandler();
	}
	
	@Override
	public boolean check(Operator op) {
		return op.equals(Operator.CHILD)
				|| op.equals(Operator.DESCENDANT);
	}
}
