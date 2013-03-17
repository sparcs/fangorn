package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.join.AbstractJoin.PostingsAndFreq;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

class HalfPairJoinPipeline {
	Pipe root;
	LogicalNodePositionAware nodePositionAware;
	HalfPairJoin join;
	BinaryOperator[] operators;

	public HalfPairJoinPipeline(LogicalNodePositionAware nodePositionAware,
			HalfPairJoin join) {
		this.nodePositionAware = nodePositionAware;
		this.join = join;
	}

	Pipe createExecPipeline(PostingsAndFreq pfRoot, BinaryOperator[] operators,
			NodePositions prev, NodePositions... buffer) {
		this.operators = operators;
		if (BinaryOperator.CHILD.equals(operators[pfRoot.position])) {
			root = new GetRootNodePipe(pfRoot.postings);
		} else {
			root = new GetAllPipe(pfRoot.postings);
		}
		pathFromNode(pfRoot, root);
		return root;
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
			BinaryOperator op = operators[node.children[i].position];
			MetaPipe meta = new MetaPipe(BinaryOperatorInverse.get(op), prevPipe);
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
					BinaryOperatorInverse.get(operators[node.children[0].position]), prev);
			prev.setNext(current);
			return current;
		}
		PostingsAndFreq firstChild = node.children[0];
		Pipe prev = addInReverse(firstChild);
		Pipe current = new SimplePipe(node.postings,
				BinaryOperatorInverse.get(operators[firstChild.position]), prev);
		prev.setNext(current);
		prev = current;
		for (int i = 1; i < node.children.length; i++) {
			PostingsAndFreq child = node.children[i];
			MetaPipe meta = new MetaPipe(
					BinaryOperatorInverse.get(operators[child.position]), prev);
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
		private BinaryOperator op;

		public SimplePipe(DocsAndPositionsEnum node, BinaryOperator op, Pipe prev) {
			super(node);
			this.op = op;
			this.prev = prev;
		}

		@Override
		public NodePositions execute() throws IOException {
			prevPositions.offset = 0;
			join.match(prevPositions, op, node, buffers);
			if (buffers[0].size > 0) {
				return continueExection();
			}
			return buffers[0];
		}
		
		BinaryOperator getOp() {
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
		private BinaryOperator op;
		private NodePositions metaPrev;
		private NodePositions[] buffers;
		private NodePositions innerResultsCopy;
		private NodePositions prevPositions;
		private Pipe prev;

		public MetaPipe(BinaryOperator op, Pipe prev) {
			this.op = op;
			metaPrev = new NodePositions();
			innerResultsCopy = new NodePositions();
			this.prev = prev;
		}

		public void setInner(Pipe pipe) {
			this.inner = pipe;
		}

		@Override
		public NodePositions execute() throws IOException {
			prevPositions.reset();
			inner.setPrevPositionsAndBuffer(prevPositions, buffers);
			NodePositions results = inner.execute();
			if (results.size == 0) {
				return results;
			}
			innerResultsCopy.copyFrom(results);
			join.match(innerResultsCopy, op, metaPrev, buffers);
			if (next == null) {
				return buffers[0];
			}
			metaPrev.copyFrom(buffers[0]);
			next.setPrevPositionsAndBuffer(metaPrev, buffers);
			return next.execute();
		}

		@Override
		public void setPrevPositionsAndBuffer(NodePositions prevPositions, NodePositions[] buffers) {
			metaPrev.copyFrom(prevPositions);
			this.prevPositions = prevPositions;
			this.buffers = buffers;
		}

		@Override
		public void setNext(Pipe pipe) {
			this.next = pipe;
		}
		
		@Override
		public Pipe getNext() {
			return next;
		}
		
		BinaryOperator getOp() {
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
		NodePositions prevPositions;
		NodePositions[] buffers;
		Pipe next;
		Pipe prev;
		DocsAndPositionsEnum node;

		public AbstractPipe(DocsAndPositionsEnum node) {
			this.node = node;
		}

		@Override
		public void setPrevPositionsAndBuffer(NodePositions prevPositions, NodePositions[] buffer) {
			this.prevPositions = prevPositions;
			this.buffers = buffer;
		}

		public void setNext(Pipe pipe) {
			this.next = pipe;
		}

		NodePositions continueExection() throws IOException {
			if (next == null || buffers[0].size == 0)
				return buffers[0];
			NodePositions result = buffers[0];
			buffers[0] = prevPositions;
			prevPositions = result;
			next.setPrevPositionsAndBuffer(prevPositions, buffers);
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
		void setPrevPositionsAndBuffer(NodePositions prev, NodePositions[] buffers);

		NodePositions execute() throws IOException;

		void setNext(Pipe pipe);
		
		Pipe getNext();
		
		Pipe getStart();
		
		int maxBufferSize();
	}
}
