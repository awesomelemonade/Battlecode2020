package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	// If the dirt difference is this high, we won't terraform
	public static final int LANDSCAPING_THRESHOLD = 120;
	private RobotController controller;
	private RobotBehavior[] behaviors;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		// Order of behaviors to be executed
		behaviors = new RobotBehavior[] {
				this::tryEmergencyHealHQ,
				Util::tryKiteFromAdjacentDrones,
				this::tryBuryAdjacentEnemyBuildings,
				this::tryDefend,
				() -> tryHealAdjacent(SharedInfo.getOurHQLocation()),
				() -> tryHealAdjacent(getNearestBuilding(Cache.ALL_NEARBY_FRIENDLY_ROBOTS)),
				this::tryFillUpAdjacentHQ,
				this::tryWall,
				this::tryTerraform,
				() -> tryGoToAttack(getNearestBuilding(Cache.ALL_NEARBY_ENEMY_ROBOTS)),
				() -> tryGoToAttack(SharedInfo.getEnemyHQLocation()),
				Util::randomExplore
		};
	}
	public boolean tryFillUpAdjacentHQ() throws GameActionException {
		if (SharedInfo.getOurHQLocation() == null) {
			return false;
		}
		int targetElevation = getRealTargetElevation();
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		for (Direction direction : Direction.values()) {
			MapLocation location = Cache.CURRENT_LOCATION.add(direction);
			if (Util.onTheMap(location) && Cache.controller.canSenseLocation(location)) {
				if (location.isAdjacentTo(ourHQ) && (!location.equals(ourHQ))) {
					int elevation = Cache.controller.senseElevation(location);
					if (!LatticeUtil.isPit(location)) {
						if (elevation < targetElevation) {
							if (controller.canDepositDirt(direction)) {
								controller.depositDirt(direction);
								controller.setIndicatorLine(Cache.CURRENT_LOCATION, location, 128, 128, 128);
							} else {
								tryDigFromPit();
							}
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	private static int targetElevation = 1;
	@Override
	public void turn() throws GameActionException {
		while (Util.getTurnsToFlooded(targetElevation) - controller.getRoundNum() < 100) {
			if (targetElevation >= Util.TURNS_TO_FLOODED.length) {
				targetElevation = Integer.MAX_VALUE / 2;
				break;
			}
			targetElevation++;
		}
		if (!controller.isReady()) {
			return;
		}
		for (int i = 0; i < behaviors.length; i++) {
			if (behaviors[i].execute()) {
				// Executed successfully
				return;
			}
		}
	}
	public boolean tryEmergencyHealHQ() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation ourHQLocation = SharedInfo.getOurHQLocation();
		if (ourHQLocation == null) {
			return false;
		}
		if (currentLocation.isAdjacentTo(ourHQLocation)) {
			if (controller.canSenseLocation(ourHQLocation)) {
				int hqHealth = RobotType.HQ.dirtLimit - controller.senseRobotAtLocation(ourHQLocation).dirtCarrying;
				if (hqHealth <= 10) {
					Direction direction = currentLocation.directionTo(ourHQLocation);
					if (controller.canDigDirt(direction)) {
						controller.digDirt(direction);
					} else {
						tryDepositToPit();
					}
					return true;
				}
			}
		}
		return false;
	}
	public boolean tryBuryAdjacentEnemyBuildings() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		Direction bestDirection = null;
		int bestBuryPriority = 0;
		if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation location = currentLocation.add(direction);
				if (Util.onTheMap(location)) {
					RobotInfo robot = controller.senseRobotAtLocation(location);
					if (robot != null && robot.getTeam() == Cache.OPPONENT_TEAM && robot.getType().isBuilding()) {
						int buryPriority = getBuryPriority(robot.getType());
						if (buryPriority > bestBuryPriority) {
							bestBuryPriority = buryPriority;
							bestDirection = direction;
						}
					}
				}
			}
		}
		if (bestDirection != null) {
			if (controller.canDepositDirt(bestDirection)) {
				controller.depositDirt(bestDirection);
			} else {
				tryDigFromPit();
				controller.setIndicatorDot(currentLocation, 255, 255, 0);
			}
			controller.setIndicatorLine(currentLocation, currentLocation.add(bestDirection), 255, 128, 0);
			return true;
		}
		return false;
	}
	public int getBuryPriority(RobotType type) {
		switch (type) {
			case FULFILLMENT_CENTER:
				return 6;
			case NET_GUN:
				return 5;
			case DESIGN_SCHOOL:
				return 4;
			case VAPORATOR:
				return 3;
			case REFINERY:
				return 2;
			case HQ:
				return 1;
			default:
				return 0;
		}
	}
	public boolean tryDefend() throws GameActionException {
		int ourHQState = SharedInfo.getOurHQState();
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation ourHQLocation = SharedInfo.getOurHQLocation();
		if (ourHQLocation == null) {
			return false;
		}
		if (ourHQState == HQBot.NO_HELP_NEEDED) {
			return false;
		}
		if (currentLocation.isAdjacentTo(ourHQLocation)) {
			// Try dig towards HQ
			Direction direction = currentLocation.directionTo(ourHQLocation);
			if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
			} else {
				if (!tryWall()) {
					tryDepositToPit();
				}
			}
			return true;
		} else {
			if (ourHQState == HQBot.NO_ADDITIONAL_HELP_NEEDED) {
				// Bury nearest building if it is near HQ - TODO - should we really do this? maybe only fulfillment centers and net guns?
				RobotInfo bestEnemy = null;
				int bestDistanceSquared = Integer.MAX_VALUE;
				for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
					MapLocation location = enemy.getLocation();
					if (location.isWithinDistanceSquared(ourHQLocation, RobotType.HQ.sensorRadiusSquared)) {
						if (enemy.getType() == RobotType.FULFILLMENT_CENTER || enemy.getType() == RobotType.NET_GUN) {
							int distanceSquared = currentLocation.distanceSquaredTo(location);
							if (distanceSquared < bestDistanceSquared) {
								bestDistanceSquared = distanceSquared;
								bestEnemy = enemy;
							}
						}
					}
				}
				if (bestEnemy != null) {
					return tryGoToAttack(bestEnemy.getLocation());
				}
			} else { // if (ourHQState == HQBot.NEEDS_HELP)
				// Pathfind towards our hq
				Pathfinding.execute(ourHQLocation);
				return true;
			}
		}
		return false;
	}
	public boolean tryWall() throws GameActionException {
		MapLocation hqLocation = SharedInfo.getOurHQLocation();
		if (hqLocation == null) {
			return false;
		}
		switch (SharedInfo.wallState) {
			case SharedInfo.WALL_STATE_NONE:
				return false;
			case SharedInfo.WALL_STATE_NEEDS:
				if (hqLocation.isAdjacentTo(Cache.CURRENT_LOCATION)) {
					// Shuffle around
					Direction direction = Util.randomAdjacentDirection();
					MapLocation location = Cache.CURRENT_LOCATION.add(direction);
					if (location.isAdjacentTo(hqLocation) && Util.canSafeMove(direction) && (!Util.isInCorner(location))) {
						controller.move(direction);
					}
				} else {
					Pathfinding.execute(hqLocation);
				}
				return true;
			case SharedInfo.WALL_STATE_STAYS:
				if (hqLocation.isAdjacentTo(Cache.CURRENT_LOCATION)) {
					Direction lowestDirection = null;
					int lowestElevation = Integer.MAX_VALUE;
					for (Direction direction : Util.ALL_DIRECTIONS) {
						MapLocation location = Cache.CURRENT_LOCATION.add(direction);
						if (controller.canSenseLocation(location) && location.isAdjacentTo(hqLocation) &&
								(!location.equals(hqLocation)) && (!Util.isInCorner(location))) {
							int elevation = controller.senseElevation(location);
							if (Util.canPotentiallyBeFlooded(elevation)) {
								if (controller.canDepositDirt(direction)) {
									controller.depositDirt(direction);
									return true;
								}
							}
							if (elevation < lowestElevation) {
								lowestDirection = direction;
								lowestElevation = elevation;
							}
						}
					}
					if (!tryDigFromPit()) {
						if (lowestDirection != null) {
							if (controller.canDepositDirt(lowestDirection)) {
								controller.depositDirt(lowestDirection);
							}
						}
					}
					return true;
				}
				break;
		}
		return false;
	}
	public boolean tryHealAdjacent(MapLocation location) throws GameActionException {
		// TODO: Consider depositing dirt if full?
		if (location != null) {
			MapLocation currentLocation = Cache.CURRENT_LOCATION;
			if (currentLocation.isAdjacentTo(location)) {
				Direction direction = currentLocation.directionTo(location);
				if (controller.canDigDirt(direction)) {
					controller.digDirt(direction);
					return true;
				}
			}
		}
		return false;
	}
	public boolean tryGoToAttack(MapLocation location) throws GameActionException {
		if (location != null) {
			if (Cache.CURRENT_LOCATION.isAdjacentTo(location)) {
				if (controller.getDirtCarrying() > 0) {
					Direction direction = Cache.CURRENT_LOCATION.directionTo(location);
					if (controller.canDepositDirt(direction)) {
						controller.depositDirt(direction);
					}
				} else {
					if (controller.canDigDirt(Direction.CENTER)) {
						controller.digDirt(Direction.CENTER);
					}
				}
			} else {
				Pathfinding.execute(location);
			}
			return true;
		}
		return false;
	}
	public MapLocation getNearestBuilding(RobotInfo[] robots) {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		int bestDistance = Integer.MAX_VALUE;
		MapLocation bestEnemy = null;
		for (RobotInfo enemy : robots) {
			// TODO: Square distance instead of r^2 distance?
			if (enemy.getType().isBuilding()) {
				int distance = enemy.getLocation().distanceSquaredTo(currentLocation);
				if (distance < bestDistance) {
					bestDistance = distance;
					bestEnemy = enemy.getLocation();
				}
			}
		}
		return bestEnemy;
	}
	public static int getRealTargetElevation() {
		return Math.max((targetElevation - targetElevation % 3) + 5, SharedInfo.getOurHQElevation());
	}
	public static boolean testStep(MapLocation location, int elevation, int targetStepElevation) throws GameActionException {
		if (!Cache.controller.senseFlooding(location)) {
			boolean flag = false;
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation adjacent = location.add(direction);
				if (!Util.onTheMap(adjacent)) {
					continue;
				}
				if (!Cache.controller.canSenseLocation(adjacent)) {
					return false;
				}
				if (Cache.controller.senseFlooding(adjacent)) {
					continue;
				}
				if (LatticeUtil.isPit(adjacent)) {
					continue;
				}
				if (Cache.controller.senseRobotAtLocation(adjacent) != null) {
					continue;
				}
				int adjacentElevation = Cache.controller.senseElevation(adjacent);
				if (Math.abs(adjacentElevation - targetElevation) > LANDSCAPING_THRESHOLD) {
					continue;
				}
				if (adjacentElevation < targetStepElevation) {
					flag = true;
					break;
				}
			}
			if (flag && elevation >= targetStepElevation) {
				Cache.controller.setIndicatorDot(location, 0, 0, 0);
				return false;
			}
			Cache.controller.setIndicatorDot(location, 255, 255, 255);
		}
		return true;
	}
	public boolean tryTerraform() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation ourHQLocation = SharedInfo.getOurHQLocation();
		if (ourHQLocation == null) {
			return false;
		}
		int targetElevation = getRealTargetElevation();
		int upperTargetElevation = targetElevation + GameConstants.MAX_DIRT_DIFFERENCE;
		int targetStepElevation = targetElevation - GameConstants.MAX_DIRT_DIFFERENCE;
		boolean creatingWall = SharedInfo.wallState != SharedInfo.WALL_STATE_NONE;
		MapLocation bestLocation = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
		boolean bestRaise = false;
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (!Cache.controller.canSenseLocation(location)) {
				break;
			}
			if (!LatticeUtil.isPit(location)) {
				int elevation = Cache.controller.senseElevation(location);
				// Fill up to target elevation or dig to targetElevation + 3
				if (elevation < targetElevation) {
					if (targetElevation - elevation <= LANDSCAPING_THRESHOLD) {
						// Try to raise elevation
						int distanceSquared = (int) (Math.sqrt(ourHQLocation.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
							RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
							if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
								continue;
							}
							if (targetElevation <= 8 && (!testStep(location, elevation, targetStepElevation))) {
								continue;
							}
							bestDistanceSquared = distanceSquared;
							bestLocation = location;
							bestRaise = true;
						}
					}
				} else if (elevation > upperTargetElevation) {
					// Check if we're next to hq and we don't want to do that anymore
					if (creatingWall && ourHQLocation.isAdjacentTo(location)) {
						continue;
					}
					if (elevation - upperTargetElevation <= LANDSCAPING_THRESHOLD) {
						// Try to lower elevation
						int distanceSquared = (int) (Math.sqrt(ourHQLocation.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
							RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
							if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
								continue;
							}
							bestDistanceSquared = distanceSquared;
							bestLocation = location;
							bestRaise = false;
						}
					}
				}
			}
			// TODO: Bytecode control
			if (bestLocation != null && Clock.getBytecodesLeft() < 1000) {
				break;
			}
		}
		if (bestLocation != null) {
			Cache.controller.setIndicatorDot(bestLocation, 0, 255, 255);
			if (bestRaise) {
				tryToRaise(bestLocation);
			} else {
				tryToLower(bestLocation);
			}
			return true;
		}
		return false;
	}
	public void tryToRaise(MapLocation location) throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (currentLocation.isWithinDistanceSquared(location, 2)) {
			Direction direction = currentLocation.directionTo(location);
			if (controller.canDepositDirt(direction)) {
				controller.depositDirt(direction);
			} else {
				tryDigFromPit();
			}
		} else {
			Pathfinding.execute(location);
		}
	}
	public void tryToLower(MapLocation location) throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (currentLocation.isWithinDistanceSquared(location, 2)) {
			Direction direction = currentLocation.directionTo(location);
			if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
			} else {
				tryDepositToPit();
			}
		} else {
			Pathfinding.execute(location);
		}
	}
	public static boolean tryDigFromPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(Cache.CURRENT_LOCATION)) {
			MapLocation location = Cache.CURRENT_LOCATION.add(pitDirection);
			if (Cache.controller.canSenseLocation(location)) {
				RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
				if (robot != null) {
					// Don't trap our own units or help the enemy team
					if (robot.getTeam() == Cache.OUR_TEAM || robot.getType().isBuilding()) {
						continue;
					}
				}
				if (Cache.controller.canDigDirt(pitDirection)) {
					Cache.controller.digDirt(pitDirection);
					return true;
				}
			}
		}
		// Dig from very high elevations (but not around our hq)
		int threshold = Math.min(getRealTargetElevation(), 100);
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		if (ourHQ != null) {
			for (Direction direction : Direction.values()) {
				MapLocation location = Cache.CURRENT_LOCATION.add(direction);
				if (Cache.controller.canSenseLocation(location)) {
					// Don't destroy our own wall
					if (ourHQ.isAdjacentTo(location)) {
						continue;
					}
					RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
					if (robot != null && robot.getTeam() == Cache.OPPONENT_TEAM && robot.getType().isBuilding()) {
						// Don't help enemy team
						continue;
					}
					int elevation = Cache.controller.senseElevation(location);
					if (elevation > threshold) {
						if (Cache.controller.canDigDirt(direction)) {
							Cache.controller.digDirt(direction);
							return true;
						}
					}
				}
			}
		}
		// Dig from corner
		if (SharedInfo.ourHQNearCorner) {
			if (Util.isNearCorner(Cache.CURRENT_LOCATION)) {
				Direction direction = Util.directionToNearestCorner(Cache.CURRENT_LOCATION);
				if (Cache.controller.canDigDirt(direction)) {
					Cache.controller.digDirt(direction);
					return true;
				}
			}
		}
		return false;
	}
	public static boolean tryDepositToPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(Cache.CURRENT_LOCATION)) {
			MapLocation location = Cache.CURRENT_LOCATION.add(pitDirection);
			if (Cache.controller.canSenseLocation(location)) {
				RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
				if (robot != null && robot.getType().isBuilding() && robot.getTeam() == Cache.OUR_TEAM) {
					continue;
				}
				if (Cache.controller.canDepositDirt(pitDirection)) {
					Cache.controller.depositDirt(pitDirection);
					return true;
				}
			}
		}
		return false;
	}
}
