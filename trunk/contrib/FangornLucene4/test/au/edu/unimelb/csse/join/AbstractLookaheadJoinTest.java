package au.edu.unimelb.csse.join;

import java.io.IOException;

import junit.framework.TestCase;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.join.HalfPairLATEJoin.PruneOperation;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class AbstractLookaheadJoinTest extends TestCase {
	LogicalNodePositionAware lrdp = new LRDP(
			LRDP.PhysicalPayloadFormat.BYTE1111);
	
	public void testFwdIterPrecedingLAReturnsJoinMatchReplace()
			throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 3, 4 });
		AbstractLookaheadJoin join = getJoin(Operator.PRECEDING);
		join.result = new NodePositions(new int[] { 2, 3, 4, 5 });
		PruneOperation op = join.getFwdIterPruneOperation(node, Operator.PRECEDING);
		assertEquals(PruneOperation.JOIN_MATCH_REPLACE, op);
	}

	public void testBwdIterPrecedingLABwdIterReturnsStopAtJoinMatch()
			throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 3, 4 });
		AbstractLookaheadJoin join = getJoin(Operator.PRECEDING);
		PruneOperation op = join.getBwdIterPruneOperation(node, Operator.PRECEDING);
		assertEquals(PruneOperation.JOIN_MATCH_ADD_STOP, op);
		
		//this scenario should never happen 
		//result cannot have a position and then join again for preceding op 
//		join.result = new NodePositions(new int[] { 2, 3, 4, 5 });
//		op = join.getPruneOperation(node, Operator.PRECEDING, false);
//		assertEquals(PruneOperation.JOIN_MATCH_ADD_STOP, op);
	}

	public void testFwdIterAllLAReturnJoinMatchAddWhenNoResults()
			throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 3, 4 });
		for (Operator op : new Operator[] { Operator.DESCENDANT,
				Operator.ANCESTOR, Operator.FOLLOWING, Operator.PRECEDING }) {
			AbstractLookaheadJoin join = getJoin(op);
			PruneOperation pruneOp = join.getFwdIterPruneOperation(node, op);
			assertEquals(PruneOperation.JOIN_MATCH_ADD, pruneOp);
		}
	}
	
	public void testFwdIterDescendantLAReturnsPruneWhenDesc() throws Exception {
		NodePositions node = new NodePositions(new int[] { 2, 3, 3, 4 });
		AbstractLookaheadJoin join = getJoin(Operator.DESCENDANT);
		join.result = new NodePositions(new int[] { 2, 3, 1, 2 });
		PruneOperation pruneOp = join.getFwdIterPruneOperation(node, Operator.DESCENDANT);
		assertEquals(PruneOperation.PRUNE, pruneOp);
		
		join.result = new NodePositions(new int[] { 1, 2, 1, 1 });
		pruneOp = join.getFwdIterPruneOperation(node, Operator.DESCENDANT);
		assertEquals(PruneOperation.JOIN_MATCH_ADD, pruneOp);
	}
	
	public void testBwdIterDescendantLABwdIterReturnsJoinMatchReplaceManyWhenAncestor() throws Exception {
		NodePositions node = new NodePositions(new int[] { 2, 3, 1, 2 });
		AbstractLookaheadJoin join = getJoin(Operator.DESCENDANT);
		join.result = new NodePositions(new int[] { 2, 3, 3, 4 });
		PruneOperation pruneOp = join.getBwdIterPruneOperation(node, Operator.DESCENDANT);
		assertEquals(PruneOperation.JOIN_MATCH_REPLACE_MANY, pruneOp);
		
		join.result = new NodePositions(new int[] { 3, 4, 1, 6 });
		pruneOp = join.getBwdIterPruneOperation(node, Operator.DESCENDANT);
		assertEquals(PruneOperation.JOIN_MATCH_ADD, pruneOp);
	}
	
	public void testFwdIterAncestorLAReturnsJoinMatchReplaceWhenDesc() throws Exception {
		NodePositions node = new NodePositions(new int[] { 2, 3, 3, 4 });
		AbstractLookaheadJoin join = getJoin(Operator.ANCESTOR);
		join.result = new NodePositions(new int[] { 2, 3, 1, 2 });
		PruneOperation pruneOp = join.getFwdIterPruneOperation(node, Operator.ANCESTOR);
		assertEquals(PruneOperation.JOIN_MATCH_REPLACE, pruneOp);
		
		join.result = new NodePositions(new int[] { 1, 2, 1, 1 });
		pruneOp = join.getFwdIterPruneOperation(node, Operator.ANCESTOR);
		assertEquals(PruneOperation.JOIN_MATCH_ADD, pruneOp);
	}
	
	public void testBwdIterAncestorLABwdIterReturnsPruneWhenAncestor() throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 1, 2 });
		AbstractLookaheadJoin join = getJoin(Operator.ANCESTOR);
		join.result = new NodePositions(new int[] { 1, 2, 3, 4 });
		PruneOperation pruneOp = join.getBwdIterPruneOperation(node, Operator.ANCESTOR);
		assertEquals(PruneOperation.PRUNE, pruneOp);
		
		join.result = new NodePositions(new int[] { 2, 3, 4, 5 });
		pruneOp = join.getBwdIterPruneOperation(node, Operator.ANCESTOR);
		assertEquals(PruneOperation.JOIN_MATCH_ADD, pruneOp);
	}

	public void testFwdIterFollowingLAReturnsJoinMatchReplaceWhenDescPruneStopOtherwise() throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 3, 4 });
		AbstractLookaheadJoin join = getJoin(Operator.FOLLOWING);
		join.result = new NodePositions(new int[] { 1, 2, 1, 2 });
		PruneOperation pruneOp = join.getFwdIterPruneOperation(node, Operator.FOLLOWING);
		assertEquals(PruneOperation.JOIN_MATCH_REPLACE, pruneOp);
		
		join.result = new NodePositions(new int[] { 1, 2, 4, 5 });
		pruneOp = join.getFwdIterPruneOperation(node, Operator.FOLLOWING);
		assertEquals(PruneOperation.PRUNE_STOP, pruneOp);
	}

	public void testBwdIterFollowingLABwdIterReturnsPruneWhenAncJoinMatchReplaceOtherwise() throws Exception {
		NodePositions node = new NodePositions(new int[] { 1, 2, 1, 2 });
		AbstractLookaheadJoin join = getJoin(Operator.FOLLOWING);
		join.result = new NodePositions(new int[] { 1, 2, 3, 4 });
		PruneOperation pruneOp = join.getBwdIterPruneOperation(node, Operator.FOLLOWING);
		assertEquals(PruneOperation.PRUNE, pruneOp);
		
		join.result = new NodePositions(new int[] { 3, 4, 5, 6 });
		pruneOp = join.getBwdIterPruneOperation(node, Operator.FOLLOWING);
		assertEquals(PruneOperation.JOIN_MATCH_REPLACE, pruneOp);
	}
	
	AbstractLookaheadJoin getJoin(Operator op) {
		return new AbstractLookaheadJoin(op, lrdp) {

			@Override
			public NodePositions matchTerminateEarly(NodePositions prev,
					NodePositions next) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public NodePositions match(NodePositions prev, NodePositions next) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected NodePositions matchLookaheadFwdIter(NodePositions prev,
					Operator op, NodePositions next, Operator nextOp) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected NodePositions matchLookaheadBwdIter(NodePositions prev,
					Operator op, NodePositions next, Operator nextOp) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
}
