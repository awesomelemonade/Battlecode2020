package citricsky.battlecode2020;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import citricsky.RunnableBot;
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
		if ((controller.getTeamSoup() > 220 && spawnCount < 9) ||
				controller.getTeamSoup() > 500 || (controller.getRoundNum() < 100 && spawnCount < 3)) {
			tryBuildLandscaper();
		}
	}
	public void tryBuildLandscaper() throws GameActionException {
		Direction direction = Util.randomAdjacentDirection();
		if (Util.canSafeBuildRobot(RobotType.LANDSCAPER, direction)) {
			controller.buildRobot(RobotType.LANDSCAPER, direction);
			this.spawnCount++;
		}
	}
}
