package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	private InfoMap infoMap;
	private boolean attacking;
	private boolean wallCreator;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
		this.attacking = false;
		this.wallCreator = false;
	}
	@Override
	public void init() {
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
	}
	@Override
	public void turn() throws GameActionException {
		if (wallCreator) {
			createWall();
			return;
		}

		// Turn logic
		MapLocation currentLocation = controller.getLocation();
		// Update explore map
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (controller.canSenseLocation(location)) {
				infoMap.set(location, 1);
			} else {
				break;
			}
		}
		// Dig trench from water to hq
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		MapLocation ourHQ = SharedInfo.getOurHQLocation();

		if (attacking) {
			if (enemyHQ == null) {
				Util.randomExplore();
			} else {
				if (controller.getLocation().isWithinDistanceSquared(enemyHQ, 2)) {
					if (controller.isReady()) {
						Direction direction = controller.getLocation().directionTo(enemyHQ);
						if (controller.canDepositDirt(direction)) {
							controller.depositDirt(direction);
						} else if (controller.canDigDirt(Direction.CENTER)) {
							controller.digDirt(Direction.CENTER);
						}
					}
				} else {
					Pathfinding.execute(enemyHQ);
				}
			}
		} else {
			if (ourHQ == null) {
				Util.randomExplore();
			} else {
				if (controller.getLocation().isWithinDistanceSquared(ourHQ, 2)) {
					wallCreator = true;
					turn();
					return;
				} else {
					boolean notVisible = false;
					for (Direction dir : Util.ADJACENT_DIRECTIONS) {
						MapLocation location = ourHQ.add(dir);
						if (controller.canSenseLocation(location)) {
							if (!controller.isLocationOccupied(location)) {
								Pathfinding.execute(location);
								return;
							}
						} else {
							notVisible = true;
						}
					}
					if (notVisible) {
						Pathfinding.execute(ourHQ);
					} else {
						// All locations were blocked
						attacking = true;
						turn();
						return;
					}
				}
			}
		}
	}

	private void createWall() throws GameActionException  {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		if (controller.isReady()) {
			if (controller.getDirtCarrying() > 0) {
				Direction bestDirection = null;
				int bestElevation = Integer.MAX_VALUE;
				for (Direction direction : Direction.values()) {
					MapLocation location = controller.getLocation().add(direction);
					int distanceSquared = location.distanceSquaredTo(ourHQ);
					if (distanceSquared <= 2 && distanceSquared > 0 && controller.canSenseLocation(location) &&
							controller.canDepositDirt(direction) && controller.isLocationOccupied(location)) {
						RobotInfo robot = controller.senseRobotAtLocation(location);
						// Only fill if it's our landscaper or an enemy
						if (robot.getTeam() == Cache.OPPONENT_TEAM || robot.getType() == RobotType.LANDSCAPER) {
							int elevation = controller.senseElevation(location);
							if (elevation < bestElevation) {
								bestElevation = elevation;
								bestDirection = direction;
							}
						}
					}
				}
				if (bestDirection != null) {
					controller.depositDirt(bestDirection);
				}
			} else {
				Direction direction = controller.getLocation().directionTo(ourHQ).opposite();
				if (controller.canDigDirt(direction)) {
					controller.digDirt(direction);
				}
			}
		}
	}
}
