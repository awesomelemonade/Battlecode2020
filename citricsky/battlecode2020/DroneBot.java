package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DroneBot implements RunnableBot {
	private RobotController controller;
	private boolean pickedUpUnit;
	private Direction lastRandomDirection;
	public DroneBot(RobotController controller) {
		this.controller = controller;
		this.pickedUpUnit = false;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (controller.isCurrentlyHoldingUnit()) {
			// Find adjacent water
			if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
				for (Direction direction : Util.ADJACENT_DIRECTIONS) {
					MapLocation location = currentLocation.add(direction);
					if (Util.onTheMap(location) && controller.senseFlooding(location)) {
						if (controller.canDropUnit(direction)) {
							controller.dropUnit(direction);
							return;
						}
					}
				}
			}
			// Find Water
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
					// Move towards location
					Pathfinding.execute(location);
					return;
				}
			}
			Util.randomExplore();
		} else {
			//if we don't know where the enemy HQ is, proceed as normal (or if the game is early enough that potential disruption > drone life value)
			if (SharedInfo.getEnemyHQLocation() == null || (SharedInfo.getEnemyHQLocation() != null && controller.getRoundNum() < 500)) {
			// Find Target
				RobotInfo target = findBestTarget();
				if (target == null) {
					Util.randomExplore();
				} else {
					if (currentLocation.isAdjacentTo(target.getLocation())) {
						if (controller.canPickUpUnit(target.getID())) {
							controller.pickUpUnit(target.getID());
						}
					} else {
						Pathfinding.execute(target.getLocation());
					}
				}
			}
			else {
				for (Direction dir : Util.ADJACENT_DIRECTIONS) {
					RobotInfo target = controller.senseRobotAtLocation(Cache.CURRENT_LOCATION.add(dir));
					if(target != null){
						if(controller.canPickUpUnit(target.getID())) {
							controller.pickUpUnit(target.getID());
						}
					}
				}
				if(SharedInfo.attacking) {
					Pathfinding.execute(SharedInfo.getEnemyHQLocation());
				}
				else {
					avoidHQWalk();
				}
				
			}
			
		}
	}
	public RobotInfo findBestTarget() {
		RobotInfo best = null;
		int bestPriority = -1;
		int bestDistanceSquared = -1;
		for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			int priority = getPriority(enemy.getType());
			if (priority == 0) {
				// Cannot pick up
				continue;
			}
			int distanceSquared = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
			if (best == null || priority > bestPriority || (priority == bestPriority && distanceSquared < bestDistanceSquared)) {
				best = enemy;
				bestPriority = priority;
				bestDistanceSquared = distanceSquared;
			}
		}
		return best;
	}
	public int getPriority(RobotType type) {
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
	//assumes enemyHQLocation is not null
	public boolean avoidHQWalk() throws GameActionException {
		if (lastRandomDirection == null) {
			lastRandomDirection = Util.randomAdjacentDirection();
		}
		boolean success = false;
		for (int i = 0; i < 16 && !success; i++) {
			if (Cache.CURRENT_LOCATION.add(lastRandomDirection).distanceSquaredTo(SharedInfo.getEnemyHQLocation()) > 20) {
				success = Pathfinding.naiveMove(lastRandomDirection);
				lastRandomDirection = Util.randomAdjacentDirection();
			}
				
		}
		return success;
	}
}
