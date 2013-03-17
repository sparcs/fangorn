package au.edu.unimelb.csse.join;

public class NodePositions {
	private static final int DEFAULT_SIZE = 128;
	private static final int SIZE_INCREMENT = 128;
	public int[] positions;
	public int size; // used while writing
	public int offset; // used while iterating/reading/stack-pointer

	public NodePositions() {
		positions = new int[DEFAULT_SIZE];
		size = 0;
		offset = 0;
	}

	public void expand() {
		int[] newPositions = new int[positions.length + SIZE_INCREMENT];
		System.arraycopy(positions, 0, newPositions, 0, positions.length);
		positions = newPositions;
	}

	public boolean hasMore(int positionLength) {
		return offset + positionLength <= size;
	}

	public void reset() {
		size = 0;
		offset = 0;
	}
	
	public void copyFrom(NodePositions other) {
		if (other.positions.length > positions.length) {
			int[] newPositions = new int[other.positions.length];
			positions = newPositions;
		}
		System.arraycopy(other.positions, 0, positions, 0, other.size);
		this.size = other.size;
		this.offset = other.offset;
	}

	public boolean removeLast(int length) {
		if (size - length >= 0) {
			size -= length;
			if (offset > size) {
				offset = size;
			}
			return true;
		}
		return false;
	}
	
	public void push(NodePositions value, int length) {
		while (size + length >= positions.length) {
			expand();
		}
		for (int i = 0; i < length; i++) {
			positions[size++] = value.positions[value.offset + i];
		}
		offset = size - length;
	}
	
	public void pushInt(int value) {
		if (size + 1 >= positions.length) {
			expand();
		}
		positions[size++] = value;
		offset++;
	}
	
	public void pop(int length) {
		if (size >= length) {
			size -= length;
		}
		if (offset > 0) {
			offset -= length;
		}
	}
	
	public int popInt() {
		if (size > 0) {
			int value = positions[size - 1];
			size -= 1;
			if (offset > 0) {
				offset -= 1;
			}
			return value;
		}
		return -1;
	}

	@Override
	public int hashCode() {
		return positions.hashCode() + size + offset;
	};

	@Override
	public boolean equals(Object arg0) {
		if (arg0 == null)
			return false;
		if (!(arg0 instanceof NodePositions))
			return false;
		NodePositions other = (NodePositions) arg0;
		if (size != other.size)
			return false;
		for (int i = 0; i < size; i++) {
			if (positions[i] != other.positions[i])
				return false;
		}
		if (offset != other.offset)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[ ");
		for (int i = 0; i < size - 1; i++) {
			sb.append(positions[i]);
			sb.append(", ");
		}
		if (size - 1 >= 0) {
			sb.append(positions[size - 1]);
		}
		sb.append(" ]");
		return sb.toString();
	}

	public void retain(NodePositions mark, int length) {
		int newOffset = 0;
		if (mark.size * length == size) {
			offset = 0;
			return; // retain all
		}
		for (int i = 0; i < mark.size; i++) {
			System.arraycopy(positions, mark.positions[i], positions, newOffset, length);
			newOffset += length;
		}
		offset = 0;
		size = mark.size * length;
	}

	public void setValues(int[] values) {
		System.arraycopy(values, 0, positions, 0, values.length);
		size = values.length;
		offset = 0;
	}

	public void retain(int retainOffset, int length) {
		System.arraycopy(positions, retainOffset, positions, 0, length);
		this.offset = 0;
		size = length; 
	}
}
