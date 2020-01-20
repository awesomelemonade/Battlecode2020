package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	public static final int LANDSCAPING_THRESHOLD = 200; // If the dirt difference is this high, we won't terraform
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
				this::tryBuryAdjacentEnemyBuildings,
				this::tryDefend,
				() -> tryHeal(SharedInfo.getOurHQLocation()),
				() -> tryHeal(getNearestBuilding(Cache.ALL_NEARBY_FRIENDLY_ROBOTS)),
				this::tryWall,
				this::tryTerraform,
				() -> tryGoToAttack(getNearestBuilding(Cache.ALL_NEARBY_ENEMY_ROBOTS)),
				() -> tryGoToAttack(SharedInfo.getEnemyHQLocation()),
				Util::randomExplore
		};
	}
	private static int targetElevation = 1;
	@Override
	public void turn() throws GameActionException {
		while (Util.getTurnsToFlooded(targetElevation) - controller.getRoundNum() < 100) {
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
				int upperTargetElevation = getRealTargetElevation() + GameConstants.MAX_DIRT_DIFFERENCE;
				int elevation = controller.senseElevation(currentLocation);
				// Try deposit up to upperTargetElevation
				if (elevation < upperTargetElevation) {
					if (controller.canDepositDirt(Direction.CENTER)) {
						controller.depositDirt(Direction.CENTER);
					} else {
						tryDigFromPit();
					}
				} else {
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
		// Check if we need to wall
		if (SharedInfo.wallState == SharedInfo.WALL_STATE_NONE) {
			return false;
		}
		if (hqLocation.isAdjacentTo(Cache.CURRENT_LOCATION)) {
			if (controller.canDepositDirt(Direction.CENTER)) {
				controller.depositDirt(Direction.CENTER);
			} else {
				tryDigFromPit();
			}
			return true;
		} else {
			if (SharedInfo.wallState == SharedInfo.WALL_STATE_NEEDS) {
				Pathfinding.execute(hqLocation);
			}
		}
		return false;
	}
	public boolean tryHeal(MapLocation location) throws GameActionException {
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
		return (targetElevation - targetElevation % 3) + 5;
	}
	public boolean tryTerraform() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation ourHQLocation = SharedInfo.getOurHQLocation();
		if (ourHQLocation == null) {
			return false;
		}
		int targetElevation = getRealTargetElevation();
		int upperTargetElevation = targetElevation + GameConstants.MAX_DIRT_DIFFERENCE;
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
			if (!controller.canSenseLocation(location)) {
				break;
			}
			if (!LatticeUtil.isPit(location)) {
				int elevation = controller.senseElevation(location);
				// Fill up to target elevation or dig to targetElevation + 3
				if (elevation < targetElevation) {
					if (targetElevation - elevation <= LANDSCAPING_THRESHOLD) {
						// Try to raise elevation
						int distanceSquared = (int) (Math.sqrt(ourHQLocation.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
							RobotInfo robot = controller.senseRobotAtLocation(location);
							if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
								// TODO: Maybe we should consider burying our own buildings to make space for new buildings
								continue;
							}
							bestDistanceSquared = distanceSquared;
							bestLocation = location;
							bestRaise = true;
						}
					}
				} else if (elevation > upperTargetElevation) {
					// Check if we're next to hq and we don't want to do that anymore
					if (ourHQLocation.isAdjacentTo(location) && SharedInfo.wallState != SharedInfo.WALL_STATE_NONE) {
						continue;
					}
					if (elevation - upperTargetElevation <= LANDSCAPING_THRESHOLD) {
						// Try to lower elevation
						int distanceSquared = (int) (Math.sqrt(ourHQLocation.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
							RobotInfo robot = controller.senseRobotAtLocation(location);
							if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
								// TODO: Maybe we should consider burying our own buildings to make space for new buildings
								continue;
							}
							bestDistanceSquared = distanceSquared;
							bestLocation = location;
							bestRaise = false;
						}
					}
				}
			}
		}
		if (bestLocation != null) {
			controller.setIndicatorDot(bestLocation, 0, 255, 255);
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
	public static void tryDigFromPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(Cache.CURRENT_LOCATION)) {
			MapLocation location = Cache.CURRENT_LOCATION.add(pitDirection);
			if (Cache.controller.canSenseLocation(location)) {
				RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
				if (robot != null && robot.getType().isBuilding() && robot.getTeam() == Cache.OPPONENT_TEAM) {
					continue;
				}
			} else {
				continue;
			}
			if (Cache.controller.canDigDirt(pitDirection)) {
				Cache.controller.digDirt(pitDirection);
				break;
			}
		}
	}
	public static void tryDepositToPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(Cache.CURRENT_LOCATION)) {
			MapLocation location = Cache.CURRENT_LOCATION.add(pitDirection);
			if (Cache.controller.canSenseLocation(location)) {
				RobotInfo robot = Cache.controller.senseRobotAtLocation(location);
				if (robot != null && robot.getType().isBuilding() && robot.getTeam() == Cache.OUR_TEAM) {
					continue;
				}
			} else {
				continue;
			}
			if (Cache.controller.canDepositDirt(pitDirection)) {
				Cache.controller.depositDirt(pitDirection);
				break;
			}
		}
	}
}
