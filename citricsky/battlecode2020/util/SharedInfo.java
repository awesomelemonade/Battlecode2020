package citricsky.battlecode2020.util;

import battlecode.common.*;

public class SharedInfo {
	private static RobotController controller;
	private static Team opponentTeam;
	private static MapLocation enemyHQLocation;
	private static final int ENEMYHQ_TRANSACTION_COST = 10;

	public static void init(RobotController controller) {
		SharedInfo.controller = controller;
		SharedInfo.opponentTeam = controller.getTeam().opponent();
	}
	public static void loop() throws GameActionException {
		CommunicationProcessor.processAll();
		if (enemyHQLocation == null) {
			RobotInfo[] enemyUnits = controller.senseNearbyRobots(-1, opponentTeam);
			for (RobotInfo enemy : enemyUnits) {
				if (enemy.getType() == RobotType.HQ) {
					SharedInfo.sendEnemyHQ(enemy.getLocation());
					break;
				}
			}
		}
		CommunicationProcessor.sendAll();
	}
	public static void sendEnemyHQ(MapLocation location) {
		enemyHQLocation = location;
		int[] message = new int[] {
				12345, 12345, 12345, 12345, enemyHQLocation.x, enemyHQLocation.y, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, ENEMYHQ_TRANSACTION_COST);
	}
	public static void processMessage(int[] message) {
		if (message[0] == 12345 && message[1] == 12345 &&
				message[2] == 12345 && message[3] == 12345) {
			int x = message[4];
			int y = message[5];
			enemyHQLocation = new MapLocation(x, y);
		}
	}
	public static MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}
}
