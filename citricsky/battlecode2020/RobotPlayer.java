package citricsky.battlecode2020;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer {
	public static RobotController controller;
	public static void run(RobotController controller) throws GameActionException {
		RobotPlayer.controller = controller;
		RunnableBot bot;
		switch (controller.getType()) {
			case HQ:
				bot = new HQBot(controller);
				break;
			case MINER:
				bot = new MinerBot(controller);
				break;
			default:
				throw new IllegalStateException("Unimplemented!");
		}
		bot.init();
		while (true) {
			bot.turn();
			// TODO: Catch up on ledger while waiting
			Clock.yield();
		}
	}
}
