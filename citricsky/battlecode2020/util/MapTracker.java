package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MapTracker {
	
	public static MapLocationArray soupLocations = new MapLocationArray(100);
	//public static MapLocationArray enemyLocations = new MapLocationArray(100);
	
	public static MapLocation closestWaterToHQ = null;
	public static MapLocation closestWaterToEnemyHQ = null;
	
	public static void updateWaterLocations() throws GameActionException {
		for (Direction direction : Util.ADJACENT_DIRECTIONS) {
			MapLocation adjacentLocation = Cache.CURRENT_LOCATION.add(direction);
			if (Cache.controller.canSenseLocation(adjacentLocation) && Util.onTheMap(adjacentLocation)) {
				if (Cache.controller.senseFlooding(adjacentLocation)) {
					if (closestWaterToHQ == null) {
						SharedInfo.sendWaterState(adjacentLocation, 1, 1);
					} else if (adjacentLocation.distanceSquaredTo(SharedInfo.getOurHQLocation()) < closestWaterToHQ.distanceSquaredTo(SharedInfo.getOurHQLocation())) {
						SharedInfo.sendWaterState(adjacentLocation, 1, 1);
					}
					if (closestWaterToEnemyHQ == null) {
						SharedInfo.sendWaterState(adjacentLocation, 1, 0);
					} else if (SharedInfo.getEnemyHQLocation() != null && adjacentLocation.distanceSquaredTo(SharedInfo.getEnemyHQLocation()) < closestWaterToHQ.distanceSquaredTo(SharedInfo.getEnemyHQLocation())) {
						SharedInfo.sendWaterState(adjacentLocation, 1, 0);
					}
				} else {
					if (adjacentLocation == closestWaterToHQ) {
						SharedInfo.sendWaterState(adjacentLocation, 0, 1);
					}
					if (adjacentLocation == closestWaterToEnemyHQ) {
						SharedInfo.sendWaterState(adjacentLocation, 0, 0);
					}
				}
			}
		}
	}
}
