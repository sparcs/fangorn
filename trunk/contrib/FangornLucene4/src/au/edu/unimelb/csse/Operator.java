package au.edu.unimelb.csse;

public interface Operator {

	boolean match(int[] prev, int poff, int[] next, int noff);
	
	String getName();
};
