package citricsky.battlecode2020;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
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
		if (spawnCount < 3) {
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
