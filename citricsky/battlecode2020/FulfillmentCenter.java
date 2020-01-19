package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Util;

public class FulfillmentCenter implements RunnableBot {
	private RobotController controller;
	private int spawnCount = 0;
	public FulfillmentCenter(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (spawnCount > 0 && controller.getRoundNum() < 1000) {
			return;
		}
		if (Util.trySafeBuildTowardsEnemyHQ(RobotType.DELIVERY_DRONE)) {
			spawnCount++;
		}
	}
}
