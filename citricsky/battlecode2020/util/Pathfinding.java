package citricsky.battlecode2020.util;

import battlecode.common.*;
import citricsky.battlecode2020.LandscaperBot;

public class Pathfinding {
	public static boolean ignoreNetGuns = false;
	private static RobotController controller;
	private static FastIntSet2D visitedSet;
	private static MapLocation lastTarget;

	public static void init(RobotController controller) {
		Pathfinding.controller = controller;
		visitedSet = new FastIntSet2D(controller.getMapWidth(), controller.getMapHeight());
	}
	public static void execute(MapLocation target) throws GameActionException {
		if (lastTarget == null || !lastTarget.equals(target)) {
			lastTarget = target;
			visitedSet.reset();
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		controller.setIndicatorLine(currentLocation, target, 0, 255, 0);
		if (!controller.isReady()) {
			return;
		}
		if (currentLocation.equals(target)) {
			// We're already there
			return;
		}
		visitedSet.add(currentLocation.x, currentLocation.y);
		Direction idealDirection = currentLocation.directionTo(target);
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = currentLocation.add(direction);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (visitedSet.contains(location.x, location.y)) {
				continue;
			}
			if (naiveMove(direction)) {
				return;
			}
		}
		// We stuck bois
		visitedSet.reset();
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = currentLocation.add(direction);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (naiveMove(direction)) {
				return;
			}
		}
	}
	public static boolean naiveMove(Direction direction) throws GameActionException {
		MapLocation location = Cache.CURRENT_LOCATION.add(direction);
		if (Util.isBlocked(location)) {
			return false;
		}
		if (Cache.ROBOT_TYPE != RobotType.DELIVERY_DRONE && LatticeUtil.isPit(location)) {
			// TODO - Miners do not need to avoid pits all the time
			return false;
		}
		if (!controller.canSenseLocation(location)) {
			return false;
		}
		if (Cache.ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			if (!ignoreNetGuns) {
				for (int i = Cache.ALL_NEARBY_ENEMY_NET_GUNS_SIZE; --i >= 0; ) {
					MapLocation netgunLocation = Cache.ALL_NEARBY_ENEMY_NET_GUNS[i];
					if (location.isWithinDistanceSquared(netgunLocation, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
						return false;
					}
				}
			}
		} else {
			int currentElevation = controller.senseElevation(Cache.CURRENT_LOCATION);
			int toElevation = controller.senseElevation(location);
			if (Math.abs(currentElevation - toElevation) > GameConstants.MAX_DIRT_DIFFERENCE) {
				if (Cache.ROBOT_TYPE == RobotType.LANDSCAPER) {
					// Try terraform
					int lower = LandscaperBot.getRealTargetElevation();
					int upper = lower + GameConstants.MAX_DIRT_DIFFERENCE;
					// Figure out which one is more out of line - currentElevation or toElevation
					int currentDifference = calculateDifference(currentElevation, lower, upper);
					int toDifference = calculateDifference(currentElevation, lower, upper);
					if (toDifference > currentDifference) {
						// Deposit/dig from to, then deposit/dig from current
						if (tryTerraform(location, toElevation, lower, upper)) {
							return true;
						}
						if (tryTerraform(Cache.CURRENT_LOCATION, currentElevation, lower, upper)) {
							return true;
						}
					} else {
						// Deposit/dig from current, then deposit/dig from to
						if (tryTerraform(Cache.CURRENT_LOCATION, currentElevation, lower, upper)) {
							return true;
						}
						if (tryTerraform(location, toElevation, lower, upper)) {
							return true;
						}
					}
				} else {
					return false;
				}
			}
		}
		if (controller.canMove(direction)) {
			controller.move(direction);
		}
		return true;
	}
	private static boolean tryTerraform(MapLocation location, int elevation, int lower, int upper) throws GameActionException {
		Direction direction = Cache.CURRENT_LOCATION.directionTo(location);
		if (elevation < lower) {
			if (controller.canDepositDirt(direction)) {
				controller.depositDirt(direction);
				return true;
			}
		} else { // if (elevation > upper)
			if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
				return true;
			}
		}
		return false;
	}
	private static int calculateDifference(int elevation, int lower, int upper) {
		if (elevation < lower) {
			return lower - elevation;
		}
		if (elevation > upper) {
			return elevation - upper;
		}
		return 0;
	}
}
