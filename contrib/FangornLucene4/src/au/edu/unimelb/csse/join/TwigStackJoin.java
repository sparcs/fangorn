package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import au.edu.unimelb.csse.Op;
import au.edu.unimelb.csse.Operator;
import au.edu.unimelb.csse.PayOff;

public class TwigStackJoin extends AbstractHolisticJoin {

	// pfPositionIds maps PostingsAndFreq.position to postingsFreqs array index
	int[] pfIdxByPfPos;
	boolean[] maxPosReached;
	Map<Integer, List<int[]>> partialResultsLists;
	MergeNode rootMergeNode;

	public TwigStackJoin(String[] labels, int[] parentPos, Operator[] operators) {
		super(labels, parentPos, operators);
		pfIdxByPfPos = new int[labels.length];
		maxPosReached = new boolean[labels.length];
		partialResultsLists = new HashMap<Integer, List<int[]>>();
	}

	@Override
	public List<int[]> match() throws IOException {
		while (true) {
			int pfIdx = getMinSource(root);
			if (pfIdx == -1) {
				break;
			}
			if (!postingsFreqs[pfIdx].isLeaf
					&& postingsFreqs[pfIdx].parent != null) {
				cleanStack(postingsFreqs[pfIdx].parent, positions[pfIdx * 4
						+ PayOff.LEFT]);
			}
			if (postingsFreqs[pfIdx].parent == null
					|| positionStacksSizes[postingsFreqs[pfIdx].parent.position] > 0) {
				cleanStack(postingsFreqs[pfIdx], positions[pfIdx * 4
						+ PayOff.LEFT]);
				updateStack(pfIdx);
				if (postingsFreqs[pfIdx].isLeaf) {
					List<int[]> partialResults = partialResultsLists
							.get(postingsFreqs[pfIdx].position);
					getPathSolutions(partialResults, postingsFreqs[pfIdx]);
					positionStacksSizes[postingsFreqs[pfIdx].position]--;
				}
			}
			getNextPosition(pfIdx);
		}
		return rootMergeNode.mergedResults();
	}

	private void cleanStack(PostingsAndFreq node, int left) {
		int[] stack = positionStacks[node.position];
		int stackSize = positionStacksSizes[node.position];
		while (stackSize > 0
				&& stack[(stackSize - 1) * 5 + PayOff.RIGHT] <= left) {
			stackSize--;
		}
		positionStacksSizes[node.position] = stackSize;
	}

	/*
	 * Returns index of the postingsFreqs element that should be processed next
	 */
	int getMinSource(PostingsAndFreq pf) throws IOException {
		if (pf.isLeaf) {
			int pfIdx = pfIdxByPfPos[pf.position];
			if (maxPosReached[pfIdx]) {
				return -1;
			}
			return pfIdx;
		}
		int minIdx = -1;
		int maxIdx = -1;
		for (PostingsAndFreq child : pf.children) {
			int pfIdx = getMinSource(child);
			if (pfIdx == -1) {
				continue;
			} else if (pfIdx != pfIdxByPfPos[child.position]) {
				return pfIdx;
			}
			if (minIdx == -1 && maxIdx == -1) {
				minIdx = maxIdx = pfIdx;
			} else if (startsBefore(positions, minIdx * 4, positions, pfIdx * 4)) {
				minIdx = pfIdx;
			} else if (startsAfter(positions, maxIdx * 4, positions, pfIdx * 4)) {
				maxIdx = pfIdx;
			}
		}
		int pfIdx = pfIdxByPfPos[pf.position];
		if (maxPosReached[pfIdx])
			return minIdx;
		while (nextPosCalledCount[pfIdx] <= freqs[pfIdx]
				&& Op.FOLLOWING.match(positions, pfIdx * 4, positions,
						maxIdx * 4)) {
			getNextPosition(pfIdx);
		}
		if (startsBefore(positions, minIdx * 4, positions, pfIdx * 4))
			return pfIdx;
		return minIdx;
	}

	@Override
	void getNextPosition(int pos) throws IOException {
		if (nextPosCalledCount[pos] == freqs[pos]) {
			maxPosReached[pos] = true;
		}
		super.getNextPosition(pos);
	}

	/**
	 * Does pos2 start after pos1?
	 * 
	 * @param pos1
	 * @param off1
	 * @param pos2
	 * @param off2
	 * @return
	 */
	boolean startsAfter(int[] pos1, int off1, int[] pos2, int off2) {
		return pos1[off1 + PayOff.LEFT] < pos2[off2 + PayOff.LEFT]
				|| (pos1[off1 + PayOff.LEFT] == pos2[off2 + PayOff.LEFT] && pos1[off1
						+ PayOff.DEPTH] < pos2[off2 + PayOff.DEPTH]);
	}

	/**
	 * Does pos2 start before pos1?
	 * 
	 * @param pos1
	 * @param off1
	 * @param pos2
	 * @param off2
	 * @return
	 */
	boolean startsBefore(int[] pos1, int off1, int[] pos2, int off2) {
		return pos1[off1 + PayOff.LEFT] > pos2[off2 + PayOff.LEFT]
				|| (pos1[off1 + PayOff.LEFT] == pos2[off2 + PayOff.LEFT] && pos1[off1
						+ PayOff.DEPTH] > pos2[off2 + PayOff.DEPTH]);
	}

	@Override
	public void setupPerAtomicContext() {
		super.setupPerAtomicContext();
		for (int i = 0; i < postingsFreqs.length; i++) {
			PostingsAndFreq pf = postingsFreqs[i];
			pfIdxByPfPos[pf.position] = i;
		}
		setupTwigMergePolicy();
	}

	private void setupTwigMergePolicy() {
		Stack<Integer> parentStack = new Stack<Integer>();
		List<Integer> descendantPos = new ArrayList<Integer>();
		this.rootMergeNode = createMergeNodeTree(root, parentStack,
				descendantPos);
	}

	private MergeNode createMergeNodeTree(PostingsAndFreq node,
			Stack<Integer> parentStack, List<Integer> descendantPos) {
		if (node.children.length == 1) { // no need to merge anything when 1
											// child
			parentStack.add(node.position);
			List<Integer> childDescPos = new ArrayList<Integer>();
			MergeNode n = createMergeNodeTree(node.children[0], parentStack,
					childDescPos);
			childDescPos.add(node.position);
			descendantPos.addAll(childDescPos);
			parentStack.pop();
			return n;
		}
		MergeNode n = new MergeNode(node.position, parentStack);
		if (node.children.length == 0) {
			descendantPos.add(node.position);
			return n;
		}
		parentStack.add(node.position);
		List<Integer> allDesc = new ArrayList<Integer>();
		for (PostingsAndFreq child : node.children) {
			List<Integer> childDescPos = new ArrayList<Integer>();
			MergeNode cn = createMergeNodeTree(child, parentStack, childDescPos);
			n.addChildNodeAndDescendants(cn, childDescPos);
			allDesc.addAll(childDescPos);
		}
		int[] allDescPos = new int[allDesc.size()];
		for (int i = 0; i < allDescPos.length; i++) {
			allDescPos[i] = allDesc.get(i);
		}
		allDesc.add(n.position);
		Arrays.sort(allDescPos);
		descendantPos.addAll(allDesc);
		parentStack.pop();
		return n;
	}

	@Override
	public void setupPerDoc() throws IOException {
		super.setupPerDoc();
		for (int i = 0; i < postingsFreqs.length; i++) {
			maxPosReached[i] = false;
			if (postingsFreqs[i].isLeaf) {
				if (partialResultsLists.get(postingsFreqs[i].position) == null) {
					partialResultsLists.put(postingsFreqs[i].position,
							new ArrayList<int[]>());
				}
				partialResultsLists.get(postingsFreqs[i].position).clear();
			}
		}
	}

	class MergeNode {
		private static final int POS_ENC_LENGTH = 4; 

		int position; // position of the node in the query
		int[] ancSelfPos;
		List<int[]> descPos = new ArrayList<int[]>();
		List<MergeNode> children = new ArrayList<TwigStackJoin.MergeNode>();
		
		private PartialPathComparator ancPathComp;

		public MergeNode(int position, List<Integer> parentStack) {
			this.position = position;
			int size = parentStack.size();
			ancSelfPos = new int[size + 1];
			// store in decreasing ancestor depth order
			for (int i = 0; i < size; i++) {
				ancSelfPos[i] = parentStack.get(i);
			}
			ancSelfPos[size] = this.position;
			this.ancPathComp = new PartialPathComparator(POS_ENC_LENGTH, ancSelfPos);
		}

		public void addChildNodeAndDescendants(MergeNode child,
				List<Integer> childDescPos) {
			children.add(child);
			int[] descPosArr = new int[childDescPos.size()];
			for (int i = 0; i < childDescPos.size(); i++) {
				descPosArr[i] = childDescPos.get(i);
			}
			Arrays.sort(descPosArr);
			descPos.add(descPosArr);
		}

		List<int[]> mergedResults() {
			if (children.size() == 0) {
				return partialResultsLists.get(position);
			}
			List<int[]> results = new ArrayList<int[]>();
			List<int[]> prev = children.get(0).mergedResults();
			Collections.sort(prev, ancPathComp);
			for (int i = 1; i < children.size(); i++) {
				List<int[]> next = children.get(i).mergedResults();
				Collections.sort(next, ancPathComp);
				int[] descIdxArr = descPos.get(i);
				int prevSkip = 0;
				for (int j = 0; j < next.size(); j++) {
					for (int k = prevSkip; k < prev.size(); k++) {
						int[] pr = prev.get(k);
						int[] nr = next.get(j);
						int compare = ancPathComp.compare(pr, nr);
						if (compare < 0) {
							prevSkip++;
						} else if (compare > 0) {
							break;
						} else {
							int[] newResult = new int[pr.length];
							System.arraycopy(pr, 0, newResult, 0, pr.length);
							for (int m = 0; m < descIdxArr.length; m++) {
								for (int n = 0; n < POS_ENC_LENGTH; n++) {
									newResult[descIdxArr[m] * POS_ENC_LENGTH + n] = nr[descIdxArr[m] * POS_ENC_LENGTH + n];
								}
							}
							results.add(newResult);
						}
					}
				}
				prev = results;
			}
			return results;
		}
	}

}
