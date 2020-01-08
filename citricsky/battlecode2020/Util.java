package citricsky.battlecode2020;

import battlecode.common.Direction;

public class Util {
	public static final Direction[] ADJACENT_DIRECTIONS = new Direction[] {
			Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
	};
	public static int randomInt() {
		return Float.floatToRawIntBits((float) Math.random());
	}
}
