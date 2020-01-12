package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DroneBot implements RunnableBot {
	private RobotController controller;
	public DroneBot(RobotController controller) {
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
		MapLocation currentLocation = controller.getLocation();
		if (controller.isCurrentlyHoldingUnit()) {
			MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
			if (enemyHQ == null) {
				Util.randomExplore();
			} else {
				if (currentLocation.isWithinDistanceSquared(enemyHQ, 25)) {
					Direction idealDirection = currentLocation.directionTo(enemyHQ);
					for (Direction direction : Util.getAttemptOrder(idealDirection)) {
						if ((!Util.isFlooding(direction)) && controller.canDropUnit(direction)) {
							controller.dropUnit(direction);
							return;
						}
					}
				}
				Pathfinding.execute(enemyHQ);
				return;
			}
		} else {
			int id = SharedInfo.getAttackerMinerId();
			if (id != 0) {
				if (controller.canPickUpUnit(id)) {
					controller.pickUpUnit(id);
				} else {
					RobotInfo robot = controller.senseRobot(id);
					if (robot != null) {
						Pathfinding.bug0(robot.getLocation());
					}
				}
			}

			// Find target
			RobotInfo target = findBestTarget();
			// Try to pick them up
			// Drop them to water

		}
	}
	public RobotInfo findBestTarget() {
		RobotInfo best = null;
		int bestPriority = -1;
		int bestDistanceSquared = -1;
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			int priority = getPriority(enemy.getType());
			if (priority == 0) {
				// Cannot pick up
				continue;
			}
			int distanceSquared = controller.getLocation().distanceSquaredTo(enemy.getLocation());
			if (best == null || priority > bestPriority || (priority == bestPriority && distanceSquared < bestDistanceSquared)) {
				best = enemy;
				bestPriority = priority;
				bestDistanceSquared = distanceSquared;
			}
		}
		return best;
	}
	public int getPriority(RobotType type) {
		switch (type) {
			case LANDSCAPER:
				return 2;
			case MINER:
				return 1;
			default:
				// Cannot pick up
				return 0;
		}
	}
}
