package citricsky.battlecode2020;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Util;

public class NetGunBot implements RunnableBot {
	private RobotController controller;
	public NetGunBot(RobotController controller) {
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
	}
	public boolean tryShootDrone() throws GameActionException {
		RobotInfo[] enemies = controller.senseNearbyRobots(Cache.CURRENT_LOCATION,
				GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, controller.getTeam().opponent());
		int bestDistance = Integer.MAX_VALUE;
		RobotInfo bestTarget = null;
		for (RobotInfo enemy : enemies) {
			int enemyID = enemy.getID();
			int enemyDistance = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
			if (enemyDistance < bestDistance) {
				if (controller.canShootUnit(enemyID)) {
					bestDistance = enemyDistance;
					bestTarget = enemy;
				}
			}
		}
		if (bestTarget != null) {
			controller.shootUnit(bestTarget.getID());
			return true;
		}
		return false;
	}
}
