package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorInverse;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class HalfPairJoinPipeline implements BooleanJoinPipeline {
	Pipe root;
	final LogicalNodePositionAware nodePositionAware;
//	HalfPairJoin join;
	Operator[] operators;
	NodePositions prevPositions;
	JoinBuilder joinBuilder;
	
	public HalfPairJoinPipeline(LogicalNodePositionAware nodePositionAware,
			JoinBuilder joinBuilder) {
		this.nodePositionAware = nodePositionAware;
		this.joinBuilder = joinBuilder;
	}

	public Pipe createExecPipeline(PostingsAndFreq pfRoot, Operator[] operators) {
		this.operators = operators;
		if (Operator.CHILD.equals(operators[pfRoot.position])) {
			root = new GetRootNodePipe(pfRoot.postings);
		} else {
			root = new GetAllPipe(pfRoot.postings);
		}
		pathFromNode(pfRoot, root);
		return root;
	}

	public void setPrevBuffer(NodePositions prev) {
		this.prevPositions = prev;
	}

	private void pathFromNode(PostingsAndFreq node, Pipe prevPipe) {
		if (node.children.length == 0) {
			return;
		} else if (node.children.length == 1) {
			final PostingsAndFreq child = node.children[0];
			SimplePipe next = new SimplePipe(child.postings,
					operators[child.position], prevPipe);
			prevPipe.setNext(next);
			pathFromNode(child, next);
			return;
		}
		for (int i = 0; i < node.children.length; i++) {
			Operator op = operators[node.children[i].position];
			MetaPipe meta = new MetaPipe(OperatorInverse.get(op), prevPipe);
			Pipe last = addInReverse(node.children[i]);
			meta.setInner(last.getStart());
			prevPipe.setNext(meta);
			prevPipe = meta;
		}
	}

	private Pipe addInReverse(PostingsAndFreq node) {
		if (node.children.length == 0) {
			return new GetAllPipe(node.postings);
		} else if (node.children.length == 1) {
			Pipe prev = addInReverse(node.children[0]);
			Pipe current = new SimplePipe(node.postings,
					OperatorInverse.get(operators[node.children[0].position]),
					prev);
			prev.setNext(current);
			return current;
		}
		PostingsAndFreq firstChild = node.children[0];
		Pipe prev = addInReverse(firstChild);
		Pipe current = new SimplePipe(node.postings,
				OperatorInverse.get(operators[firstChild.position]), prev);
		prev.setNext(current);
		prev = current;
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

	class GetAllPipe extends AbstractPipe {

		public GetAllPipe(DocsAndPositionsEnum node) {
			super(node);
		}

		@Override
		public NodePositions execute() throws IOException {
			prevPositions.reset();
			nodePositionAware.getAllPositions(prevPositions, node);
			if (prevPositions.size > 0 && next != null) {
				return next.execute();
			}
			return prevPositions;
		}
	}

	class GetRootNodePipe extends AbstractPipe {

		public GetRootNodePipe(DocsAndPositionsEnum node) {
			super(node);
		}

		@Override
		public NodePositions execute() throws IOException {
			prevPositions.reset();
			nodePositionAware.getAllPositions(prevPositions, node);
			prevPositions.offset = 0;
			if (nodePositionAware
					.isTreeRootPosition(prevPositions.positions, 0)) {
				prevPositions.size = nodePositionAware.getPositionLength();
				return continueExection(prevPositions);
			}
			prevPositions.reset();
			return prevPositions;
		}
	}

	class SimplePipe extends AbstractPipe {
		private Operator op;
		HalfPairJoin join;

		public SimplePipe(DocsAndPositionsEnum node, Operator op, Pipe prev) {
			super(node, prev);
			this.op = op;
			join = joinBuilder.getHalfPairJoin(op, nodePositionAware);
		}

		@Override
		public NodePositions execute() throws IOException {
			NodePositions result = join.match(prevPositions, node);
			if (result.size > 0) {
				return continueExection(result);
			}
			return result;
		}

		Operator getOp() {
			return op;
		}
	}

	class MetaPipe implements Pipe {
		protected Pipe inner;
		protected Pipe next;
		protected Operator op;
		protected NodePositions metaPrev;
		private Pipe prev;
		HalfPairJoin join;

		public MetaPipe(Operator op, Pipe prev) {
			this.op = op;
			join = joinBuilder.getHalfPairJoin(op, nodePositionAware);
			metaPrev = new NodePositions();
			this.prev = prev;
		}

		public void setInner(Pipe pipe) {
			this.inner = pipe;
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
			results = join.match(prevPositions, metaPrev);
			if (next == null) {
				return results;
			}
			prevPositions.makeCloneOf(results);
			return next.execute();
		}

		@Override
		public void setNext(Pipe pipe) {
			this.next = pipe;
		}

		@Override
		public Pipe getNext() {
			return next;
		}

		Operator getOp() {
			return op;
		}

		public Pipe getInner() {
			return inner;
		}

		// metapipe can never start a pipeline chain so no need of null check
		@Override
		public Pipe getStart() {
			return prev.getStart();
		}
	}

	abstract class AbstractPipe implements Pipe {

		Pipe next;
		Pipe prev;
		DocsAndPositionsEnum node;

		public AbstractPipe(DocsAndPositionsEnum node) {
			this.node = node;
		}

		public AbstractPipe(DocsAndPositionsEnum node, Pipe prev) {
			this(node);
			this.prev = prev;
		}

		public void setNext(Pipe pipe) {
			this.next = pipe;
		}

		NodePositions continueExection(NodePositions result) throws IOException {
			if (next == null || result.size == 0)
				return result;
			prevPositions.makeCloneOf(result);
			return next.execute();
		}

		public Pipe getNext() {
			return next;
		}

		@Override
		public Pipe getStart() {
			return prev == null ? this : prev.getStart();
		}
	}

}
