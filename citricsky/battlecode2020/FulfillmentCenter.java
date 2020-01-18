package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
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
		Util.trySafeBuildTowardsEnemyHQ(RobotType.DELIVERY_DRONE);
	}
}
