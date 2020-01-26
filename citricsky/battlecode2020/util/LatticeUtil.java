package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class LatticeUtil {
	public static boolean isPit(MapLocation location) {
		return (location.x % 2 == SharedInfo.getOurHQParityX() &&
				location.y % 2 == SharedInfo.getOurHQParityY()) ||
				(SharedInfo.ourHQNearCorner && Util.isInCorner(location));
	}
	public static boolean isBuildLocation(MapLocation location) {
		return (location.x % 2 != SharedInfo.getOurHQParityX()) &&
				(location.y % 2 != SharedInfo.getOurHQParityY());
	}
	private static final Direction[][] PIT_DIRECTIONS = {
			{Direction.CENTER},
			{Direction.NORTH, Direction.SOUTH},
			{Direction.WEST, Direction.EAST},
			{Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST}
	};
	public static Direction[] getPitDirections(MapLocation location) {
		int parityX = (location.x - SharedInfo.getOurHQParityX() + 2) % 2;
		int parityY = (location.y - SharedInfo.getOurHQParityY() + 2) % 2;
		switch (parityX) {
			case 0:
				switch(parityY) {
					case 0:
						return PIT_DIRECTIONS[0];
					case 1:
						return PIT_DIRECTIONS[1];
				}
			case 1:
				switch(parityY) {
					case 0:
						return PIT_DIRECTIONS[2];
					case 1:
						return PIT_DIRECTIONS[3];
				}
		}
		throw new IllegalStateException("not possible");
	}
}
