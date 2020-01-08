package citricsky.battlecode2020.util;

public class FastDeque {
	private int[] array;
	private int index;
	private int size;
	public FastDeque(int arraySize) {
		this.array = new int[arraySize];
	}
	public void push(int i) {
		array[(index + (size++)) % array.length] = i;
	}
	public int poll() {
		size--;
		return array[(index++) % array.length];
	}
	public boolean isEmpty() {
		return size == 0;
	}
	public void reset() {
		size = 0;
	}
}
