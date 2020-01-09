package citricsky.battlecode2020.util;

import battlecode.common.MapLocation;

public class InfoMap {
	private static final int BITMASK_LENGTH = 8;
	private static final int BITMASK = 0b11111111;
	private long[][] array;
	public InfoMap(int width, int height) {
		this.array = new long[width][height];
	}
	public void set(MapLocation location, long value) {
		set(location.x, location.y, value);
	}
	public void set(int x, int y, long value) {
		array[x][y] = value;
	}
	public long get(int x, int y) {
		return array[x][y];
	}
}
