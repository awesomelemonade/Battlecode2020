package citricsky.battlecode2020;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Util;

public class NetGunBot implements RunnableBot {
	private RobotController controller;
	public NetGunBot(RobotController controller) {
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
		Util.tryShootDrone();
	}
}
