package citricsky;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import citricsky.battlecode2020.*;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.Util;

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
			case LANDSCAPER:
				bot = new LandscaperBot(controller);
				break;
			case DELIVERY_DRONE:
				bot = new DroneBot(controller);
				break;
			case DESIGN_SCHOOL:
				bot = new DesignSchoolBot(controller);
				break;
			case REFINERY:
				bot = new RefineryBot(controller);
				break;
			case NET_GUN:
				bot = new NetGunBot(controller);
				break;
			case FULFILLMENT_CENTER:
				bot = new FulfillmentCenterBot(controller);
				break;
			case VAPORATOR:
				bot = new VaporatorBot(controller);
				break;
			default:
				throw new IllegalStateException("Unimplemented!");
		}
		Util.init(controller);
		bot.init();
		boolean errored = false;
		while (true) {
			try {
				while (true) {
					if (errored) {
						controller.setIndicatorDot(Cache.CURRENT_LOCATION, 255, 0, 0);
					}
					int currentTurn = controller.getRoundNum();
					Util.loop();
					bot.turn();
					Util.postLoop();
					if (controller.getRoundNum() != currentTurn) {
						// We ran out of bytecodes! - MAGENTA
						controller.setIndicatorDot(Cache.CURRENT_LOCATION, 255, 0, 255);
					}
					Clock.yield();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				errored = true;
				Clock.yield();
			}
		}
	}
}
