package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
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
		if ((controller.getTeamSoup() >= 150 && spawnCount < 9) ||
				controller.getTeamSoup() > 500 || (controller.getRoundNum() < 100 && spawnCount < 3)) {
			if (Util.trySafeBuildTowardsEnemyHQ(RobotType.LANDSCAPER)) {
				this.spawnCount++;
			}
		}
	}
}
