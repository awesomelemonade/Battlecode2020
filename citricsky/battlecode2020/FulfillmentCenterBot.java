package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class FulfillmentCenterBot implements RunnableBot {
	private RobotController controller;
	public FulfillmentCenterBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		if (SharedInfo.getOurHQState() == HQBot.NEEDS_HELP) {
			// If hq is in distress, we should probably build landscapers instead
			return;
		}
		if (seeEnemyMinerOrLandscaper()) {
			if (controller.getTeamSoup() < RobotType.DELIVERY_DRONE.cost) {
				return;
			}
		} else {
			if (controller.getTeamSoup() < RobotType.VAPORATOR.cost + SharedInfo.getMissingBuildingsCost() +
					RobotType.DELIVERY_DRONE.cost || Math.random() < 0.5) {
				return;
			}
		}
		if (seeEnemyNetGun()) {
			return;
		}
		Util.trySafeBuildTowardsEnemyHQ(RobotType.DELIVERY_DRONE);
	}
	public static boolean seeEnemyMinerOrLandscaper() {
		for (RobotInfo robot : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER) {
				return true;
			}
		}
		return false;
	}
	public boolean seeEnemyNetGun() {
		for (RobotInfo robot : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (robot.getType() == RobotType.NET_GUN) {
				return true;
			}
		}
		return false;
	}
}
