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
				() -> tryHeal(SharedInfo.getOurHQLocation()),
				() -> tryHeal(getNearestBuilding(Cache.ALL_NEARBY_FRIENDLY_ROBOTS)),
				this::tryTerraform,
				() -> tryAttack(getNearestBuilding(Cache.ALL_NEARBY_ENEMY_ROBOTS)),
				() -> tryAttack(SharedInfo.getEnemyHQLocation()),
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
	public boolean tryHeal(MapLocation location) throws GameActionException {
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
	public boolean tryAttack(MapLocation location) throws GameActionException {
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
	public boolean tryTerraform() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		int targetElevation = (this.targetElevation - this.targetElevation % 3) + 5;
		int upperTargetElevation = targetElevation + GameConstants.MAX_DIRT_DIFFERENCE;
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (!controller.canSenseLocation(location)) {
				return false;
			}
			RobotInfo robot = controller.senseRobotAtLocation(location);
			if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
				continue;
			}
			if (!LatticeUtil.isPit(location)) {
				int elevation = controller.senseElevation(location);
				// Fill up to target elevation or dig to targetElevation + 3
				if (elevation < targetElevation) {
					if (targetElevation - elevation <= LANDSCAPING_THRESHOLD) {
						// Try to raise elevation
						tryToRaise(location);
						return true;
					}
				} else if (elevation > upperTargetElevation) {
					if (elevation - upperTargetElevation <= LANDSCAPING_THRESHOLD) {
						// Try to lower elevation
						tryToLower(location);
						return true;
					}
				}
			}
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
				for (Direction pitDirection : LatticeUtil.getPitDirections(currentLocation)) {
					if (controller.canDigDirt(pitDirection)) {
						controller.digDirt(pitDirection);
						break;
					}
				}
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
				for (Direction pitDirection : LatticeUtil.getPitDirections(currentLocation)) {
					if (controller.canDepositDirt(pitDirection)) {
						controller.depositDirt(pitDirection);
						break;
					}
				}
			}
		} else {
			Pathfinding.execute(location);
		}
	}
}
