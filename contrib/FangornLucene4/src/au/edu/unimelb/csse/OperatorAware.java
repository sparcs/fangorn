package au.edu.unimelb.csse;

public interface OperatorAware {
	boolean descendant(int[] prev, int poff, int[] next, int noff);
	boolean ancestor(int[] prev, int poff, int[] next, int noff);
	boolean child(int[] prev, int poff, int[] next, int noff);
	boolean parent(int[] prev, int poff, int[] next, int noff);
	boolean following(int[] prev, int poff, int[] next, int noff);
	boolean preceding(int[] prev, int poff, int[] next, int noff);
	boolean followingSibling(int[] prev, int poff, int[] next, int noff);
	boolean precedingSibling(int[] prev, int poff, int[] next, int noff);	
	boolean immediateFollowing(int[] prev, int poff, int[] next, int noff);
	boolean immediatePreceding(int[] prev, int poff, int[] next, int noff);
	boolean immediateFollowingSibling(int[] prev, int poff, int[] next, int noff);
	boolean immediatePrecedingSibling(int[] prev, int poff, int[] next, int noff);
	boolean same(int[] prev, int poff, int[] next, int noff);
	// equivalent to preceding || ancestor
	boolean startsBefore(int[] prev, int poff, int[] next, int noff);
	// equivalent to following || descendant
	boolean startsAfter(int[] prev, int poff, int[] next, int noff);
	int relativeDepth(int[] prev, int poff, int[] next, int noff);
	boolean isLeftAligned(int[] prev, int poff, int[] next, int noff);
	Operator mostRelevantOpRelation(int[] prev, int poff, int[] next, int noff);
	Position positionRelation(int[] prev, int poff, int[] next, int noff);
}