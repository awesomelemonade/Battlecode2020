package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.battlecode2020.util.Util;

public class HQBot implements RunnableBot {
	private RobotController controller;
	private int spawnCount;
	public HQBot(RobotController controller) {
		this.controller = controller;
		this.spawnCount = 0;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = controller.getLocation();
		turn: {
			if (tryShootDrone()) {
				break turn;
			}
			if (spawnCount < 20) {
				for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
					MapLocation location = currentLocation.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
					if (!controller.canSenseLocation(location)) {
						break;
					}
					if (controller.senseSoup(location) > 0) {
						Direction idealDirection = Util.getDirection(Util.FLOOD_FILL_DX_CLAMPED[i], Util.FLOOD_FILL_DY_CLAMPED[i]);
						for (Direction direction : Util.getAttemptOrder(idealDirection)) {
							if (Util.canSafeBuildRobot(RobotType.MINER, direction)) {
								controller.buildRobot(RobotType.MINER, direction);
								this.spawnCount++;
								break;
							}
						}
						break turn;
					}
				}
				if (spawnCount < 3) {
					tryBuildMiner();
				}
			}
		}
	}
	public void tryBuildMiner() throws GameActionException {
		Direction direction = Util.randomAdjacentDirection();
		if (Util.canSafeBuildRobot(RobotType.MINER, direction)) {
			controller.buildRobot(RobotType.MINER, direction);
			this.spawnCount++;
		}
	}
	public boolean tryShootDrone() throws GameActionException {
		RobotInfo[] enemies = controller.senseNearbyRobots(controller.getLocation(),
				GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, controller.getTeam().opponent());
		for (RobotInfo enemy : enemies) {
			int enemyId = enemy.getID();
			if (controller.canShootUnit(enemyId)) {
				controller.shootUnit(enemyId);
				return true;
			}
		}
		return false;
	}
}
