package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.battlecode2020.util.InfoMap;
import citricsky.battlecode2020.util.Pathfinding;
import citricsky.battlecode2020.util.Util;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	private MapLocation enemyHQ;
	private InfoMap infoMap;
	private Pathfinding pathfinding;
	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
		pathfinding = new Pathfinding(controller);
	}
	@Override
	public void turn() throws GameActionException {
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
		// Find enemy HQ (by exploring)
		if (enemyHQ == null) {
			RobotInfo[] enemyUnits = controller.senseNearbyRobots(-1, controller.getTeam().opponent());
			for (RobotInfo enemy : enemyUnits) {
				if (enemy.getType() == RobotType.HQ) {
					this.enemyHQ = enemy.getLocation();
					break;
				}
			}
		}
		// Dig trench from water to hq
		if (enemyHQ == null) {
			Util.randomWalk();
		} else {
			pathfinding.execute(enemyHQ);
		}
	}
}
