package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class FulfillmentCenter implements RunnableBot {
	private RobotController controller;
	public FulfillmentCenter(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		int attackerMinerId = SharedInfo.getAttackerMinerId();
		if (controller.canSenseRobot(attackerMinerId)) {
			boolean seeDrone = false;
			for (RobotInfo robot : Cache.ALL_FRIENDLY_ROBOTS) {
				if (robot.getType() == RobotType.DELIVERY_DRONE) {
					seeDrone = true;
				}
			}
			if (!seeDrone) {
				RobotInfo attackerMiner = controller.senseRobot(attackerMinerId);
				Util.trySafeBuildTowards(RobotType.DELIVERY_DRONE, attackerMiner.getLocation());
			}
		}
	}
}
