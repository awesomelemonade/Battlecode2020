package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class HQBot implements RunnableBot {
	private RobotController controller;
	private int spawnCount;
	private int initialSoupCount = 0;
	public HQBot(RobotController controller) {
		this.controller = controller;
		this.spawnCount = 0;
	}
	@Override
	public void init() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (controller.canSenseLocation(location)) {
				initialSoupCount += controller.senseSoup(location);
			} else {
				break;
			}
		}
		SharedInfo.sendOurHQ(currentLocation);
	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = controller.getLocation();
		if (Util.tryShootDrone()) {
			return;
		}
		if (spawnCount < 20 && controller.getRoundNum() > 300 ||
				spawnCount < Math.min(5, Math.max(2, initialSoupCount * 2 / RobotType.MINER.cost / 3))) {
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
					return;
				}
			}
			if (spawnCount < 3) {
				tryBuildMiner();
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
}
