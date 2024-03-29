package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class MinerBot implements RunnableBot {
	private RobotController controller;
	public MinerBot(RobotController controller) {
		this.controller = controller;
	}
	public static MapLocation hqLocation;

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
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		// Kite drones
		if (Util.tryKiteFromAdjacentDrones()) {
			return;
		}
		if (Util.hasLattice) {
			// Check if we're on low elevation - flee to lattice
			if (!hqLocation.isWithinDistanceSquared(Cache.CURRENT_LOCATION, RobotType.HQ.sensorRadiusSquared)) {
				int elevation = controller.senseElevation(Cache.CURRENT_LOCATION);
				if (Util.getTurnsToFlooded(elevation) - controller.getRoundNum() < 50) {
					Pathfinding.execute(hqLocation);
					return;
				}
			}
		}
		// See if we should build net guns
		if (tryBuildNetGun()) {
			return;
		}
		if (localSaveForNetGun) {
			localSaveForNetGun = false;
			SharedInfo.sendSaveForNetgunSignal(false);
		}
		// See if we should be design school, fulfillment center, or vaporator
		RobotType buildTarget = getBuildTypeTarget();
		if (buildTarget != null) {
			if (tryBuildAtBestLocation(buildTarget)) {
				return;
			}
		}
		if (tryDeposit()) {
			return;
		}
		if (SharedInfo.wallState == SharedInfo.WALL_STATE_NEEDS) {
			if (currentLocation.isAdjacentTo(hqLocation)) {
				// Try move away
				for (Direction direction : Util.getAttemptOrder(hqLocation.directionTo(currentLocation))) {
					if (controller.canMove(direction)) {
						controller.move(direction);
						return;
					}
				}
				// Last resort :/
				controller.disintegrate();
			}
		}
		// Try mine soup
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			// Mine adjacent, broadcast location if new
			for (Direction direction : Util.ALL_DIRECTIONS) {
				if (controller.canMineSoup(direction)) {
					controller.mineSoup(direction);
					if (!MapTracker.sharedSoupLocations.contains(currentLocation.add(direction))) {
						SharedInfo.sendSoup(currentLocation.add(direction));
					}
					return;
				}
			}
			// Move towards visible soup
			MapLocation soupLocation = findSoupLocation();
			if (soupLocation != null) {
				Pathfinding.execute(soupLocation);
			} else {
				if (!MapTracker.sharedSoupLocations.isEmpty()) {
					MapLocation nearestSoup = MapTracker.sharedSoupLocations.nearestSoup(currentLocation);
					if (currentLocation.isAdjacentTo(nearestSoup)) {
						// if the soup is gone
						if (controller.senseSoup(nearestSoup) == 0) {
							SharedInfo.sendSoupGone(nearestSoup);
							return;
						} else { // mine if there is still soup
							Direction directionToSoup = currentLocation.directionTo(nearestSoup);
							if (controller.canMineSoup(directionToSoup)) {
								controller.mineSoup(directionToSoup);
								return;
							}
						}
					} else { //if not adjacent, path to the soupLocation
						Pathfinding.execute(nearestSoup);
					}
				} else {
					Util.randomExplore();
				}
			}
		}
	}
	public boolean tryDeposit() throws GameActionException {
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			return false;
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		// Try to deposit soup
		for (Direction direction : Direction.values()) {
			if (controller.canDepositSoup(direction)) {
				MapLocation location = currentLocation.add(direction);
				if (controller.canSenseLocation(location)) {
					RobotInfo robot = controller.senseRobotAtLocation(location);
					if (robot != null && robot.getTeam() == controller.getTeam()) {
						controller.depositSoup(direction, controller.getSoupCarrying());
						return true;
					}
				}
			}
		}
		// Move towards HQ or refinery
		MapLocation bestLocation = null;
		int bestDistanceSquared = Integer.MAX_VALUE;

		boolean hqAvailable = false;

		for (Direction direction : Util.ADJACENT_DIRECTIONS) {
			MapLocation location = hqLocation.add(direction);
			if (controller.canSenseLocation(location)) {
				if (!controller.isLocationOccupied(location)) {
					hqAvailable = true;
				}
			} else {
				hqAvailable = true;
			}
		}
		if (hqAvailable) {
			if (Pathfinding.getTurnsSpentSoFar() < 70) {
				bestLocation = hqLocation;
				bestDistanceSquared = currentLocation.distanceSquaredTo(hqLocation);
			}
		}
		for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (robot.getType() == RobotType.REFINERY) {
				MapLocation location = robot.getLocation();
				int distanceSquared = currentLocation.distanceSquaredTo(location);
				if (distanceSquared < bestDistanceSquared) {
					bestDistanceSquared = distanceSquared;
					bestLocation = location;
				}
			}
		}
		if (bestLocation == null) {
			// Build a refinery
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation location = currentLocation.add(direction);
				if (location.isWithinDistanceSquared(hqLocation, 2)) {
					continue;
				}
				if (Util.canSafeBuildRobot(RobotType.REFINERY, direction)) {
					controller.buildRobot(RobotType.REFINERY, direction);
					return true;
				}
			}
			Util.randomExplore();
		} else {
			Pathfinding.execute(bestLocation);
		}
		return true;
	}
	public int closestDistanceSquaredToOurNetGun(MapLocation location) {
		int bestDistanceSquared = Integer.MAX_VALUE;
		for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (ally.getType() == RobotType.NET_GUN || ally.getType() == RobotType.HQ) {
				int distanceSquared = ally.getLocation().distanceSquaredTo(location);
				if (distanceSquared < bestDistanceSquared) {
					bestDistanceSquared = distanceSquared;
				}
			}
		}
		return bestDistanceSquared;
	}

	private boolean localSaveForNetGun = true;
	public boolean tryBuildNetGun() throws GameActionException {
		// Only build net gun if there is something to protect
		// Only if there is a friendly building nearby
		boolean seeFriendlyBuilding = false;
		boolean seeEnemyHQOrFulfillmentCenter = false;
		for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (ally.getType().isBuilding()) {
				if (ally.getType() == RobotType.NET_GUN) {
					if (SharedInfo.getVaporatorCount() < 8) {
						if (ally.getLocation().isWithinDistanceSquared(Cache.CURRENT_LOCATION, 5)) {
							return false;
						}
					} else {
						seeFriendlyBuilding = true;
					}
				} else {
					seeFriendlyBuilding = true;
				}
			}
		}
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (enemy.getType() == RobotType.HQ || enemy.getType() == RobotType.FULFILLMENT_CENTER) {
				seeEnemyHQOrFulfillmentCenter = true;
				break;
			}
		}
		if ((!seeEnemyHQOrFulfillmentCenter) && (!seeFriendlyBuilding)) {
			return false;
		}
		// Find closest enemy drone
		int bestDistanceSquared = Integer.MAX_VALUE;
		RobotInfo bestEnemy = null;
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (enemy.getType() == RobotType.DELIVERY_DRONE) {
				MapLocation enemyLocation = enemy.getLocation();
				int distanceSquared = enemyLocation.distanceSquaredTo(Cache.CURRENT_LOCATION);
				if (distanceSquared < bestDistanceSquared) {
					// Check if there is an ally net gun nearby
					if (closestDistanceSquaredToOurNetGun(enemyLocation) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
						continue;
					}
					bestDistanceSquared = distanceSquared;
					bestEnemy = enemy;
				}
			}
		}
		if (bestEnemy != null) {
			// We should build a net gun in range of the drone
			// do we have enough money
			boolean saveForNetGun = true;
			turn: {
				MapLocation enemyLocation = bestEnemy.getLocation();
				controller.setIndicatorLine(Cache.CURRENT_LOCATION, enemyLocation, 128, 0, 255);
				// Pathfind towards enemyLocation if it's not close enough
				if (!Cache.CURRENT_LOCATION.isWithinDistanceSquared(enemyLocation, 8)) {
					Pathfinding.execute(enemyLocation);
					break turn;
				}
				// Kite away
				if (Cache.CURRENT_LOCATION.isWithinDistanceSquared(enemyLocation, Util.ADJACENT_DISTANCE_SQUARED)) {
					// Move away
					Util.tryKiteAwayFrom(enemyLocation, true);
				}
				// do we have enough money
				if (controller.getTeamSoup() >= RobotType.NET_GUN.cost) {
					for (Direction direction : Util.getAttemptOrder(Cache.CURRENT_LOCATION.directionTo(enemyLocation))) {
						MapLocation location = Cache.CURRENT_LOCATION.add(direction);
						// Ensure the net gun location would be in range of drone
						if (!location.isWithinDistanceSquared(enemyLocation, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
							continue;
						}
						// Ensure it is not too close to our HQ
						if (location.isWithinDistanceSquared(hqLocation, Util.ADJACENT_DISTANCE_SQUARED)) {
							continue;
						}
						// Don't build in pit
						if (LatticeUtil.isPit(location)) {
							continue;
						}
						if (Util.canSafeBuildRobot(RobotType.NET_GUN, direction)) {
							controller.buildRobot(RobotType.NET_GUN, direction);
							saveForNetGun = false;
							break turn;
						}
					}
				}
				// Stay Still
			}
			if (saveForNetGun && !SharedInfo.isSavingForNetgun) {
				localSaveForNetGun = true;
				SharedInfo.sendSaveForNetgunSignal(true);
			}
			return true;
		}
		return false;
	}
	public static MapLocation findSoupLocation() throws GameActionException {
		int currentElevation = Cache.controller.senseElevation(Cache.CURRENT_LOCATION);
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			MapLocation location = Cache.CURRENT_LOCATION.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
			if (!Cache.controller.canSenseLocation(location)) {
				break;
			}
			if (Cache.controller.senseSoup(location) > 0) {
				int lowestElevationDifference = Integer.MAX_VALUE;
				for (Direction direction : Direction.values()) {
					MapLocation adjacent = location.add(direction);
					if (Cache.controller.canSenseLocation(adjacent)) {
						lowestElevationDifference =
								Math.min(lowestElevationDifference, Math.abs(Cache.controller.senseElevation(adjacent) - currentElevation));
					}
				}
				// elevationDifference / 3 < distance + 2 (buffer)
				if (lowestElevationDifference / 3.0 < Math.sqrt(Cache.CURRENT_LOCATION.distanceSquaredTo(location)) + 2) {
					return location;
				}
			}
		}
		return null;
	}
	public boolean willNotGetFloodedSoon(MapLocation location) throws GameActionException {
		if (controller.canSenseLocation(location)) {
			int turnsToFlooded = Util.getTurnsToFlooded(controller.senseElevation(location));
			return !((turnsToFlooded - controller.getRoundNum() < 200) && Util.isAdjacentToFlooding(location));
		}
		return false;
	}
	public boolean tryBuildAtBestLocation(RobotType type) throws GameActionException {
		MapLocation location = findBestBuildLocation(type);
		if (location == null) {
			Pathfinding.execute(hqLocation);
			return true;
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		controller.setIndicatorDot(location, 0, 0, 255);
		if (currentLocation.isAdjacentTo(location)) {
			// Build
			Direction direction = currentLocation.directionTo(location);
			if (controller.canBuildRobot(type, direction)) {
				controller.buildRobot(type, direction);
			}
		} else {
			Pathfinding.execute(location);
		}
		return true;
	}
	public MapLocation findBestBuildLocation(RobotType type) throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation bestLocation = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
		int currentElevation = Cache.controller.senseElevation(Cache.CURRENT_LOCATION);
		int targetElevation = LandscaperBot.getRealTargetElevation();
		// Do not consider the location where the unit currently is (starts at i = 1)
		mainLoop: for (int i = 1; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (!Cache.controller.canSenseLocation(location)) {
				break;
			}
			if (Cache.controller.senseFlooding(location)) {
				continue;
			}
			if (!LatticeUtil.isBuildLocation(location)) {
				continue;
			}
			int hqDistanceSquared = hqLocation.distanceSquaredTo(location);
			if (hqDistanceSquared <= Util.ADJACENT_DISTANCE_SQUARED) {
				continue;
			}
			// If it's a design school or fulfillment center, ensure that we are close enough to the hq
			if (type == RobotType.DESIGN_SCHOOL || type == RobotType.FULFILLMENT_CENTER) {
				if (hqDistanceSquared > RobotType.HQ.sensorRadiusSquared) {
					continue;
				}
			}
			int distanceSquared = (int) (Math.sqrt(hqDistanceSquared) * 3 + Math.sqrt(currentLocation.distanceSquaredTo(location)));
			int elevation = Cache.controller.senseElevation(location);
			if (Math.abs(currentElevation - elevation) > GameConstants.MAX_DIRT_DIFFERENCE) {
				continue;
			}
			if (elevation >= targetElevation) {
				distanceSquared -= 1000; // Artificially increase the score
			}
			if (distanceSquared < bestDistanceSquared/* && willNotGetFloodedSoon(location)*/) {
				// Ensure there are no surrounding enemy landscapers
				if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 8) {
					for (Direction direction : Util.ADJACENT_DIRECTIONS) {
						MapLocation adjacent = location.add(direction);
						if (controller.canSenseLocation(adjacent)) {
							RobotInfo robot = Cache.controller.senseRobotAtLocation(adjacent);
							if (robot != null && robot.getTeam() == Cache.OPPONENT_TEAM && robot.getType() == RobotType.LANDSCAPER) {
								continue mainLoop;
							}
						} else {
							continue mainLoop;
						}
					}
				} else {
					for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
						if (enemy.getType() == RobotType.LANDSCAPER && enemy.getLocation().isAdjacentTo(location)) {
							continue mainLoop;
						}
					}
				}
				// Check for elevation difference
				if (type != RobotType.DESIGN_SCHOOL || isValidDesignSchoolLocation(location, elevation)) {
					RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
					if (robot == null) {
						bestDistanceSquared = distanceSquared;
						bestLocation = location;
					}
					/*if (!UnitsMap.hasBlockingUnit(location)) {
						bestDistanceSquared = distanceSquared;
						bestLocation = location;
					}*/
				}
			}
		}
		return bestLocation;
	}
	public static boolean isValidDesignSchoolLocation(MapLocation buildingLocation, int elevation) throws GameActionException {
		if (!Cache.controller.canSenseLocation(buildingLocation)) {
			return false;
		}
		// Design schools can only spawn landscapers in cardinal directions
		// Because the ordinal directions will be lattice pits
		for (Direction direction : Util.CARDINAL_DIRECTIONS) {
			MapLocation location = buildingLocation.add(direction);
			if (!Cache.controller.canSenseLocation(location)) {
				return false;
			}
			// Checks whether there is a spawn point
			if (Math.abs(Cache.controller.senseElevation(location) - elevation) <= GameConstants.MAX_DIRT_DIFFERENCE) {
				return true;
			}
		}
		return false;
	}
	public RobotType getBuildTypeTarget() throws GameActionException {
		int teamSoup = controller.getTeamSoup();
		RobotType target = BuildOrder.getNextRobotType();
		if (target == RobotType.LANDSCAPER && SharedInfo.getDesignSchoolCount() <= 0) {
			// Build design school
			if (teamSoup >= BuildOrder.getSoupThreshold(RobotType.LANDSCAPER)) {
				return RobotType.DESIGN_SCHOOL;
			} else {
				return null;
			}
		}
		if (target == RobotType.DELIVERY_DRONE && SharedInfo.getFulfillmentCenterCount() <= 0) {
			// Build fulfillment center
			if (teamSoup >= BuildOrder.getSoupThreshold(RobotType.DELIVERY_DRONE)) {
				return RobotType.FULFILLMENT_CENTER;
			} else {
				return null;
			}
		}
		// try building vaporators
		if (teamSoup >= BuildOrder.getSoupThreshold(RobotType.VAPORATOR)) {
			return RobotType.VAPORATOR;
		} else {
			return null;
		}
	}
	public static boolean seeEnemyMinerOrLandscaper() {
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (enemy.getType() == RobotType.MINER || enemy.getType() == RobotType.LANDSCAPER) {
				return true;
			}
		}
		return false;
	}
	public boolean netGunIsClose() {
		for (int i = Cache.ALL_NEARBY_ENEMY_NET_GUNS_SIZE; --i >= 0;) {
			MapLocation location = Cache.ALL_NEARBY_ENEMY_NET_GUNS[i];
			if (Cache.CURRENT_LOCATION.isWithinDistanceSquared(location, RobotType.MINER.sensorRadiusSquared)) {
				return true;
			}
		}
		return false;
	}
}
