package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.MapTracker;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DroneBot implements RunnableBot {
	public static final int PREPARED_TO_RUSH_DISTANCE_SQUARED = 100;
	public static boolean isReadyForAttack = false;
	private boolean sent = false;
	private RobotController controller;
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
		if (!sent) {
			SharedInfo.builtNewDrone();
			sent = true;
		}
		if (!isReadyForAttack) {
			MapLocation enemyHQLocation = SharedInfo.getEnemyHQLocation();
			if (enemyHQLocation != null &&
					Cache.CURRENT_LOCATION.isWithinDistanceSquared(enemyHQLocation, PREPARED_TO_RUSH_DISTANCE_SQUARED)) {
				SharedInfo.sendDroneReady();
				isReadyForAttack = true;
			}
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		// Check MapTracker
		MapLocation closestWaterInVision = null;
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
				if (closestWaterInVision == null) {
					closestWaterInVision = location;
				}
				MapTracker.addWaterLocation(location);
			}
		}


		if (!controller.isReady()) {
			return;
		}

		if (!Pathfinding.ignoreNetGuns) {
			// Try kite net guns if they are too close
			int closestNetGunDistanceSquared = Integer.MAX_VALUE;
			MapLocation closestNetGun = null;
			for (int i = Cache.ALL_NEARBY_ENEMY_NET_GUNS_SIZE; --i >= 0; ) {
				MapLocation enemyNetGun = Cache.ALL_NEARBY_ENEMY_NET_GUNS[i];
				int distanceSquared = currentLocation.distanceSquaredTo(enemyNetGun);
				if (distanceSquared < closestNetGunDistanceSquared) {
					closestNetGunDistanceSquared = distanceSquared;
					closestNetGun = enemyNetGun;
				}
			}
			// Kiting
			if (closestNetGunDistanceSquared <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
				// Don't pathfind because all directions are not pathfindable
				Util.tryKiteAwayFrom(closestNetGun, false);
				return;
			}
		}

		// AI logic
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
								MapTracker.addSharedWaterLocation(location);
								return;
							}
						}
					}
				}
				if (closestWaterInVision != null) {
					Pathfinding.execute(closestWaterInVision);
					return;
				}
				// Use water location in MapTracker
				MapLocation location = findClosestStoredWaterTile();
				if (location == null) {
					Util.randomWalk();
				} else {
					Pathfinding.execute(location);
				}
			}
		} else {
			// Not currently holding any unit
			RobotInfo target = findBestEnemyTarget();
			if (SharedInfo.getOurHQState() == HQBot.NO_HELP_NEEDED) {
				if (target == null) {
					target = findCowTarget();
				}
			}
			int attackState = SharedInfo.getAttackState();
			if (target == null) {
				// Pick up our own landscapers for attack
				if (attackState == SharedInfo.ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS) {
					int bestDistanceSquared = Integer.MAX_VALUE;
					RobotInfo best = null;
					MapLocation ourHQLocation = SharedInfo.getOurHQLocation();
					if (ourHQLocation != null) {
						for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
							if (!ally.getType().equals(RobotType.LANDSCAPER)) {
								continue;
							}
							MapLocation allyLocation = ally.getLocation();
							if (ourHQLocation.isAdjacentTo(allyLocation)) {
								continue;
							}
							int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(allyLocation);
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
							return;
						}
					}
				}
			}
			if (target == null) {
				if (attackState != SharedInfo.ATTACK_STATE_NONE) {
					goTowardsEnemyHQ();
				} else {
					if (SharedInfo.getOurHQState() != HQBot.NO_HELP_NEEDED) {
						goTowardsOurHQ();
					} else {
						goTowardsEnemyHQ();
					}
				}
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
	public void goTowardsOurHQ() throws GameActionException {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		if (ourHQ == null) {
			Util.randomExplore();
		} else {
			Pathfinding.execute(ourHQ);
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
			int priority = getPriority(enemy.getLocation(), enemy.getType());
			if (priority == 0) {
				// Cannot pick up
				continue;
			}
			int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
			if (distanceSquared <= Util.ADJACENT_DISTANCE_SQUARED) {
				priority += 1000;
			}
			if (best == null || priority > bestPriority || (priority == bestPriority && distanceSquared < bestDistanceSquared)) {
				best = enemy;
				bestPriority = priority;
				bestDistanceSquared = distanceSquared;
			}
		}
		return best;
	}
	public RobotInfo findCowTarget() {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		if (ourHQ == null || enemyHQ == null) {
			return null;
		}
		RobotInfo best = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
		for (RobotInfo robot : Cache.ALL_NEARBY_ROBOTS) {
			if (robot.getTeam() == Team.NEUTRAL) {
				MapLocation location = robot.getLocation();
				int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(robot.getLocation());
				if (distanceSquared < bestDistanceSquared) {
					// Only pick up cows on our side of the map
					if (Cache.controller.getRoundNum() > 1000 ||
							(ourHQ.distanceSquaredTo(location) < enemyHQ.distanceSquaredTo(location))) {
						best = robot;
						bestDistanceSquared = distanceSquared;
					}
				}
			}
		}
		return best;
	}
	public int getPriority(MapLocation location, RobotType type) {
		if (controller.getRoundNum() < 2500) {
			MapLocation ourHQ = SharedInfo.getOurHQLocation();
			MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
			if (ourHQ != null && enemyHQ != null) {
				if (enemyHQ.distanceSquaredTo(location) < ourHQ.distanceSquaredTo(location)) {
					switch (type) {
						case MINER:
							return 2;
						case LANDSCAPER:
							return 1;
						default:
							// Cannot pick up
							return 0;
					}
				}
			}
		}
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
	public MapLocation findClosestStoredWaterTile() {
		MapLocation closestLocation = null;
		int closestDistanceSquared = Integer.MAX_VALUE;
		if (MapTracker.closestWaterToHQ != null) {
			int distanceSquared = MapTracker.closestWaterToHQ.distanceSquaredTo(Cache.CURRENT_LOCATION);
			if (distanceSquared < closestDistanceSquared) {
				closestLocation = MapTracker.closestWaterToHQ;
				closestDistanceSquared = distanceSquared;
			}
		}
		if (MapTracker.closestWaterToEnemyHQ != null) {
			int distanceSquared = MapTracker.closestWaterToEnemyHQ.distanceSquaredTo(Cache.CURRENT_LOCATION);
			if (distanceSquared < closestDistanceSquared) {
				closestLocation = MapTracker.closestWaterToEnemyHQ;
				closestDistanceSquared = distanceSquared;
			}
		}
		if (MapTracker.sharedClosestWaterToHQ != null) {
			int distanceSquared = MapTracker.sharedClosestWaterToHQ.distanceSquaredTo(Cache.CURRENT_LOCATION);
			if (distanceSquared < closestDistanceSquared) {
				closestLocation = MapTracker.sharedClosestWaterToHQ;
				closestDistanceSquared = distanceSquared;
			}
		}
		if (MapTracker.sharedClosestWaterToEnemyHQ != null) {
			int distanceSquared = MapTracker.sharedClosestWaterToEnemyHQ.distanceSquaredTo(Cache.CURRENT_LOCATION);
			if (distanceSquared < closestDistanceSquared) {
				closestLocation = MapTracker.sharedClosestWaterToEnemyHQ;
				closestDistanceSquared = distanceSquared;
			}
		}
		return closestLocation;
	}
}
