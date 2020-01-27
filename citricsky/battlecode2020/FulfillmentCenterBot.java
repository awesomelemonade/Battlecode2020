package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.BuildOrder;
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
		if (controller.getTeamSoup() < RobotType.DELIVERY_DRONE.cost) {
			return;
		}
		if (seeEnemyNetGun()) {
			return;
		}
		RobotInfo enemy = findEnemyMinerOrLandscaper();
		if (enemy == null) {
			if (controller.getTeamSoup() < BuildOrder.getSoupThreshold(RobotType.DELIVERY_DRONE)) {
				return;
			}
		}
		MapLocation location;
		if (enemy != null) {
			location = enemy.getLocation();
		} else if (SharedInfo.getEnemyHQLocation() != null) {
			location = SharedInfo.getEnemyHQLocation();
		} else {
			location = Cache.MAP_CENTER_LOCATION;
		}
		Util.trySafeBuildTowards(RobotType.DELIVERY_DRONE, location);
	}
	public static RobotInfo findEnemyMinerOrLandscaper() {
		RobotInfo bestEnemy = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (enemy.getType() == RobotType.MINER || enemy.getType() == RobotType.LANDSCAPER) {
				int distanceSquared = enemy.getLocation().distanceSquaredTo(Cache.CURRENT_LOCATION);
				if (distanceSquared < bestDistanceSquared) {
					bestDistanceSquared = distanceSquared;
					bestEnemy = enemy;
				}
			}
		}
		return bestEnemy;
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
