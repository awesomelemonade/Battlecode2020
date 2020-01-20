package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DroneBot implements RunnableBot {
	private RobotController controller;
	private Direction lastRandomDirection;
	private boolean carryingAllyLandscaper;
	public DroneBot(RobotController controller) {
		this.controller = controller;
		this.carryingAllyLandscaper = false;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (controller.isCurrentlyHoldingUnit() && carryingAllyLandscaper == false) {
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
		} else {
			if (SharedInfo.attackMode == 2 && carryingAllyLandscaper == false) {
				int bestDistanceSquared = -1;
				RobotInfo best = null;
				for(RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
					if (!ally.getType().equals(RobotType.LANDSCAPER)) {
						continue;
					}
					int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(ally.getLocation());
					if (best == null || distanceSquared < bestDistanceSquared) {
						best = ally;
						bestDistanceSquared = distanceSquared;
					}
				}
				if(best != null) {
					if (currentLocation.isAdjacentTo(best.getLocation())) {
						if (controller.canPickUpUnit(best.getID())) {
							controller.pickUpUnit(best.getID());
							carryingAllyLandscaper = true;
						}
					} else {
						Pathfinding.execute(best.getLocation());
					}
				}
				else {
					Util.randomExplore();
				}
			}
			else {
				RobotInfo target = null;
				if(!controller.isCurrentlyHoldingUnit()) {
					target = findBestTarget();
				}
				if (target == null) {
					if (SharedInfo.getEnemyHQLocation() != null) {
						int distanceToEnemyHQ = Cache.CURRENT_LOCATION.distanceSquaredTo(SharedInfo.getEnemyHQLocation());
						if(distanceToEnemyHQ > 3 && distanceToEnemyHQ < 9) {
							for(Direction dir : Util.ADJACENT_DIRECTIONS) {
								if(Cache.CURRENT_LOCATION.add(dir).isAdjacentTo(SharedInfo.getEnemyHQLocation())) {
									if (controller.canDropUnit(dir)) {
										controller.dropUnit(dir);
										return;
									}
								}
							}
						}
						Pathfinding.execute(SharedInfo.getEnemyHQLocation());
					}
					else {
						Util.randomExplore();
					}
				} else {
					if (currentLocation.isAdjacentTo(target.getLocation())) {
						if (controller.canPickUpUnit(target.getID())) {
							controller.pickUpUnit(target.getID());
						}
					} else {
						Pathfinding.execute(target.getLocation());
					}
				}
			}
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
