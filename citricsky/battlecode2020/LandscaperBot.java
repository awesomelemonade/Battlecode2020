package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	private InfoMap infoMap;
	private Pathfinding pathfinding;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
		pathfinding = new Pathfinding();
	}
	@Override
	public void turn() throws GameActionException {
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
}
