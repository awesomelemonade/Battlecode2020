package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class DesignSchoolBot implements RunnableBot {
	private RobotController controller;
	public DesignSchoolBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		if (!MinerBot.isValidDesignSchoolLocation(Cache.CURRENT_LOCATION,
				controller.senseElevation(Cache.CURRENT_LOCATION))) {
			controller.disintegrate();
		}
		// TODO: If we see a drone, we should not build landscapers
		if (controller.getTeamSoup() < RobotType.LANDSCAPER.cost) {
			return;
		}
		if (controller.getTeamSoup() >= BuildOrder.getSoupThreshold(RobotType.LANDSCAPER)) {
			tryBuildLandscaper();
		}
		/*if (shouldLocalBuildLandscaper()) {
			tryBuildLandscaper();
		} else {
			if (controller.getTeamSoup() >= BuildOrder.getSoupThreshold(RobotType.LANDSCAPER)) {
				tryBuildLandscaper();
			}
		}*/
	}
	public static boolean shouldLocalBuildLandscaper() {
		int friendlyLandscapersCount = 0;
		int enemyLandscapersCount = 0;
		boolean seeEnemyBuilding = false;
		for (RobotInfo robot : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
			if (robot.getType() == RobotType.LANDSCAPER) {
				enemyLandscapersCount++;
			} else if (robot.getType().isBuilding()) {
				seeEnemyBuilding = true;
			}
		}
		if (seeEnemyBuilding) {
			return true;
		} else {
			for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
				if (robot.getType() == RobotType.LANDSCAPER) {
					friendlyLandscapersCount++;
				}
			}
			return enemyLandscapersCount > friendlyLandscapersCount;
		}
	}
	public static void tryBuildLandscaper() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		MapLocation location = SharedInfo.getEnemyHQLocation();
		if (location == null) {
			location = Cache.MAP_CENTER_LOCATION;
		}
		Direction idealDirection = currentLocation.directionTo(location);
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation temp = currentLocation.add(direction);
			if (!LatticeUtil.isPit(temp) &&
					Util.canSafeBuildRobot(RobotType.LANDSCAPER, direction)) {
				Cache.controller.buildRobot(RobotType.LANDSCAPER, direction);
				SharedInfo.builtNewLandscaper();
				return;
			}
		}
	}
}
