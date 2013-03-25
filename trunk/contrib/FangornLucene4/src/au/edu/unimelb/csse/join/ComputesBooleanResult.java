package au.edu.unimelb.csse.join;

import java.io.IOException;

public interface ComputesBooleanResult extends IndexDocumentAware {

	boolean match() throws IOException;

}
