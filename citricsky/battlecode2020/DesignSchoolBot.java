package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.LatticeUtil;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

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

		if (controller.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
			if (SharedInfo.getOurHQState() == HQBot.NO_ADDITIONAL_HELP_NEEDED) {
				// We need to create drones or net guns
				if (controller.getTeamSoup() < SharedInfo.getMissingBuildingsCost() +
						RobotType.LANDSCAPER.cost + RobotType.NET_GUN.cost) {
					return;
				}
			}
			boolean shouldMassCreate = SharedInfo.getVaporatorCount() >= FulfillmentCenterBot.MASS_SPAWN_VAPORATOR_THRESHOLD;
			if (!shouldMassCreate || Math.random() < 0.3) {
				// Listen to distress signal
				if (SharedInfo.getOurHQState() != HQBot.NEEDS_HELP) {
					boolean buildInitialTwoLandscapers = SharedInfo.landscapersBuilt < 2;
					boolean buildAfterThreeVaporators = SharedInfo.landscapersBuilt < 6 && SharedInfo.getVaporatorCount() >= 3;
					boolean buildAfterFulfillmentCenterAndVaporators = SharedInfo.landscapersBuilt < 16 &&
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
					SharedInfo.builtNewLandscaper();
					return;
				}
			}
		}
	}
}
