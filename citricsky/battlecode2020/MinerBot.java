package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.Util;

public class MinerBot implements RunnableBot {
	private RobotController controller;
	private Pathfinding pathfinding;
	public MinerBot(RobotController controller) {
		this.controller = controller;
	}
	private MapLocation hqLocation;

	@Override
	public void init() {
		pathfinding = new Pathfinding(controller);
		for (RobotInfo robot : controller.senseNearbyRobots(-1, controller.getTeam())) {
			if (robot.type == RobotType.HQ) {
				hqLocation = robot.getLocation();
				break;
			}
		}
	}
	private MapLocation lastTarget;
	public void pathTowards(MapLocation location) throws GameActionException {
		if (lastTarget == null || (!lastTarget.equals(location))) {
			pathfinding.reset();
		}
		lastTarget = location;
		pathfinding.execute(location);
	}
	@Override
	public void turn() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			// Try to mine soup
			for (Direction direction : Direction.values()) {
				if (controller.canMineSoup(direction)) {
					controller.mineSoup(direction);
					return;
				}
			}
			// Move towards visible soup
			MapLocation soupLocation = findSoupLocation();
			if (soupLocation == null) {
				// TODO: Do random exploration
			} else {
				pathTowards(soupLocation);
			}
		} else {
			// Try to deposit soup
			for (Direction direction : Direction.values()) {
				if (controller.canDepositSoup(direction)) {
					controller.depositSoup(direction, controller.getSoupCarrying());
					return;
				}
			}
			// Move towards HQ or refinery
			pathTowards(hqLocation);
		}
	}
	public MapLocation findSoupLocation() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			MapLocation location = currentLocation.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
			if (!controller.canSenseLocation(location)) {
				break;
			}
			if (controller.senseSoup(location) > 0) {
				return location;
			}
		}
		return null;
	}
}
