package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.BinaryOperator;

public interface PairJoin {
	int numBuffers(BinaryOperator op);
}
