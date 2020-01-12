package citricsky.battlecode2020.util;

public class FastIntDeque {
	private int[] array;
	private int index;
	private int size;
	public FastIntDeque(int arraySize) {
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
	public int get(int index) {
		return array[(this.index + index) % array.length];
	}
	public int size() {
		return size;
	}
}
