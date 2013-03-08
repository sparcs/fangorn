package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.Constants;
import au.edu.unimelb.csse.LRDP;
import au.edu.unimelb.csse.paypack.PhysicalPayloadFormatAware;

abstract class AbstractHolisticJoin extends AbstractJoin implements
		OperatorCompatibilityAware {
	protected LRDP nodePositionAware;
	protected int positionLength;
	protected int stackLength;
	protected BinaryOperatorAware operatorAware;

	// positions, freq, preorderPos and nextPosCalledCount are indexed by
	// the order of arrangement of positionFreqs not by positionFreqs.position
	int[] positions; // the current position of each node in current doc
	int[] freqs; // the frequency of a node in a document
	int[] preorderPos;
	int[] nextPosCalledCount; // no of times pos called in a doc
	// stack indexed by positionFreqs.position not index of positionFreqs.
	// See updateStackIfNeeded method for details
	int[][] positionStacks;
	int[] positionStacksSizes;
	int[] resultStackPointers;

	// stack size is times 5 because an additional pointer is stored at each
	// position
	private static int DEFAULT_STACK_SIZE = 50 * 5;

	protected PhysicalPayloadFormatAware payloadFormat = Constants.DEFAULT_PAYLOAD_FORMAT;

	public AbstractHolisticJoin(String[] labels, int[] parentPos,
			BinaryOperator[] operators,
			LRDP nodePositionAware) {
		super(labels, parentPos, operators);
		setupVars(nodePositionAware);
	}

	public AbstractHolisticJoin(String[] labels, BinaryOperator[] operators,
			LRDP nodePositionAware) {
		super(labels, operators);
		setupVars(nodePositionAware);
	}

	private void setupVars(LRDP nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		this.positionLength = nodePositionAware.getPositionLength();
		this.stackLength = positionLength + 1;
		this.operatorAware = nodePositionAware.getBinaryOperatorHandler();
		freqs = new int[postingsFreqs.length];
		positions = new int[postingsFreqs.length * positionLength];
		preorderPos = new int[postingsFreqs.length];
		nextPosCalledCount = new int[postingsFreqs.length];
		positionStacks = new int[postingsFreqs.length][];
		for (int i = 0; i < postingsFreqs.length; i++) {
			positionStacks[i] = new int[DEFAULT_STACK_SIZE];
		}
		positionStacksSizes = new int[postingsFreqs.length];
		resultStackPointers = new int[postingsFreqs.length];
	}

	public abstract List<int[]> match() throws IOException;

	// is called only when AbstractJoin.nextDoc() returns a valid result
	@Override
	public void setupPerDoc() throws IOException {
		for (int i = 0; i < postingsFreqs.length; i++) {
			freqs[i] = postingsFreqs[i].postings.freq();
			// no need to ensure that timesNextPositionsCalled < freq because
			// all terms are required to be present at least once for control to
			// get here
			preorderPos[i] = postingsFreqs[i].postings.nextPosition();
			positions = payloadFormat.decode(
					postingsFreqs[i].postings.getPayload(), positions, i
							* positionLength);
			nextPosCalledCount[i] = 1; // all nextPostion() are called once
		}
		// reset stack sizes
		for (int i = 0; i < postingsFreqs.length; i++) {
			positionStacksSizes[i] = 0;
		}
	}

	boolean updateStackIfNeeded(int pos) {
		if (postingsFreqs[pos].parent == null
				|| positionStacksSizes[postingsFreqs[pos].parent.position] > 0) {
			updateStack(pos);
			return true;
		}
		return false;
	}

	void updateStack(int pos) {
		int index = postingsFreqs[pos].position; // stack index
		int[] curStack = positionStacks[index];
		int curStackSize = positionStacksSizes[index];
		try {
			System.arraycopy(positions, pos * positionLength, curStack,
					curStackSize * stackLength, positionLength);
			curStack[curStackSize * stackLength + positionLength] = (postingsFreqs[pos].parent == null ? -1
					: positionStacksSizes[postingsFreqs[pos].parent.position] - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] newStack = new int[positionStacks[index].length
					+ DEFAULT_STACK_SIZE];
			System.arraycopy(curStack, 0, newStack, 0, curStack.length);
			positionStacks[index] = (curStack = newStack);
			// redo
			System.arraycopy(positions, pos * positionLength, curStack,
					curStackSize * stackLength, positionLength);
			curStack[curStackSize * stackLength + positionLength] = (postingsFreqs[pos].parent == null ? -1
					: positionStacksSizes[postingsFreqs[pos].parent.position] - 1);

		}
		positionStacksSizes[index] += 1;
	}

	boolean shouldStop() {
		for (int i = 0; i < postingsFreqs.length; i++) {
			if (postingsFreqs[i].isLeaf && nextPosCalledCount[i] <= freqs[i]) {
				return false;
			}
		}
		return true;
	}

	void getNextPosition(int pos) throws IOException {
		if (nextPosCalledCount[pos] <= freqs[pos]) {
			if (nextPosCalledCount[pos] != freqs[pos]) {
				preorderPos[pos] = postingsFreqs[pos].postings.nextPosition();
				positions = payloadFormat.decode(
						postingsFreqs[pos].postings.getPayload(), positions,
						pos * positionLength);
			}
			nextPosCalledCount[pos] += 1;
		}
	}

	@Override
	public boolean check(BinaryOperator op) {
		return BinaryOperator.CHILD.equals(op)
				|| BinaryOperator.DESCENDANT.equals(op);
	}

	void getPathSolutions(List<int[]> results, PostingsAndFreq leafPf) {
		getPathSolutionsIter(results, leafPf, 0,
				new int[postingsFreqs.length * positionLength]);
	}

	private void getPathSolutionsIter(List<int[]> results, PostingsAndFreq pf,
			int i, int[] result) {
		System.arraycopy(positionStacks[pf.position], i * stackLength, result,
				pf.position * positionLength, positionLength);
		if (parentPos[pf.position] == -1) { // root
			if (BinaryOperator.CHILD.equals(operators[pf.position])) {
				// root of query should be root of tree
				int depth = nodePositionAware.depth(positionStacks[pf.position], i * stackLength);
				if (depth != 0)
					return;
			}
			results.add(Arrays.copyOf(result, result.length));
		} else {
			if (BinaryOperator.CHILD.equals(operators[pf.position])) {
				int parentStackPointer = positionStacks[pf.position][i * stackLength + positionLength];
				for (int j = 0; j <= parentStackPointer; j++) {
					if (operatorAware.child(positionStacks[pf.parent.position],
							j * stackLength, positionStacks[pf.position], i * stackLength)) {
						getPathSolutionsIter(results, pf.parent, j, result);
						break; // a child can have max 1 parent in a tree
					}
				}
			} else {
				int parentStackPointer = positionStacks[pf.position][i * stackLength + positionLength];
				for (int j = 0; j <= parentStackPointer; j++) {
					getPathSolutionsIter(results, pf.parent, j, result);
				}
			}
		}
	}

}
