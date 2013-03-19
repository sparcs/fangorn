package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.OperatorInverse;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

class HalfPairJoinPipeline {
	Pipe root;
	final LogicalNodePositionAware nodePositionAware;
	final HalfPairJoin join;
	Operator[] operators;
	NodePositions prevPositions;
	NodePositions[] buffers;

	public HalfPairJoinPipeline(LogicalNodePositionAware nodePositionAware,
			HalfPairJoin join) {
		this.nodePositionAware = nodePositionAware;
		this.join = join;
	}

	Pipe createExecPipeline(PostingsAndFreq pfRoot, Operator[] operators) {
		this.operators = operators;
		if (Operator.CHILD.equals(operators[pfRoot.position])) {
			root = new GetRootNodePipe(pfRoot.postings);
		} else {
			root = new GetAllPipe(pfRoot.postings);
		}
		pathFromNode(pfRoot, root);
		return root;
	}
	
	void setPrevAndBuffers(NodePositions prev, NodePositions... buffers) {
		this.prevPositions = prev;
		this.buffers = buffers;
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
					OperatorInverse.get(operators[node.children[0].position]), prev);
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
	
	int getMaxBufferSize() {
		return root.maxBufferSize();
	}

	class GetAllPipe extends AbstractPipe {

		public GetAllPipe(DocsAndPositionsEnum node) {
			super(node);
		}

		@Override
		public NodePositions execute() throws IOException {
			buffers[0].reset();
			nodePositionAware.getAllPositions(buffers[0], node);
			return continueExection();
		}
	}

	class GetRootNodePipe extends AbstractPipe {

		public GetRootNodePipe(DocsAndPositionsEnum node) {
			super(node);
		}

		@Override
		public NodePositions execute() throws IOException {
			final NodePositions b = buffers[0];
			b.reset();
			nodePositionAware.getAllPositions(b, node);
			b.offset = 0;
			if (nodePositionAware.isTreeRootPosition(b.positions, 0)) {
				b.size = nodePositionAware.getPositionLength();
				return continueExection();
			}
			b.size = 0;
			return b;
		}
	}

	class SimplePipe extends AbstractPipe {
		private Operator op;

		public SimplePipe(DocsAndPositionsEnum node, Operator op, Pipe prev) {
			super(node);
			this.op = op;
			this.prev = prev;
		}

		@Override
		public NodePositions execute() throws IOException {
			join.match(prevPositions, op, node, buffers);
			if (buffers[0].size > 0) {
				return continueExection();
			}
			return buffers[0];
		}
		
		Operator getOp() {
			return op;
		}
		
		@Override
		int bufferSize() {
			return join.numBuffers(op);
		}
	}

	class MetaPipe implements Pipe {
		private Pipe inner;
		private Pipe next;
		private Operator op;
		private NodePositions metaPrev;
		private Pipe prev;

		public MetaPipe(Operator op, Pipe prev) {
			this.op = op;
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
			join.match(prevPositions, op, metaPrev, buffers);
			if (next == null) {
				return buffers[0];
			}
			prevPositions.makeCloneOf(buffers[0]);
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

		@Override
		public int maxBufferSize() {
			int thisbs = join.numBuffers(op);
			int innerbs = inner.maxBufferSize();
			int max = thisbs > innerbs ? thisbs : innerbs;
			if (next == null) return max;
			int nextbs = next.maxBufferSize();
			return max > nextbs ? max : nextbs;
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

		public void setNext(Pipe pipe) {
			this.next = pipe;
		}

		NodePositions continueExection() throws IOException {
			if (next == null || buffers[0].size == 0)
				return buffers[0];
			prevPositions.makeCloneOf(buffers[0]);
			return next.execute();
		}
		
		public Pipe getNext() {
			return next;
		}
		
		@Override
		public int maxBufferSize() {
			int thisbs = bufferSize();
			if (next == null) return thisbs;
			int nbs = next.maxBufferSize();
			return thisbs > nbs ? thisbs : nbs ;
		}
		
		int bufferSize() {
			return 1;
		}
		
		@Override
		public Pipe getStart() {
			return prev == null ? this : prev.getStart();
		}
	}

	interface Pipe {

		NodePositions execute() throws IOException;

		void setNext(Pipe pipe);
		
		Pipe getNext();
		
		Pipe getStart();
		
		int maxBufferSize();
	}
}
