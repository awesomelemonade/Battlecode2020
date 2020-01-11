package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class MinerBot implements RunnableBot {
	private RobotController controller;
	public MinerBot(RobotController controller) {
		this.controller = controller;
	}
	private MapLocation hqLocation;

	@Override
	public void init() {
		for (RobotInfo robot : controller.senseNearbyRobots(-1, controller.getTeam())) {
			if (robot.type == RobotType.HQ) {
				hqLocation = robot.getLocation();
				break;
			}
		}
	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
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
				// try build design school
				if (!tryBuild(RobotType.DESIGN_SCHOOL)) {
					// Otherwise, walk randomly
					Util.randomExplore();
				}
			} else {
				Pathfinding.execute(soupLocation);
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
			Pathfinding.execute(hqLocation);
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
	private boolean spawned = false;
	public boolean tryBuild(RobotType type) throws GameActionException {
		// TODO: temporary hack to make sure landscapers spawn before more design schools
		if (spawned || controller.getTeamSoup() < 200) {
			return false;
		}
		Direction direction = Util.randomAdjacentDirection();
		MapLocation location = controller.getLocation().add(direction);
		if ((location.x + location.y) % 2 == 0) {
			if (Util.canSafeBuildRobot(type, direction)) {
				controller.buildRobot(type, direction);
				spawned = true;
				return true;
			}
		}
		return false;
	}
}
