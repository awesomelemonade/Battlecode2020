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
		// Kite drones
		if (Util.tryKiting()) {
			return;
		}
		// See if we should build net guns
		if (tryBuildNetGun()) {
			return;
		}
		if (localSaveForNetGun) {
			localSaveForNetGun = false;
			SharedInfo.sendSaveForNetgunSignal(false);
		}
		// Check if we're on low elevation
		if (hqLocation.isWithinDistanceSquared(Cache.CURRENT_LOCATION, RobotType.HQ.sensorRadiusSquared)) {
			int elevation = controller.senseElevation(Cache.CURRENT_LOCATION);
			if (Util.getTurnsToFlooded(elevation) - controller.getRoundNum() < 100) {
				Pathfinding.execute(hqLocation);
				return;
			}
		}
		// See if we should be design school, fulfillment center, or vaporator
		RobotType buildTarget = getBuildTypeTarget();
		if (buildTarget != null) {
			if (tryBuildAtBestLocation(buildTarget)) {
				return;
			}
		}
		// Try mine soup
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			// Mine adjacent, broadcast location if new
			for (Direction direction : Util.ALL_DIRECTIONS) {
				if (controller.canMineSoup(direction)) {
					controller.mineSoup(direction);
					if(!SharedInfo.mapTracker.soupLocations.contains(currentLocation.add(direction))) {
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
				if (!SharedInfo.mapTracker.soupLocations.isEmpty()) {
					MapLocation nearestSoup = SharedInfo.mapTracker.soupLocations.nearestSoup(currentLocation);
					if (currentLocation.isAdjacentTo(nearestSoup)) {
						// if the soup is gone
						if(controller.senseSoup(nearestSoup) == 0) {
							SharedInfo.sendSoupGone(nearestSoup);
							return;
						} else { // mine if there is still soup
							Direction directionToSoup = currentLocation.directionTo(nearestSoup);
							if(controller.canMineSoup(directionToSoup)) {
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
			bestLocation = hqLocation;
			bestDistanceSquared  = currentLocation.distanceSquaredTo(hqLocation);
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
			if (ally.getType() == RobotType.NET_GUN) {
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
		for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (ally.getType().isBuilding()) {
				seeFriendlyBuilding = true;
			}
		}
		if (!seeFriendlyBuilding) {
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
					Util.tryKiteAwayFrom(enemyLocation);
				}
				// do we have enough money
				if (controller.getTeamSoup() >= RobotType.NET_GUN.cost) {
					for (Direction direction : Util.getAttemptOrder(Cache.CURRENT_LOCATION.directionTo(enemyLocation))) {
						MapLocation location = Cache.CURRENT_LOCATION.add(direction);
						// Ensure the net gun location would be in range of drone
						if (!location.isWithinDistanceSquared(enemyLocation, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
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
	public MapLocation findSoupLocation() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			MapLocation location = currentLocation.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
			if (!controller.canSenseLocation(location)) {
				break;
			}
			if (controller.senseSoup(location) > 0) {
				return location;
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
		int currentElevation = controller.senseElevation(Cache.CURRENT_LOCATION);
		int targetElevation = LandscaperBot.getRealTargetElevation();
		// Do not consider the location where the unit currently is (starts at i = 1)
		for (int i = 1; i < Util.FLOOD_FILL_DX.length; i++) {
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
			int distanceSquared = (int) (Math.sqrt(hqDistanceSquared) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
			int elevation = Cache.controller.senseElevation(location);
			if (Math.abs(currentElevation - elevation) > GameConstants.MAX_DIRT_DIFFERENCE) {
				continue;
			}
			if (elevation >= targetElevation) {
				distanceSquared -= 1000; // Artificially increase the score
			}
			if (distanceSquared < bestDistanceSquared/* && willNotGetFloodedSoon(location)*/) {
				// Check for elevation difference
				if (type != RobotType.DESIGN_SCHOOL || isValidDesignSchoolLocation(location, elevation)) {
					RobotInfo robot = controller.senseRobotAtLocation(location);
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
		int missingBuildingsCost = SharedInfo.getMissingBuildingsCost();
		/* TODO: only place net guns near enemy drones
		netGun: {
			if (teamSoup < RobotType.NET_GUN.cost) {
				break netGun;
			}
			// Check if there are drones
			for (RobotInfo robot : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
				if (robot.getType() == RobotType.DELIVERY_DRONE) {
					return RobotType.NET_GUN;
				}
			}
		}*/
		designSchool: {
			if (SharedInfo.getDesignSchoolCount() > 0 || teamSoup < RobotType.DESIGN_SCHOOL.cost) {
				break designSchool;
			}
			return RobotType.DESIGN_SCHOOL;
		}
		fulfillmentCenter: {
			if (SharedInfo.getOurHQState() == HQBot.NEEDS_HELP) {
				break fulfillmentCenter;
			}
			if (SharedInfo.getFulfillmentCenterCount() > 0 || teamSoup < missingBuildingsCost + 15) {
				break fulfillmentCenter;
			}
			if (netGunIsClose()) {
				break fulfillmentCenter;
			}
			// Check if we built 3 vaporators or there are enemies nearby
			if (SharedInfo.getVaporatorCount() >= 3 || seeEnemyMinerOrLandscaper()) {
				return RobotType.FULFILLMENT_CENTER;
			}
		}
		vaporator: {
			if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
				// Don't build vaporator when you see enemies
				break vaporator;
			}
			if (SharedInfo.getVaporatorCount() > 120) {
				break vaporator;
			}
			if (SharedInfo.getVaporatorCount() > 3) {
				if (teamSoup < RobotType.VAPORATOR.cost + missingBuildingsCost) {
					break vaporator;
				}
			} else {
				if (teamSoup < RobotType.VAPORATOR.cost) {
					break vaporator;
				}
			}
			return RobotType.VAPORATOR;
		}
		return null;
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
