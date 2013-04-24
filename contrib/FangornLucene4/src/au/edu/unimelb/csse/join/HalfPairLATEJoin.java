package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;

public interface HalfPairLATEJoin extends HalfPairJoin {
	
	NodePositions matchWithLookahead(NodePositions prev, Operator op,
			DocsAndPositionsEnum node, Operator nextOp) throws IOException;

	NodePositions matchTerminateEarly(NodePositions prev, Operator op,
			DocsAndPositionsEnum node) throws IOException;
}
