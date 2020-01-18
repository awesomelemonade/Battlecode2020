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
		MapLocation currentLocation = controller.getLocation();
		if (controller.isCurrentlyHoldingUnit()) {
			// Find Water
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
				if (controller.senseFlooding(location)) {
					// Move towards location
					if (currentLocation.isAdjacentTo(location)) {
						Direction direction = currentLocation.directionTo(location);
						if (controller.canDropUnit(direction)) {
							controller.dropUnit(direction);
						}
					} else {
						Pathfinding.execute(location);
					}
					return;
				}
			}
			Util.randomExplore();
		} else {
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
			int distanceSquared = controller.getLocation().distanceSquaredTo(enemy.getLocation());
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
}
