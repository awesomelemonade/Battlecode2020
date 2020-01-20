package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DroneBot implements RunnableBot {
	private RobotController controller;
	private boolean carryingAllyLandscaper;
	private boolean sent;
	public DroneBot(RobotController controller) {
		this.controller = controller;
		this.carryingAllyLandscaper = false;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!sent) {
			SharedInfo.builtNewDrone();
			sent = true;
		}
		if (!controller.isReady()) {
			return;
		}
		System.out.println("Drone State: " + SharedInfo.attackState + " - " + Pathfinding.ignoreNetGuns);
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (controller.isCurrentlyHoldingUnit()) {
			if (carryingAllyLandscaper) {
				// Try to drop the landscaper next to the enemy hq
				MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
				if (enemyHQ == null) {
					Util.randomExplore();
					return;
				}
				for (Direction direction : Util.ADJACENT_DIRECTIONS) {
					MapLocation location = Cache.CURRENT_LOCATION.add(direction);
					if (enemyHQ.isWithinDistanceSquared(location, Util.ADJACENT_DISTANCE_SQUARED)) {
						if (controller.canDropUnit(direction)) {
							controller.dropUnit(direction);
							carryingAllyLandscaper = false;
							return;
						}
					}
				}
				goTowardsEnemyHQ();
			} else {
				// Find adjacent water
				if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
					for (Direction direction : Util.ADJACENT_DIRECTIONS) {
						MapLocation location = currentLocation.add(direction);
						if (Util.onTheMap(location) && controller.senseFlooding(location)) {
							if (controller.canDropUnit(direction)) {
								controller.dropUnit(direction);
								return;
							}
						}
					}
				}
				// Find Water
				for (int i = 1; i < Util.FLOOD_FILL_DX.length; i++) { // Don't look for water directly underneath
					int dx = Util.FLOOD_FILL_DX[i];
					int dy = Util.FLOOD_FILL_DY[i];
					MapLocation location = currentLocation.translate(dx, dy);
					if (!Util.onTheMap(location)) {
						continue;
					}
					if (!controller.canSenseLocation(location)) {
						break;
					}
					if (controller.senseFlooding(location)) {
						// Move towards location
						Pathfinding.execute(location);
						return;
					}
				}
				Util.randomExplore();
			}
		} else {
			// Not currently holding any unit
			int attackState = SharedInfo.attackState;
			if (attackState == SharedInfo.ATTACK_STATE_ENEMYHQ || attackState == SharedInfo.ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS) {
				goTowardsEnemyHQ();
			} else if (attackState == SharedInfo.ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS) {
				int bestDistanceSquared = Integer.MAX_VALUE;
				RobotInfo best = null;
				for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
					if (!ally.getType().equals(RobotType.LANDSCAPER)) {
						continue;
					}
					int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(ally.getLocation());
					if (distanceSquared < bestDistanceSquared) {
						best = ally;
						bestDistanceSquared = distanceSquared;
					}
				}
				if (best != null) {
					if (currentLocation.isAdjacentTo(best.getLocation())) {
						if (controller.canPickUpUnit(best.getID())) {
							controller.pickUpUnit(best.getID());
							carryingAllyLandscaper = true;
						}
					} else {
						Pathfinding.execute(best.getLocation());
					}
				} else {
					goTowardsEnemyHQ();
				}
			} else { // attackState == SharedInfo.ATTACK_STATE_NONE
				RobotInfo target = findBestEnemyTarget();
				if (target == null) {
					Util.randomExplore();
				} else {
					if (currentLocation.isAdjacentTo(target.getLocation())) {
						if (controller.canPickUpUnit(target.getID())) {
							controller.pickUpUnit(target.getID());
							carryingAllyLandscaper = false;
						}
					} else {
						Pathfinding.execute(target.getLocation());
					}
				}
			}
		}
	}
	public void goTowardsEnemyHQ() throws GameActionException {
		// Go towards enemy HQ
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		if (enemyHQ == null) {
			Util.randomExplore();
		} else {
			Pathfinding.execute(enemyHQ);
		}
	}
	public RobotInfo findBestEnemyTarget() {
		RobotInfo best = null;
		int bestPriority = -1;
		int bestDistanceSquared = -1;
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			int priority = getPriority(enemy.getType());
			if (priority == 0) {
				// Cannot pick up
				continue;
			}
			int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
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
