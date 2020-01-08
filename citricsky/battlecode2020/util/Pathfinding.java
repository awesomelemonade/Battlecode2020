package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinding {
	private RobotController controller;
	private boolean bugPathing = false;
	private Direction currentDirection;

	public Pathfinding(RobotController controller) {
		this.controller = controller;
	}
	public void reset() {
		this.currentDirection = null;
		this.bugPathing = false;
	}
	// Assumes landscaping is not a possibility and it's not a simple drone
	public void execute(MapLocation target) throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = controller.getLocation();
		if (!bugPathing) {
			Direction direction = currentLocation.directionTo(target);
			if (controller.canMove(direction)) {
				controller.move(direction);
				return;
			}
		}
		// Bug pathing
		bugPath(target);
		bugPathing = true;
	}
	private void bugPath(MapLocation target) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (currentDirection == null) {
			currentDirection = currentLocation.directionTo(target);
			// Follows the wall with left hand
			// This for loop ensures we're not in an infinite loop (stuck in a 1x1 square)
			for (int i = 0; i < 8 && (!controller.canMove(currentDirection)); i++) {
				currentDirection = currentDirection.rotateRight();
			}
		} else {
			Direction start = currentDirection.opposite().rotateRight();
			for (int i = 0; i < 8 && !(!controller.canMove(start.rotateLeft()) && controller.canMove(start)); i++) {
				start = start.rotateRight();
			}
			currentDirection = start;
		}
		if (controller.canMove(currentDirection)) {
			controller.move(currentDirection);
		}
	}
}
