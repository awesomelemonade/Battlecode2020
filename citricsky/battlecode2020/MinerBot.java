package citricsky.battlecode2020;

import battlecode.common.*;

public class MinerBot implements RunnableBot {
	private RobotController controller;
	public MinerBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {

	}
	@Override
	public void turn() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (controller.getSoupCarrying() < RobotType.MINER.soupLimit) {
			// Try to mine soup
			for (Direction direction : Direction.values()) {
				if (controller.canMineSoup(direction)) {
					controller.mineSoup(direction);
					return;
				}
			}
			// Move towards visible soup
		} else {
			// Try to deposit soup
			for (Direction direction : Direction.values()) {
				if (controller.canDepositSoup(direction)) {
					controller.depositSoup(direction, controller.getSoupCarrying());
					return;
				}
			}
			// Move towards HQ or refinery
		}
	}
}
