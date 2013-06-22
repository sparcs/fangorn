package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public interface JoinBuilder {
	HalfPairJoin getHalfPairJoin(Operator op, LogicalNodePositionAware nodePositionAware);
}

interface LATEJoinBuilder extends JoinBuilder {
	HalfPairLATEJoin getHalfPairJoin(Operator op, LogicalNodePositionAware nodePositionAware);
}
