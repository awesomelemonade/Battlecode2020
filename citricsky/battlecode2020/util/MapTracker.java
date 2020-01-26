package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class MapTracker {
	public static MapLocationArray sharedSoupLocations = new MapLocationArray(100);
	//public static MapLocationArray enemyLocations = new MapLocationArray(100);
	
	public static MapLocation sharedClosestWaterToHQ = null;
	public static MapLocation sharedClosestWaterToEnemyHQ = null;
	public static MapLocation closestWaterToHQ = null;
	public static MapLocation closestWaterToEnemyHQ = null;

	public static void checkValidityOfWaterLocations() throws GameActionException {
		if (sharedClosestWaterToHQ != null) {
			if (Cache.controller.canSenseLocation(sharedClosestWaterToHQ)) {
				if (!Cache.controller.senseFlooding(sharedClosestWaterToHQ)) {
					SharedInfo.sendWaterState(false, sharedClosestWaterToHQ);
					sharedClosestWaterToHQ = null;
				}
			}
		}
		if (sharedClosestWaterToEnemyHQ != null) {
			if (Cache.controller.canSenseLocation(sharedClosestWaterToEnemyHQ)) {
				if (!Cache.controller.senseFlooding(sharedClosestWaterToEnemyHQ)) {
					SharedInfo.sendWaterState(false, sharedClosestWaterToEnemyHQ);
					sharedClosestWaterToEnemyHQ = null;
				}
			}
		}
		if (closestWaterToHQ != null) {
			if (Cache.controller.canSenseLocation(closestWaterToHQ)) {
				if (!Cache.controller.senseFlooding(closestWaterToHQ)) {
					closestWaterToHQ = null;
				}
			}
		}
		if (closestWaterToEnemyHQ != null) {
			if (Cache.controller.canSenseLocation(closestWaterToEnemyHQ)) {
				if (!Cache.controller.senseFlooding(closestWaterToEnemyHQ)) {
					closestWaterToEnemyHQ = null;
				}
			}
		}
	}
	public static void addWaterLocation(MapLocation location) {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		if (closestWaterToHQ == null || ourHQ != null && location.distanceSquaredTo(ourHQ) < closestWaterToHQ.distanceSquaredTo(ourHQ)) {
			closestWaterToHQ = location;
		}
		if (closestWaterToEnemyHQ == null || enemyHQ != null && location.distanceSquaredTo(enemyHQ) < closestWaterToEnemyHQ.distanceSquaredTo(enemyHQ)) {
			closestWaterToEnemyHQ = location;
		}
	}
	public static void addSharedWaterLocation(MapLocation location) {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		if (sharedClosestWaterToHQ == null || ourHQ != null && location.distanceSquaredTo(ourHQ) < sharedClosestWaterToHQ.distanceSquaredTo(ourHQ)) {
			sharedClosestWaterToHQ = location;
			SharedInfo.sendWaterState(true, location);
			return; // Prevent sending it twice
		}
		if (sharedClosestWaterToEnemyHQ == null || enemyHQ != null && location.distanceSquaredTo(enemyHQ) < sharedClosestWaterToEnemyHQ.distanceSquaredTo(enemyHQ)) {
			sharedClosestWaterToEnemyHQ = location;
			SharedInfo.sendWaterState(true, location);
			return; // Prevent sending it twice
		}
	}
}
