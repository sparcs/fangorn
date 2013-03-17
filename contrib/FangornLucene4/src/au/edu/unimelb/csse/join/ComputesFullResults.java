package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.List;

public interface ComputesFullResults {

	List<int[]> match() throws IOException;

}
