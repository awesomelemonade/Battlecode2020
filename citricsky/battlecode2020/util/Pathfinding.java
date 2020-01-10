package citricsky.battlecode2020.util;

import battlecode.common.*;

public class Pathfinding {
	private static RobotController controller;
	private boolean bugPathing = false;
	private Direction currentDirection;
	private FastIntSet visitedSet;
	private MapLocation bestLocation;
	private int bestDistanceSquared;

	public static void init(RobotController controller) {
		Pathfinding.controller = controller;
	}
	public Pathfinding() {
		this.visitedSet = new FastIntSet(controller.getMapWidth() * controller.getMapHeight());
	}
	public void reset() {
		this.currentDirection = null;
		this.bugPathing = false;
		this.bestLocation = null;
		this.bestDistanceSquared = Integer.MAX_VALUE;
		this.visitedSet.reset();
	}
	// Assumes landscaping is not a possibility and it's not a simple drone
	public void execute(MapLocation target) throws GameActionException {
		controller.setIndicatorLine(controller.getLocation(), target, 0, 255, 0);
		if (bugPathing) {
			controller.setIndicatorDot(controller.getLocation(), 255, 255, 0);
		}
		if (!controller.isReady()) {
			return;
		}
		combo(target);
	}
	private void combo(MapLocation target) throws GameActionException {
		if (!bugPathing) {
			if (naiveMove(controller.getLocation().directionTo(target))) {
				return;
			}
		}
		bugPathing = true;
		bugPath(target);
	}
	private static boolean checkDirtDifference(MapLocation location) throws GameActionException {
		return Math.abs(controller.senseElevation(controller.getLocation()) -
				controller.senseElevation(location)) <= GameConstants.MAX_DIRT_DIFFERENCE;
	}
	public static boolean naiveMove(Direction direction) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		MapLocation toLocation = currentLocation.add(direction);
		if (!Util.isBlocked(toLocation)) {
			RobotType type = controller.getType();
			boolean dirtDifferenceCheck = checkDirtDifference(toLocation);
			if (type == RobotType.DELIVERY_DRONE || dirtDifferenceCheck) {
				// Let's just sit if there's a robot passing by
				if (!controller.isLocationOccupied(toLocation)) {
					controller.move(direction);
				}
				return true;
			} else {
				// It's not a drone and the elevation difference is too big
				if (type == RobotType.LANDSCAPER) {
					// We can move dirt - should we?
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
	private void bugPath(MapLocation target) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (visited(currentLocation) && currentLocation.equals(bestLocation)) {
			reset();
			combo(target);
			return;
		}
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
			for (int i = 0; i < 8 && (!Pathfinding.naiveMove(currentDirection)); i++) {
				currentDirection = currentDirection.rotateRight();
			}
		} else {
			Direction start = currentDirection.opposite().rotateRight();
			if (Pathfinding.naiveMove(start)) {
				reset();
				return;
			} else {
				start = start.rotateRight();
			}
			for (int i = 0; i < 7 && !Pathfinding.naiveMove(start); i++) {
				start = start.rotateRight();
			}
			currentDirection = start;
		}
	}
	private void addToVisitedSet(MapLocation location) {
		visitedSet.add(location.x * controller.getMapHeight() + location.y);
	}
	private boolean visited(MapLocation location) {
		return visitedSet.contains(location.x * controller.getMapHeight() + location.y);
	}
}
