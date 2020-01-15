package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class LatticeUtil {
	public static boolean isPit(MapLocation location) {
		return location.x % 3 == SharedInfo.getOurHQParityX() &&
				location.y % 3 == SharedInfo.getOurHQParityY();
	}
	public static Direction getPitDirection(MapLocation location) {
		int parityX = (location.x - SharedInfo.getOurHQParityX() + 3) % 3;
		int parityY = (location.y - SharedInfo.getOurHQParityY() + 3) % 3;
		switch (parityX) {
			case 0:
				switch(parityY) {
					case 0:
						return Direction.CENTER;
					case 1:
						return Direction.SOUTH;
					case 2:
						return Direction.NORTH;
					default:
						return null;
				}
			case 1:
				switch(parityY) {
					case 0:
						return Direction.WEST;
					case 1:
						return Direction.SOUTHWEST;
					case 2:
						return Direction.NORTHWEST;
					default:
						return null;
				}
			case 2:
				switch(parityY) {
					case 0:
						return Direction.EAST;
					case 1:
						return Direction.SOUTHEAST;
					case 2:
						return Direction.NORTHEAST;
					default:
						return null;
				}
			default:
				return null;
		}
	}
}
