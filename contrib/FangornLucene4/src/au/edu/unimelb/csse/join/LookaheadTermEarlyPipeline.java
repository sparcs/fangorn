package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorAware;
import au.edu.unimelb.csse.OperatorInverse;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class LookaheadTermEarlyPipeline extends HalfPairJoinPipeline implements
		BooleanJoinPipeline {
	HalfPairLATEJoin lateJoin;
	NodePositions buffer;
	int positionLength;
	OperatorAware operatorAware;

	public LookaheadTermEarlyPipeline(
			LogicalNodePositionAware nodePositionAware, HalfPairLATEJoin join) {
		super(nodePositionAware, join);
		lateJoin = join;
		buffer = new NodePositions();
		positionLength = nodePositionAware.getPositionLength();
		operatorAware = nodePositionAware.getOperatorHandler();
	}

	boolean isLookaheadOp(Operator op) {
		return Operator.DESCENDANT.equals(op) || Operator.ANCESTOR.equals(op)
				|| Operator.FOLLOWING.equals(op)
				|| Operator.PRECEDING.equals(op);
	}

	Operator getCommonChildrenOp(PostingsAndFreq pf) {
		if (pf.children.length == 0)
			return null;
		Operator op = operators[pf.children[0].position];
		for (int i = 1; i < pf.children.length; i++) {
			if (!operators[pf.children[i].position].equals(op)) {
				return null;
			}
		}
		return op;
	}

	@Override
	public Pipe createExecPipeline(PostingsAndFreq pfRoot, Operator[] operators) {
		this.operators = operators;
		if (Operator.CHILD.equals(operators[pfRoot.position])) {
			root = new GetRootNodePipe(pfRoot.postings);
		} else {
			Operator op = getCommonChildrenOp(pfRoot);
			if (op != null && isLookaheadOp(op)) {
				root = new GetAllLookaheadPipe(pfRoot.postings, op);
			} else {
				root = new GetAllPipe(pfRoot.postings);
			}
		}
		pathFromNode(pfRoot, root);
		return root;
	}

	private void pathFromNode(PostingsAndFreq node, Pipe prevPipe) {
		if (node.children.length == 0) {
			return;
		} else if (node.children.length == 1) {
			final PostingsAndFreq child = node.children[0];
			final Operator op = operators[child.position];
			if (child.children.length == 0) {
				Pipe next = new TerminateEarlyPipe(child.postings, op);
				prevPipe.setNext(next);
			} else if (child.children.length == 1) {
				Operator nextOp = operators[child.children[0].position];
				Pipe next;
				if (isLookaheadOp(nextOp)) {
					next = new LookaheadPipe(child.postings, op, nextOp,
							prevPipe);
				} else {
					next = new SimplePipe(child.postings, op, prevPipe);
				}
				prevPipe.setNext(next);
				pathFromNode(child, next);
			} else {
				Operator nextOp = getCommonChildrenOp(child);
				Pipe next;
				if (nextOp != null && isLookaheadOp(nextOp)) {
					next = new LookaheadPipe(child.postings, op, nextOp,
							prevPipe);
				} else {
					next = new SimplePipe(child.postings, op, prevPipe);
				}
				prevPipe.setNext(next);
				pathFromNode(child, next);
			}
			return;
		}
		for (int i = 0; i < node.children.length; i++) {
			Operator op = operators[node.children[i].position];
			MetaPipe meta;
			if (i == node.children.length - 1) {
				meta = new MetaTerminateEarlyPipe(OperatorInverse.get(op),
						prevPipe);
			} else {
				meta = new MetaPipe(OperatorInverse.get(op), prevPipe);
			}
			Pipe last = addInReverse(node.children[i]);
			meta.setInner(last.getStart());
			prevPipe.setNext(meta);
			prevPipe = meta;
		}
	}

	private Pipe addInReverse(PostingsAndFreq node) {
		Operator nextOp = OperatorInverse.get(operators[node.position]);
		if (node.children.length == 0) {
			if (isLookaheadOp(nextOp)) {
				return new GetAllLookaheadPipe(node.postings, nextOp);
			}
			return new GetAllPipe(node.postings);
		} else if (node.children.length == 1) {
			return addInReverseFirstNode(node, nextOp, true);
		}
		Pipe prev = addInReverseFirstNode(node, nextOp, false);
		for (int i = 1; i < node.children.length; i++) {
			PostingsAndFreq child = node.children[i];
			MetaPipe meta = new MetaPipe(
					OperatorInverse.get(operators[child.position]), prev);
			Pipe last = addInReverse(child);
			meta.setInner(last.getStart());
			prev.setNext(meta);
			prev = meta;
		}
		return prev;
	}

	private Pipe addInReverseFirstNode(PostingsAndFreq node, Operator nextOp, boolean canUseLookahead) {
		Pipe prev = addInReverse(node.children[0]);
		Operator op = OperatorInverse.get(operators[node.children[0].position]);
		Pipe current;
		if (canUseLookahead && isLookaheadOp(nextOp)) {
			current = new LookaheadPipe(node.postings, op, nextOp, prev);
		} else {
			current = new SimplePipe(node.postings, op, prev);
		}
		prev.setNext(current);
		return current;
	}

	class LookaheadPipe extends AbstractPipe implements Pipe {

		Operator nextOp;
		Operator op;

		public LookaheadPipe(DocsAndPositionsEnum node, Operator op,
				Operator nextOp, Pipe prev) {
			super(node, prev);
			this.op = op;
			this.nextOp = nextOp;
		}

		@Override
		public NodePositions execute() throws IOException {
			NodePositions results = lateJoin.matchWithLookahead(prevPositions,
					op, node, nextOp);
			if (results.size > 0) {
				return continueExection(results);
			}
			return results;
		}

	}

	class TerminateEarlyPipe extends AbstractPipe implements Pipe {
		Operator op;

		public TerminateEarlyPipe(DocsAndPositionsEnum node, Operator op) {
			super(node);
			this.op = op;
		}

		@Override
		public NodePositions execute() throws IOException {
			NodePositions results = lateJoin.matchTerminateEarly(prevPositions,
					op, node);
			if (results.size > 0) {
				return continueExection(results);
			}
			return results;
		}

	}

	class GetAllLookaheadPipe extends AbstractPipe implements Pipe {
		Operator nextOp;

		public GetAllLookaheadPipe(DocsAndPositionsEnum postings, Operator op) {
			super(postings);
			nextOp = op;
		}

		@Override
		public NodePositions execute() throws IOException {
			nodePositionAware.getAllPositions(buffer, node);
			prevPositions.reset();
			prevPositions.push(buffer, positionLength);
			buffer.offset += positionLength;
			if (Operator.PRECEDING.equals(nextOp)) {
				buffer.offset = buffer.size - positionLength;
				prevPositions.reset();
				prevPositions.push(buffer, positionLength);
				return prevPositions;
			}
			while (buffer.offset < buffer.size) {
				if (Operator.DESCENDANT.equals(nextOp)) {
					if (!operatorAware.descendant(prevPositions.positions,
							prevPositions.offset, buffer.positions,
							buffer.offset)) {
						prevPositions.push(buffer, positionLength);
					}
				} else if (Operator.ANCESTOR.equals(nextOp)) {
					if (operatorAware.descendant(prevPositions.positions,
							prevPositions.offset, buffer.positions,
							buffer.offset)) {
						prevPositions.removeLast(positionLength);
					}
					prevPositions.push(buffer, positionLength);
				} else if (Operator.FOLLOWING.equals(nextOp)) {
					if (operatorAware.descendant(prevPositions.positions,
							prevPositions.offset, buffer.positions,
							buffer.offset)) {
						prevPositions.removeLast(positionLength);
						prevPositions.push(buffer, positionLength);
					} else {
						break;
					}
				}
				buffer.offset += positionLength;
			}
			if (prevPositions.size > 0 && next != null) {
				return next.execute();
			}
			return prevPositions;
		}

	}

	class MetaTerminateEarlyPipe extends MetaPipe implements Pipe {

		public MetaTerminateEarlyPipe(Operator op, Pipe prev) {
			super(op, prev);
		}

		@Override
		public NodePositions execute() throws IOException {
			metaPrev.makeCloneOf(prevPositions);
			prevPositions.reset();
			NodePositions results = inner.execute();
			if (results.size == 0) {
				return results;
			}
			prevPositions.makeCloneOf(results);
			return lateJoin.matchTerminateEarly(prevPositions, op, metaPrev);
		}
	}

	/*class MetaLookaheadPipe extends MetaPipe implements Pipe {

		private Operator nextOp;

		public MetaLookaheadPipe(Operator op, Operator nextOp, Pipe prev) {
			super(op, prev);
			this.nextOp = nextOp;
		}

		@Override
		public NodePositions execute() throws IOException {
			metaPrev.makeCloneOf(prevPositions);
			prevPositions.reset();
			NodePositions results = inner.execute();
			if (results.size == 0) {
				return results;
			}
			prevPositions.makeCloneOf(results);
			results = lateJoin.matchWithLookahead(prevPositions, op, metaPrev,
					nextOp);
			if (next == null) {
				return results;
			}
			prevPositions.makeCloneOf(results);
			return next.execute();
		}
	}*/
}
