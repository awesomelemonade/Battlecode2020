package citricsky.battlecode2020.util;

import battlecode.common.MapLocation;

public class InfoMap {
	private static final int BITMASK_LENGTH = 8;
	private static final int BITMASK = 0b11111111;
	private long[] array;
	private int width;
	private int height;
	public InfoMap(int width, int height) {
		this.width = width;
		this.height = height;
		this.array = new long[(width * height + 3) / 4]; // rounded up

	}
	public void set(MapLocation location, long value) {
		set(location.x, location.y, value);
	}
	public void set(int x, int y, long value) {
		int index = (x * height + y);
		array[index / 4] |= (value << ((index % 4) * BITMASK_LENGTH));
	}
	public long get(int x, int y) {
		int index = (x * height + y);
		return (array[index / 4] >>> ((index % 4) * BITMASK_LENGTH)) & BITMASK;
	}
}
