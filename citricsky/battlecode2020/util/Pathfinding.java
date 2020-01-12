package citricsky.battlecode2020.util;

import battlecode.common.*;

public class Pathfinding {
	private static RobotController controller;
	private static boolean bugPathing = false;
	private static Direction currentDirection;
	private static FastIntSet visitedSet;
	private static MapLocation bestLocation;
	private static int bestDistanceSquared;
	private static int bestElevationDifference;
	private static MapLocation lastTarget;

	public static void init(RobotController controller) {
		Pathfinding.controller = controller;
		visitedSet = new FastIntSet(controller.getMapWidth() * controller.getMapHeight());
	}
	public static void softReset() {
		currentDirection = null;
		bugPathing = false;
	}
	public static void reset() {
		softReset();
		bestLocation = null;
		bestDistanceSquared = Integer.MAX_VALUE;
		bestElevationDifference = Integer.MAX_VALUE;
		visitedSet.reset();
	}
	// Assumes landscaping is not a possibility and it's not a simple drone
	public static void execute(MapLocation target) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		controller.setIndicatorLine(currentLocation, target, 0, 255, 0);
		if (bugPathing) {
			controller.setIndicatorDot(currentLocation, 255, 255, 0);
		}
		if (lastTarget == null || (!lastTarget.equals(target))) {
			reset();
		}
		lastTarget = target;
		if (!controller.isReady()) {
			return;
		}
		Direction direction = combo(currentLocation, target, currentDirection, false);
		if (direction != null && direction != Direction.CENTER) {
			if (controller.canMove(direction)) {
				controller.move(direction);
			} else {
				MapLocation toLocation = currentLocation.add(direction);
				if (!checkDirtDifference(currentLocation, toLocation)) {
					moveDirt(direction);
				}
			}
		}
	}
	private static Direction combo(MapLocation currentLocation, MapLocation target,
								   Direction currentDirection, boolean canMoveDirt) throws GameActionException {
		if (!bugPathing) {
			Direction direction = currentLocation.directionTo(target);
			if (canNaiveMove(currentLocation, direction, canMoveDirt)) {
				return direction;
			}
		}
		bugPathing = true;
		return bugPath(currentLocation, target, currentDirection);
	}
	private static boolean checkDirtDifference(MapLocation a, MapLocation b) throws GameActionException {
		return Math.abs(controller.senseElevation(a) -
				controller.senseElevation(b)) <= GameConstants.MAX_DIRT_DIFFERENCE;
	}

	/**
	 * Can move in a certain direction from a certain location, and whether moving dirt counts as a move
	 */
	public static boolean canNaiveMove(MapLocation currentLocation, Direction direction, boolean canMoveDirt) throws GameActionException {
		MapLocation toLocation = currentLocation.add(direction);
		if (!Util.isBlocked(toLocation)) {
			RobotType type = controller.getType();
			boolean dirtDifferenceCheck = checkDirtDifference(currentLocation, toLocation);
			if (type == RobotType.DELIVERY_DRONE || dirtDifferenceCheck) {
				// Let's just sit if there's a robot passing by
				return true;
			} else {
				// It's not a drone and the elevation difference is too big
				if (canMoveDirt && type == RobotType.LANDSCAPER) {
					// We can move dirt - should we?
					if (controller.canSenseLocation(currentLocation) && controller.canSenseLocation(toLocation)) {
						int fromElevation = controller.senseElevation(currentLocation);
						int toElevation = controller.senseElevation(toLocation);
						int turnsToFlooded = Math.min(Util.getTurnsToFlooded(fromElevation),
								Util.getTurnsToFlooded(toElevation));
						int dirtDifference = Math.max(0,
								Math.abs(fromElevation - toElevation) - GameConstants.MAX_DIRT_DIFFERENCE);
						boolean moveDirt = dirtDifference * 3 < turnsToFlooded;
						if (!moveDirt) {
							boolean lowerTileNotNearWater = true;
							MapLocation lower = fromElevation < toElevation ? currentLocation : toLocation;
							for (Direction tempDirection : Util.ADJACENT_DIRECTIONS) {
								MapLocation tempLocation = lower.add(tempDirection);
								if (controller.canSenseLocation(tempLocation) && controller.senseFlooding(tempLocation)) {
									lowerTileNotNearWater = false;
									break;
								}
							}
							moveDirt = lowerTileNotNearWater;
						}
						if (moveDirt) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	private static boolean moveDirt(Direction direction) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		MapLocation toLocation = currentLocation.add(direction);
		if (controller.canSenseLocation(currentLocation) && controller.canSenseLocation(toLocation)) {
			int fromElevation = controller.senseElevation(currentLocation);
			int toElevation = controller.senseElevation(toLocation);
			if (fromElevation < toElevation) {
				if (moveDirt(direction, Direction.CENTER)) {
					return true;
				}
			} else {
				if (moveDirt(Direction.CENTER, direction)) {
					return true;
				}
			}
		}
		return false;
	}
	private static boolean moveDirt(Direction from, Direction to) throws GameActionException {
		// Place dirt in lower elevation
		if (controller.canDepositDirt(to)) {
			controller.depositDirt(to);
			return true;
		}
		// Dig dirt from higher elevation
		if (controller.canDigDirt(from)) {
			controller.digDirt(from);
			return true;
		}
		return false;
	}
	private static void checkBest(MapLocation from, MapLocation to, MapLocation target) throws GameActionException {
		addToVisitedSet(from);
		int distanceSquared = from.distanceSquaredTo(target);
		int elevationDifference = Integer.MAX_VALUE;
		if (controller.canSenseLocation(from) && controller.canSenseLocation(to)) {
			elevationDifference = Math.abs(controller.senseElevation(from) - controller.senseElevation(to));
		}
		if (distanceSquared < bestDistanceSquared ||
				(distanceSquared == bestDistanceSquared && elevationDifference < bestElevationDifference)) {
			bestLocation = from;
			bestDistanceSquared = distanceSquared;
			bestElevationDifference = elevationDifference;
		}
	}
	private static Direction bugPath(MapLocation currentLocation, MapLocation target, Direction currentDirection) throws GameActionException {
		if (visited(currentLocation) && currentLocation.equals(bestLocation)) {
			softReset();
			Direction direction = combo(target, currentDirection, true);
			// Add new location to visited
			if (direction != null) {
				MapLocation to = currentLocation.add(direction);
				Direction newDirection = combo(target, direction, true);
				if (newDirection != null) {
					MapLocation newLocation = to.add(newDirection);
					checkBest(to, newLocation, target);
				}
			}
			return direction;
		}
		// TODO: convert to checkBest
		addToVisitedSet(currentLocation);
		int distanceSquared = target.distanceSquaredTo(currentLocation);
		if (distanceSquared < bestDistanceSquared) {
			bestLocation = currentLocation;
			bestDistanceSquared = distanceSquared;
		}
		if (currentDirection == null) {
			currentDirection = currentLocation.directionTo(target);
			// Follows the wall with left hand
			// This for loop ensures we're not in an infinite loop (stuck in a 1x1 square)
			Direction success = null;
			for (int i = 0; i < 8 && ((success = Pathfinding.naiveMove(currentDirection, false)) == null); i++) {
				currentDirection = currentDirection.rotateRight();
			}
			if (success != null) {
				return currentDirection;
			}
		} else {
			Direction start = currentDirection.opposite().rotateRight();
			if (Pathfinding.naiveMove(start, false) != null) {
				// We can probably naiveMove again
				reset();
				return start;
			} else {
				start = start.rotateRight();
			}
			Direction success = null;
			for (int i = 0; i < 7 && ((success = Pathfinding.naiveMove(start, false)) == null); i++) {
				start = start.rotateRight();
			}
			currentDirection = start;
			if (success != null) {
				return currentDirection;
			}
		}
		return null;
	}
	private static void addToVisitedSet(MapLocation location) {
		visitedSet.add(location.x * controller.getMapHeight() + location.y);
	}
	private static boolean visited(MapLocation location) {
		return visitedSet.contains(location.x * controller.getMapHeight() + location.y);
	}
}
