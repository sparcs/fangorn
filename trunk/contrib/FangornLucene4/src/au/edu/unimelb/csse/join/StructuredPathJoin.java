package au.edu.unimelb.csse.join;

import java.io.IOException;

import au.edu.unimelb.csse.BinaryOperator;

public class StructuredPathJoin extends AbstractJoin {

	public StructuredPathJoin(String[] labels, int[] parentPos,
			BinaryOperator[] operators, Class<? extends AbstractPairJoin> cl)
			throws InstantiationException, IllegalAccessException {
		super(labels, parentPos, operators);
		cl.newInstance();
	}

	@Override
	public void setupPerDoc() throws IOException {
		// possible place to perform document level reorganisation for
		// optimisation?
	}

}
