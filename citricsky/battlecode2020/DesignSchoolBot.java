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
		if (!MinerBot.isValidDesignSchoolLocation(Cache.CURRENT_LOCATION)) {
			controller.disintegrate();
		}
		if (controller.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
			if (SharedInfo.getOurHQState() == HQBot.NO_ADDITIONAL_HELP_NEEDED) {
				// We need to create drones or net guns
				return;
			}
			// Listen to distress signal
			if (SharedInfo.getOurHQState() != HQBot.NEEDS_HELP) {
				boolean buildInitialTwoLandscapers = spawnCount < 2;
				boolean buildAfterThreeVaporators = spawnCount < 6 && SharedInfo.getVaporatorCount() >= 3;
				boolean buildAfterFulfillmentCenterAndVaporators = spawnCount < 12 &&
						SharedInfo.getFulfillmentCenterCount() > 0 && SharedInfo.getVaporatorCount() >= 3;
				if (!(buildInitialTwoLandscapers || buildAfterThreeVaporators || buildAfterFulfillmentCenterAndVaporators)) {
					if ((controller.getTeamSoup() < RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost +
							SharedInfo.getMissingBuildingsCost()) || Math.random() < 0.5) {
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
						if (!seeEnemyBuilding) {
							for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
								if (robot.getType() == RobotType.LANDSCAPER) {
									friendlyLandscapersCount++;
								}
							}
							if (enemyLandscapersCount <= friendlyLandscapersCount) {
								return;
							}
						}
					}
				}
			}
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
					controller.buildRobot(RobotType.LANDSCAPER, direction);
					spawnCount++;
					return;
				}
			}
		}
	}
}
