package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;

public interface BooleanJoinPipeline {

	public Pipe createExecPipeline(PostingsAndFreq pfRoot, Operator[] operators);

	interface Pipe {

		NodePositions execute() throws IOException;

		void setNext(Pipe pipe);

		Pipe getNext();

		Pipe getStart();

	}

	public void setPrevBuffer(NodePositions prev);
}
