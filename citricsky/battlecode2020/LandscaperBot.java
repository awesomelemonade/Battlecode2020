package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private static final int LANDSCAPING_THRESHOLD = 200; // If the dirt difference is this high, we won't terraform
	private RobotController controller;
	//private InfoMap infoMap;
	private RobotBehavior[] behaviors;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		//infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
		// Order of behaviors to be executed
		behaviors = new RobotBehavior[] {
				this::tryEmergencyHealHQ,
				this::tryBuryAdjacentEnemyBuildings,
				this::tryDefend,
				() -> tryHeal(SharedInfo.getOurHQLocation()),
				() -> tryHeal(getNearestBuilding(Cache.ALL_NEARBY_FRIENDLY_ROBOTS)),
				this::tryTerraform,
				() -> tryGoToAttack(getNearestBuilding(Cache.ALL_NEARBY_ENEMY_ROBOTS)),
				() -> tryGoToAttack(SharedInfo.getEnemyHQLocation()),
				Util::randomExplore
		};
	}
	private int targetElevation = 1;
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
		MapLocation currentLocation = controller.getLocation();
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
		MapLocation currentLocation = controller.getLocation();
		if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation location = currentLocation.add(direction);
				if (Util.onTheMap(location)) {
					RobotInfo robot = controller.senseRobotAtLocation(location);
					if (robot != null && robot.getTeam() == Cache.OPPONENT_TEAM && robot.getType().isBuilding()) {
						if (controller.canDepositDirt(direction)) {
							controller.depositDirt(direction);
						}
						tryDigFromPit();
						return true;
					}
				}
			}
		}
		return false;
	}
	public boolean tryDefend() throws GameActionException {
		int ourHQState = SharedInfo.getOurHQState();
		MapLocation currentLocation = controller.getLocation();
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
				// Bury nearest building if it is near HQ
				RobotInfo bestEnemy = null;
				int bestDistanceSquared = Integer.MAX_VALUE;
				for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
					MapLocation location = enemy.getLocation();
					if (location.isWithinDistanceSquared(ourHQLocation, RobotType.HQ.sensorRadiusSquared)) {
						if (enemy.getType().isBuilding()) {
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
	public boolean tryHeal(MapLocation location) throws GameActionException {
		// TODO: Consider depositing dirt if full?
		if (location != null) {
			MapLocation currentLocation = controller.getLocation();
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
			if (controller.getLocation().isAdjacentTo(location)) {
				if (controller.getDirtCarrying() > 0) {
					Direction direction = controller.getLocation().directionTo(location);
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
		MapLocation currentLocation = controller.getLocation();
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
	public int getRealTargetElevation() {
		return (this.targetElevation - this.targetElevation % 3) + 5;
	}
	public boolean tryTerraform() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		MapLocation ourLocationHQ = SharedInfo.getOurHQLocation();
		if (ourLocationHQ == null) {
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
			RobotInfo robot = controller.senseRobotAtLocation(location);
			if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
				// TODO: Maybe we should consider burying our own buildings to make space for new buildings
				continue;
			}
			if (!LatticeUtil.isPit(location)) {
				int elevation = controller.senseElevation(location);
				// Fill up to target elevation or dig to targetElevation + 3
				if (elevation < targetElevation) {
					if (targetElevation - elevation <= LANDSCAPING_THRESHOLD) {
						// Try to raise elevation
						int distanceSquared = (int) (Math.sqrt(ourLocationHQ.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
							bestDistanceSquared = distanceSquared;
							bestLocation = location;
							bestRaise = true;
						}
					}
				} else if (elevation > upperTargetElevation) {
					if (elevation - upperTargetElevation <= LANDSCAPING_THRESHOLD) {
						// Try to lower elevation
						int distanceSquared = (int) (Math.sqrt(ourLocationHQ.distanceSquaredTo(location)) + Math.sqrt(currentLocation.distanceSquaredTo(location)));
						if (distanceSquared < bestDistanceSquared) {
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
		MapLocation currentLocation = controller.getLocation();
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
		MapLocation currentLocation = controller.getLocation();
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
	public void tryDigFromPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(controller.getLocation())) {
			if (controller.canDigDirt(pitDirection)) {
				controller.digDirt(pitDirection);
				break;
			}
		}
	}
	public void tryDepositToPit() throws GameActionException {
		for (Direction pitDirection : LatticeUtil.getPitDirections(controller.getLocation())) {
			MapLocation location = controller.getLocation().add(pitDirection);
			if (controller.canDepositDirt(pitDirection) && (!location.equals(SharedInfo.getOurHQLocation()))) {
				controller.depositDirt(pitDirection);
				break;
			}
		}
	}
}
