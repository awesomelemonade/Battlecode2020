package citricsky.battlecode2020;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfinding {
	private RobotController controller;
	private boolean bugPathing = false;
	public Pathfinding(RobotController controller) {
		this.controller = controller;
	}


	public void execute(MapLocation target) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (!bugPathing) {
			Direction direction = currentLocation.directionTo(target);
			if (controller.canMove(direction)) {
				controller.move(direction);
				return;
			}
		}
		// Bug pathing
	}
}
