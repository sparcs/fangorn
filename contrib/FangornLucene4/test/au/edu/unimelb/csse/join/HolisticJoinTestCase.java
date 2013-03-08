package au.edu.unimelb.csse.join;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.IndexTestCase;
import au.edu.unimelb.csse.LRDP;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.BytePacking;

public abstract class HolisticJoinTestCase extends IndexTestCase {
	protected LRDP lrdp = new LRDP(new BytePacking(4));

	/**
	 * PostingsAndFreq array should be sorted first by docFreq and then by their
	 * position in the query. See
	 * {@link PostingsAndFreq#compareTo(PostingsAndFreq)} for more details
	 * 
	 * @param labels
	 *            list of assertion labels
	 * @param test
	 */
	public void assertPfArrayPos(String[] labels, PostingsAndFreq[] test) {
		assertEquals("Incorrect number of PostingsAndFreq elements",
				labels.length, test.length);
		for (int i = 0; i < labels.length; i++) {
			assertEquals("Incorrect PostingsAndFreq term at position " + i,
					labels[i], test[i].term.text());
		}
	}
	
	protected BinaryOperator[] getDescOp(int size) {
		BinaryOperator[] r = new BinaryOperator[size];
		for (int i = 0 ; i < size; i++) {
			r[i] = BinaryOperator.DESCENDANT;
		}
		return r;
	}
}
