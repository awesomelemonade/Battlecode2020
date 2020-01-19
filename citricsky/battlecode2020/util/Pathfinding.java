package citricsky.battlecode2020.util;

import battlecode.common.*;

public class Pathfinding {
	private static RobotController controller;
	private static FastIntSet2D visitedSet;
	private static MapLocation lastTarget;

	public static void init(RobotController controller) {
		Pathfinding.controller = controller;
		visitedSet = new FastIntSet2D(controller.getMapWidth(), controller.getMapHeight());
	}
	// Assumes landscaping is not a possibility and it's not a simple drone
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
		if (!checkDirtDifference(location)) {
			if (Cache.ROBOT_TYPE == RobotType.LANDSCAPER) {
				// TODO: Try terraform
				return false;
			} else {
				return false;
			}
		}
		if (controller.canMove(direction)) {
			controller.move(direction);
		}
		return true;
	}
	private static boolean checkDirtDifference(MapLocation location) throws GameActionException {
		if (Cache.ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			return true;
		}
		if (!controller.canSenseLocation(location)) {
			return false;
		}
		return Math.abs(controller.senseElevation(Cache.CURRENT_LOCATION) -
				controller.senseElevation(location)) <= GameConstants.MAX_DIRT_DIFFERENCE;
	}
}
