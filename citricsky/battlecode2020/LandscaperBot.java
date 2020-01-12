package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	private InfoMap infoMap;
	private Pathfinding pathfinding;
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
		pathfinding = new Pathfinding();
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
					pathfinding.execute(enemyHQ);
				}
			}
		}
		else {
			if (ourHQ == null) {
				Util.randomExplore();
			}
			else {
				if(controller.getLocation().isWithinDistanceSquared(ourHQ, 2)) {
					wallCreator = true;
					turn();
					return;
				}
				else {
					if (controller.canSenseRadiusSquared(controller.getLocation().distanceSquaredTo(ourHQ) + 2)) {
						for (Direction dir : Util.ADJACENT_DIRECTIONS) {
							MapLocation new_loc = ourHQ.add(dir);
							if (controller.canSenseLocation(new_loc) && !controller.isLocationOccupied(new_loc)) {
								pathfinding.execute(new_loc);
								return;
							}
						}
						// All locations were blocked.
						attacking = true;
						turn();
					} else {
						pathfinding.execute(ourHQ);
					}
				}
			}
		}
	}

	private void createWall() throws GameActionException  {
		MapLocation ourHQ = SharedInfo.getOurHQLocation();
		if (controller.isReady()) {
			Direction direction = controller.getLocation().directionTo(ourHQ).opposite();
			if (controller.canDepositDirt(Direction.CENTER)) {
				controller.depositDirt(Direction.CENTER);
			} else if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
			}
		}
	}
}
