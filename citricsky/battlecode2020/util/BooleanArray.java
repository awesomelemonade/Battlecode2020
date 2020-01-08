package citricsky.battlecode2020.util;

public class BooleanArray {
	private final long[] array;
	/**
	 *
	 * @param length ensure you divide by 64
	 */
	public BooleanArray(int length) {
		this.array = new long[length];
	}
	public void set(int index) {
		array[index / 64] |= (1L << (index % 64));
	}
	public boolean get(int index) {
		return ((array[index / 64] >>> (index % 64)) & 1) == 1;
	}
}
