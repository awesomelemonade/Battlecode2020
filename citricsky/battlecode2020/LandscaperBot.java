package citricsky.battlecode2020;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import citricsky.battlecode2020.util.InfoMap;
import citricsky.battlecode2020.util.Util;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	private MapLocation enemyHQ;
	private InfoMap infoMap;
	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
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
				if (controller.onTheMap(location)) {
					infoMap.set(location.x, location.y, 1);
				}
			} else {
				break;
			}
		}
		Util.randomWalk();
		// Find enemy HQ (by exploring)
		// Dig trench from water to hq
	}
}
