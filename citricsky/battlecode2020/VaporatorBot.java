package citricsky.battlecode2020;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.SharedInfo;

public class VaporatorBot implements RunnableBot {
	private RobotController controller;
	public VaporatorBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() throws GameActionException {

	}
	boolean sent = false;
	@Override
	public void turn() throws GameActionException {
		if (!sent) {
			SharedInfo.sendVaporatorCountIncrement();
			sent = true;
		}
	}
}
