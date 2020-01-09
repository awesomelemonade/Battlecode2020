package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private static final int TRANSACTION_COST = 10;
	private RobotController controller;
	private MapLocation enemyHQ;
	private InfoMap infoMap;
	private Pathfinding pathfinding;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
		pathfinding = new Pathfinding(controller);
		CommunicationProcessor.init(controller);
	}
	@Override
	public void turn() throws GameActionException {
		// Process Communication
		CommunicationProcessor.processAll();
		String magicString = "[12345|12345|12345|12345|";
		StringBuilder stringBuilder = CommunicationProcessor.getStringBuilder();
		int index = stringBuilder.indexOf(magicString);
		if (index != -1) {
			int endIndex = stringBuilder.indexOf("]", index);
			String info = stringBuilder.substring(index + magicString.length(), endIndex);
			int separatorIndex = info.indexOf('|');
			int x = Integer.parseInt(info.substring(0, separatorIndex));
			int y = Integer.parseInt(info.substring(separatorIndex + 1));
			enemyHQ = new MapLocation(x, y);
			// temporary
			stringBuilder.delete(0, stringBuilder.length());
		}
		// Turn logic
		MapLocation currentLocation = controller.getLocation();
		// Update explore map
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (controller.canSenseLocation(location)) {
				infoMap.set(location, 1);
			} else {
				break;
			}
		}
		// Find enemy HQ (by exploring)
		if (enemyHQ == null) {
			RobotInfo[] enemyUnits = controller.senseNearbyRobots(-1, controller.getTeam().opponent());
			for (RobotInfo enemy : enemyUnits) {
				if (enemy.getType() == RobotType.HQ) {
					this.enemyHQ = enemy.getLocation();
					if (controller.getTeamSoup() >= TRANSACTION_COST) {
						int[] message = new int[] {
								12345, 12345, 12345, 12345, enemyHQ.x, enemyHQ.y, 0
						};
						Communication.hashTransaction(message);
						if (controller.canSubmitTransaction(message, TRANSACTION_COST)) {
							controller.submitTransaction(message, TRANSACTION_COST);
						}
					}
					break;
				}
			}
		}
		// Dig trench from water to hq
		if (enemyHQ == null) {
			Util.randomWalk();
		} else {
			if (controller.getLocation().isWithinDistanceSquared(enemyHQ, 2)) {
				if (!controller.isReady()) {
					return;
				}
				Direction direction = controller.getLocation().directionTo(enemyHQ);
				if (controller.canDepositDirt(direction)) {
					controller.depositDirt(direction);
					return;
				}
				if (controller.canDigDirt(Direction.CENTER)) {
					controller.digDirt(Direction.CENTER);
					return;
				}
			} else {
				pathfinding.execute(enemyHQ);
			}
		}
	}
}
