package citricsky.battlecode2020.util;

import java.util.Arrays;

public class FastIntSet {
	private int[] array;
	private int counter;
	public FastIntSet(int size) {
		this.array = new int[size];
	}
	public void reset() {
		this.counter++;
	}
	public void add(int x) {
		this.array[x] = counter;
	}
	public boolean contains(int x) {
		return this.array[x] == counter;
	}
}
