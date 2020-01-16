package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.LatticeUtil;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class DesignSchoolBot implements RunnableBot {
	private RobotController controller;
	private int spawnCount;
	public DesignSchoolBot(RobotController controller) {
		this.controller = controller;
		this.spawnCount = 0;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		if (controller.getTeamSoup() > 200) {
			if (controller.getTeamSoup() < RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost + 50) {
				for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
					if (robot.getType() == RobotType.LANDSCAPER) {
						return;
					}
				}
			}
			MapLocation currentLocation = controller.getLocation();
			MapLocation location = SharedInfo.getEnemyHQLocation();
			if (location == null) {
				location = Cache.MAP_CENTER_LOCATION;
			}
			Direction idealDirection = currentLocation.directionTo(location);
			for (Direction direction : Util.getAttemptOrder(idealDirection)) {
				MapLocation temp = currentLocation.add(direction);
				if (!LatticeUtil.isPit(temp) &&
						Util.canSafeBuildRobot(RobotType.LANDSCAPER, direction)) {
					controller.buildRobot(RobotType.LANDSCAPER, direction);
					return;
				}
			}
		}
	}
}
