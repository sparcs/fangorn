package au.edu.unimelb.csse;

/**
 * Counts the number of times an operator comparison is made
 * 
 * Only for testing not to be used on production
 * 
 * @author sumukh
 * 
 */
public class CountingOp implements Operator {
	public static CountingOp CHILD = new CountingOp(Op.CHILD);
	public static CountingOp DESCENDANT = new CountingOp(Op.DESCENDANT);
	public static CountingOp PARENT = new CountingOp(Op.PARENT);
	public static CountingOp ANCESTOR = new CountingOp(Op.ANCESTOR);
	public static CountingOp FOLLOWING = new CountingOp(Op.FOLLOWING);
	public static CountingOp PRECEDING = new CountingOp(Op.PRECEDING);

	private Operator op;
	private long count = 0;

	public CountingOp(Operator op) {
		this.op = op;
	}

	/**
	 * Synchronisation of the count increment is not necessary as CountingOp is
	 * only used in tests
	 */
	@Override
	public boolean match(int[] prev, int poff, int[] next, int noff) {
		count++;
		return op.match(prev, poff, next, noff);
	}

	public long getCount() {
		return count;
	}

	// Equals and hashcode mimic the Op object

	@Override
	public boolean equals(Object other) {
		if (other instanceof CountingOp) {
			other = ((CountingOp) other).op;
		}
		return op.equals(other);
	}

	@Override
	public int hashCode() {
		return op.hashCode();
	}

	@Override
	public String getName() {
		return op.getName();
	}
}
