package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Util;

public class FulfillmentCenter implements RunnableBot {
	private RobotController controller;
	private boolean spawned;
	public FulfillmentCenter(RobotController controller) {
		this.controller = controller;
		this.spawned = false;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		if (!spawned) {
			if (Util.trySafeBuildTowardsEnemyHQ(RobotType.DELIVERY_DRONE)) {
				spawned = true;
			}
		}
	}
}
