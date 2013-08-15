package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// for performance testing only
public class DocIter extends AbstractJoin {
	Map<Integer, Integer> pfIndex = new HashMap<Integer, Integer>();

	public DocIter(String[] labels) {
		super(labels, null);
	}

	@Override
	public void setupPerDoc() throws IOException {
		// TODO Auto-generated method stub
	}
	
	public int getTermFreq(int idx) throws IOException {
		return postingsFreqs[pfIndex.get(idx)].postings.freq();
	}
	
	@Override
	public void setupPerAtomicContext() {
		super.setupPerAtomicContext();
		pfIndex.clear();
		for (int i = 0; i < postingsFreqs.length; i++) {
			pfIndex.put(postingsFreqs[i].position, i);
		}
	}

}
