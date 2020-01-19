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

		// See if we should build design school
		if (tryBuildVaporator()) {
			return;
		}
		if (tryBuildDesignSchool()) {
			return;
		}
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			// Try to mine soup
			for (Direction direction : Direction.values()) {
				if (controller.canMineSoup(direction)) {
					controller.mineSoup(direction);
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
		} else {
			// Try to deposit soup
			for (Direction direction : Direction.values()) {
				if (controller.canDepositSoup(direction)) {
					MapLocation location = controller.getLocation().add(direction);
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
	public boolean willNotGetFloodedSoon(MapLocation location) throws GameActionException {
		if (controller.canSenseLocation(location)) {
			int turnsToFlooded = Util.getTurnsToFlooded(controller.senseElevation(location));
			return !((turnsToFlooded - controller.getRoundNum() < 200) && Util.isAdjacentToFlooding(location));
		}
		return false;
	}
	public boolean tryBuildVaporator() throws GameActionException {
		if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
			// Don't build vaporator when you see enemies
			return false;
		}
		int teamSoup = controller.getTeamSoup();
		if (teamSoup < RobotType.VAPORATOR.cost || teamSoup > RobotType.VAPORATOR.cost * 2) {
			return false;
		}
		Direction idealDirection = controller.getLocation().directionTo(SharedInfo.getOurHQLocation());
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = controller.getLocation().add(direction);
			if (hqLocation.isWithinDistanceSquared(location, 2)) {
				continue;
			}
			if (LatticeUtil.isBuildLocation(location) && willNotGetFloodedSoon(location)) {
				if (Util.canSafeBuildRobot(RobotType.VAPORATOR, direction)) {
					controller.buildRobot(RobotType.VAPORATOR, direction);
					return true;
				}
			}
		}
		return false;
	}
	public boolean isValidDesignSchoolLocation(MapLocation robotLocation) throws GameActionException {
		if (!controller.canSenseLocation(robotLocation)) {
			return false;
		}
		int elevation = controller.senseElevation(robotLocation);
		// Design schools can only spawn landscapers in cardinal directions
		// Because the ordinal directions will be lattice pits
		for (Direction direction : Util.CARDINAL_DIRECTIONS) {
			MapLocation location = robotLocation.add(direction);
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
	public boolean tryBuildDesignSchool() throws GameActionException {
		boolean seeHQ = false;
		for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (robot.getType() == RobotType.DESIGN_SCHOOL &&
					isValidDesignSchoolLocation(robot.getLocation())) {
				return false;
			}
			if (robot.getType() == RobotType.HQ) {
				seeHQ = true;
			}
		}
		// If we see enemies near our hq, we should build one asap to defend
		if (!(seeHQ && Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0)) {
			// If we have too much soup, we might as well create em
			if (controller.getTeamSoup() < 2 * RobotType.VAPORATOR.cost) {
				// If we have little soup, don't spawn unless we haven't spawned one yet
				if (spawnedDesignSchool) {
					return false;
				}
			}
		}
		Direction idealDirection = controller.getLocation().directionTo(SharedInfo.getOurHQLocation());
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = controller.getLocation().add(direction);
			if (hqLocation.isWithinDistanceSquared(location, 2)) {
				continue;
			}
			if (LatticeUtil.isBuildLocation(location) && willNotGetFloodedSoon(location) &&
					isValidDesignSchoolLocation(location)) {
				if (spawnedDesignSchool) {
					if (Math.random() < 0.2) {
						if (Util.canSafeBuildRobot(RobotType.FULFILLMENT_CENTER, direction)) {
							controller.buildRobot(RobotType.FULFILLMENT_CENTER, direction);
							return true;
						}
					}
				}
				if (Util.canSafeBuildRobot(RobotType.DESIGN_SCHOOL, direction)) {
					controller.buildRobot(RobotType.DESIGN_SCHOOL, direction);
					spawnedDesignSchool = true;
					return true;
				}
			}
		}
		return false;
	}
}
