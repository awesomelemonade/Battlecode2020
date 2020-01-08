package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinding {
	private RobotController controller;
	private boolean bugPathing = false;
	private Direction currentDirection;
	private FastIntSet visitedSet;
	private MapLocation bestLocation;
	private int bestDistanceSquared;

	public Pathfinding(RobotController controller) {
		this.controller = controller;
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
		if (!controller.isReady()) {
			return;
		}
		combo(target);
		if (bugPathing) {
			controller.setIndicatorDot(controller.getLocation(), 255, 255, 0);
		}
	}
	private void combo(MapLocation target) throws GameActionException {
		if (!bugPathing) {
			if (naiveGoTowards(target)) {
				return;
			}
		}
		bugPath(target);
		bugPathing = true;
	}
	private boolean naiveGoTowards(MapLocation target) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		Direction direction = currentLocation.directionTo(target);
		if (Util.canSafeMove(direction)) {
			controller.move(direction);
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
			for (int i = 0; i < 8 && (!Util.canSafeMove(currentDirection)); i++) {
				currentDirection = currentDirection.rotateRight();
			}
		} else {
			Direction start = currentDirection.opposite().rotateRight();
			for (int i = 0; i < 8 && !(!Util.canSafeMove(start.rotateLeft()) &&
					Util.canSafeMove(start)); i++) {
				start = start.rotateRight();
			}
			currentDirection = start;
		}
		if (Util.canSafeMove(currentDirection)) {
			controller.move(currentDirection);
		}
	}
	private void addToVisitedSet(MapLocation location) {
		visitedSet.add(location.x * controller.getMapHeight() + location.y);
	}
	private boolean visited(MapLocation location) {
		return visitedSet.contains(location.x * controller.getMapHeight() + location.y);
	}
}
