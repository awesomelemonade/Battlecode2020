package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class MinerBot implements RunnableBot {
	private RobotController controller;
	public MinerBot(RobotController controller) {
		this.controller = controller;
	}
	private MapLocation hqLocation;

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
		MapLocation currentLocation = controller.getLocation();

		// See if we should build anything
		RobotType buildTarget = getBuildTypeTarget();
		if (buildTarget != null) {
			if (tryBuildAtBestLocation(buildTarget)) {
				return;
			}
		}
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			System.out.println("not enough soup :(");
			//try to mine from the soupLocations
			if(!SharedInfo.soupLocations.isEmpty()){
				MapLocation soupLocation = SharedInfo.soupLocations.peek();
				if(currentLocation.isAdjacentTo(soupLocation)) {
					//if the soup is gone
					if(controller.senseSoup(soupLocation) == 0) {
						SharedInfo.sendSoupGone(soupLocation);
					}
					//mine if there is still soup
					else {
						Direction directionToSoup = currentLocation.directionTo(soupLocation);
						if(controller.canMineSoup(directionToSoup)) {
							controller.mineSoup(directionToSoup);
						}
					}
				}
				//if not adjacent, path to the soupLocation
				else {
					Pathfinding.execute(soupLocation);
				}
			}
			else {
				//mine adjacent, broadcast location if new
				for (Direction direction : Direction.values()) {
					if (controller.canMineSoup(direction)) {
						controller.mineSoup(direction);
						if(!SharedInfo.soupLocations.contains(currentLocation.add(direction))) {
							SharedInfo.sendSoup(currentLocation.add(direction));
						}
						return;
					}
				}
				
				// Move towards visible soup
				MapLocation soupLocation = findSoupLocation();
				if (soupLocation == null) {
					// Otherwise, walk randomly
					Util.randomExplore();
				} else {
					Pathfinding.execute(soupLocation);
				}
			}
		} else {
			// Try to deposit soup
			for (Direction direction : Direction.values()) {
				if (controller.canDepositSoup(direction)) {
					MapLocation location = currentLocation.add(direction);
					if (controller.canSenseLocation(location)) {
						RobotInfo robot = controller.senseRobotAtLocation(location);
						if (robot != null && robot.getTeam() == controller.getTeam()) {
							controller.depositSoup(direction, controller.getSoupCarrying());
							return;
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
						return;
					}
				}
				Util.randomExplore();
			} else {
				Pathfinding.execute(bestLocation);
			}
		}
	}
	public MapLocation findSoupLocation() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
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
	private boolean spawnedDesignSchool = false;
	public void onBuildRobot(RobotType type) {
		if (type == RobotType.DESIGN_SCHOOL) {
			spawnedDesignSchool = true;
		}
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
			return false;
		}
		MapLocation currentLocation = controller.getLocation();
		if (currentLocation.isAdjacentTo(location)) {
			// Build
			Direction direction = currentLocation.directionTo(location);
			controller.setIndicatorDot(location, 0, 0, 255);
			if (controller.canBuildRobot(type, direction)) {
				controller.buildRobot(type, direction);
				onBuildRobot(type);
			}
		} else {
			Pathfinding.execute(location);
		}
		return true;
	}
	public MapLocation findBestBuildLocation(RobotType type) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		MapLocation bestLocation = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
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
			int distanceSquared = hqLocation.distanceSquaredTo(location);
			if (distanceSquared < bestDistanceSquared && distanceSquared > Util.ADJACENT_DISTANCE_SQUARED &&
					LatticeUtil.isBuildLocation(location) && willNotGetFloodedSoon(location)) {
				// Check for elevation difference
				if (type != RobotType.DESIGN_SCHOOL || isValidDesignSchoolLocation(location)) {
					RobotInfo robot = controller.senseRobotAtLocation(location);
					if (robot == null) {
						bestDistanceSquared = distanceSquared;
						bestLocation = location;
					}
				}
			}
		}
		return bestLocation;
	}
	public boolean isValidDesignSchoolLocation(MapLocation buildingLocation) throws GameActionException {
		if (!controller.canSenseLocation(buildingLocation)) {
			return false;
		}
		int elevation = controller.senseElevation(buildingLocation);
		// Design schools can only spawn landscapers in cardinal directions
		// Because the ordinal directions will be lattice pits
		for (Direction direction : Util.CARDINAL_DIRECTIONS) {
			MapLocation location = buildingLocation.add(direction);
			if (!controller.canSenseLocation(location)) {
				return false;
			}
			// Checks whether there is a spawn point
			if (Math.abs(controller.senseElevation(location) - elevation) <= GameConstants.MAX_DIRT_DIFFERENCE) {
				return true;
			}
		}
		return false;
	}
	public RobotType getBuildTypeTarget() throws GameActionException {
		int teamSoup = controller.getTeamSoup();
		vaporator: {
			if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
				// Don't build vaporator when you see enemies
				break vaporator;
			}
			if (teamSoup < RobotType.VAPORATOR.cost || teamSoup > RobotType.VAPORATOR.cost * 2) {
				break vaporator;
			}
			return RobotType.VAPORATOR;
		}
		designSchool: {
			if (teamSoup < RobotType.DESIGN_SCHOOL.cost) {
				break designSchool;
			}
			boolean seeHQ = false;
			for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
				if (robot.getType() == RobotType.DESIGN_SCHOOL &&
						isValidDesignSchoolLocation(robot.getLocation())) {
					break designSchool;
				}
				if (robot.getType() == RobotType.HQ) {
					seeHQ = true;
				}
			}
			// If we see enemies near our hq, we should build one asap to defend
			if (!(seeHQ && Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0)) {
				// If we have too much soup, we might as well create em
				if (teamSoup < 2 * RobotType.VAPORATOR.cost) {
					// If we have little soup, don't spawn unless we haven't spawned one yet
					if (spawnedDesignSchool) {
						break designSchool;
					}
				}
			}
			if (spawnedDesignSchool && Math.random() < 0.2) {
				return RobotType.FULFILLMENT_CENTER;
			} else {
				return RobotType.DESIGN_SCHOOL;
			}
		}
		return null;
	}
}
