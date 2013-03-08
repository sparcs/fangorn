package au.edu.unimelb.csse;

import au.edu.unimelb.csse.join.NodePositions;

public interface BinaryOperatorAware {
	boolean descendant(NodePositions prev, NodePositions next);
	boolean ancestor(NodePositions prev, NodePositions next);
	boolean child(NodePositions prev, NodePositions next);
	boolean parent(NodePositions prev, NodePositions next);
	boolean following(NodePositions prev, NodePositions next);
	boolean preceding(NodePositions prev, NodePositions next);
	boolean followingSibling(NodePositions prev, NodePositions next);
	boolean precedingSibling(NodePositions prev, NodePositions next);	
	boolean immediateFollowing(NodePositions prev, NodePositions next);
	boolean immediatePreceding(NodePositions prev, NodePositions next);
	boolean immediateFollowingSibling(NodePositions prev, NodePositions next);
	boolean immediatePrecedingSibling(NodePositions prev, NodePositions next);
	// equivalent to preceding || ancestor
	boolean startsBefore(NodePositions prev, NodePositions next);
	// equivalent to following || descendant
	boolean startsAfter(NodePositions prev, NodePositions next);
}