package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.BinaryOperatorAware;
import au.edu.unimelb.csse.LogicalNodePositionAware;

/**
 * This path join algorithm is an adaptation of the StackTree join described by
 * Al-Khalifa et al. (2002)
 * 
 * Al-Khalifa, S.; Jagadish, H.; Koudas, N.; Patel, J. M.; Srivastava, D. & Wu,
 * Y. Structural Joins: A Primitive for Efficient XML Query Pattern Matching
 * ICDE '02: Proceedings of the 18th International Conference on Data
 * Engineering, IEEE Computer Society, 2002, 141
 * 
 * We use the LPath numbering scheme described in Bird et.al. (2005)
 * 
 * Bird, Steven G., Yi Chen, Susan B. Davidson, Haejoong Lee, and Yifeng Zheng.
 * 2005. Extending xpath to support linguistic queries. In Proceedings of
 * Programming Language Technologies for XML (PLANX), 35--46, Long Beach, USA.
 * 
 * @author sumukh
 * 
 */
public class StackTreeJoin extends AbstractPairwiseJoin implements
		FullPairJoin {

	private LogicalNodePositionAware nodePositionAware;
	private BinaryOperatorAware operatorAware;
	private int positionLength;

	public StackTreeJoin(LogicalNodePositionAware nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
		this.positionLength = nodePositionAware.getPositionLength();
		this.operatorAware = nodePositionAware.getBinaryOperatorHandler();
	}

	@Override
	public void match(NodePositions prev, BinaryOperator op,
			DocsAndPositionsEnum node, NodePairPositions result,
			NodePositions... buffers) throws IOException {
		NodePositions buffer = buffers[0];
		NodePositions stack = buffers[1];

		nodePositionAware.getAllPositions(buffer, node);

		while (buffer.offset < buffer.size && prev.offset < prev.size) {
			if (operatorAware.startsBefore(buffer, prev)) {
				// prev starts before buffer
				while (stack.size > 0) {
					if (operatorAware.descendant(stack, prev)) { 
						// desc implies not following
						stack.push(prev, positionLength);
						break;
					}
					stack.pop(positionLength);
				}
				if (stack.size == 0) {
					stack.push(prev, positionLength);
				}
				prev.offset += positionLength;
			} else { // buffer starts before prev
				while (stack.size > 0
						&& !operatorAware.descendant(stack, buffer)) {
					stack.pop(positionLength);
				}
				if (stack.size > 0 && op.match(stack, buffer, operatorAware)) {
					if (op.equals(BinaryOperator.CHILD)) {
						result.add(prev, buffer, positionLength);
					} else {
						int numStackNodes = stack.size / positionLength;
						for (int i = 0; i < numStackNodes; i++) {
							stack.offset = i * positionLength; 
							result.add(stack,buffer, positionLength);
						}
						stack.offset = stack.size - positionLength;
					}
				}
				// if stack.size == 0 then; do nothing
				buffer.offset += positionLength;
			}
		}
		while (stack.size > 0 && buffer.offset < buffer.size) {
			while (stack.size > 0
					&& !operatorAware.descendant(stack, buffer)) {
				stack.pop(positionLength);
			}
			if (stack.size > 0
					&& op.match(stack, buffer, operatorAware)) {
				if (op.equals(BinaryOperator.CHILD)) {
					result.add(stack, buffer, positionLength);
				} else {
					int numStackNodes = stack.size / positionLength;
					for (int i = 0; i < numStackNodes; i++) {
						stack.offset = i * positionLength; 
						result.add(stack,buffer, positionLength);
					}
					stack.offset = stack.size - positionLength;
				}
			}
			buffer.offset += positionLength;
		}
	}

	@Override
	public int numBuffers(BinaryOperator op) {
		return 2;
	}

}
