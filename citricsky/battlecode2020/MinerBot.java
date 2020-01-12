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

	private boolean attackerBuiltFulfillmentCenter = false;
	private boolean attackerBuiltDesignSchool = false;

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
		if (controller.getID() == SharedInfo.getAttackerMinerId()) {
			MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
			// TODO: Check if we have funds to build a drone
			if (!attackerBuiltFulfillmentCenter) {
				if (Util.trySafeBuildTowardsEnemyHQ(RobotType.FULFILLMENT_CENTER)) {
					attackerBuiltFulfillmentCenter = true;
				}
			}
			if (enemyHQ == null) {
				if (!attackerBuiltFulfillmentCenter) {
					Util.randomExploreBug0();
				}
			} else {
				// Check if we're at the enemyHQ
				if (!attackerBuiltDesignSchool) {
					for (Direction direction : Util.ADJACENT_DIRECTIONS) {
						// If we can build next to enemyHQ
						MapLocation location = currentLocation.add(direction);
						if (location.isWithinDistanceSquared(enemyHQ, 1)) {
							if (Util.canSafeBuildRobot(RobotType.DESIGN_SCHOOL, direction)) {
								controller.buildRobot(RobotType.DESIGN_SCHOOL, direction);
								attackerBuiltDesignSchool = true;
								return;
							}
						}
					}
					if (!currentLocation.isWithinDistanceSquared(enemyHQ, 2)) {
						Pathfinding.bug0(enemyHQ);
					}
				}
			}
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
				// try build design school
				if (!tryBuild(RobotType.DESIGN_SCHOOL)) {
					// Otherwise, walk randomly
					Util.randomExplore();
				}
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
			for (RobotInfo robot : Cache.ALL_FRIENDLY_ROBOTS) {
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
	private boolean spawned = false;
	public boolean tryBuild(RobotType type) throws GameActionException {
		// TODO: temporary hack to make sure landscapers spawn before more design schools
		if (spawned || controller.getTeamSoup() < 250) {
			return false;
		}
		Direction direction = Util.randomAdjacentDirection();
		MapLocation location = controller.getLocation().add(direction);
		if (hqLocation.isWithinDistanceSquared(location, 2)) {
			return false;
		}
		if ((location.x + location.y) % 2 == 0) {
			if (Util.canSafeBuildRobot(type, direction)) {
				controller.buildRobot(type, direction);
				spawned = true;
				return true;
			}
		}
		return false;
	}
}
