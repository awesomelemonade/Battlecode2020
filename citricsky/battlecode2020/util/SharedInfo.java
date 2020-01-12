package citricsky.battlecode2020.util;

import battlecode.common.*;

import java.util.Arrays;

public class SharedInfo {
	private static RobotController controller;
	private static MapLocation ourHQLocation;
	private static MapLocation enemyHQLocation;
	private static int attackerMinerId;
	public static final int TRANSACTION_COST = 3;

	private static final int ENEMYHQ_SIGNATURE = 2130985;
	private static final int ENEMYHQ_MODE_SIGNATURE = 415912;
	private static final int OURHQ_SIGNATURE = 51351235;
	private static final int ATTACKER_MINER_SIGNATURE = 9613451;

	public static void init(RobotController controller) {
		SharedInfo.controller = controller;
		EnemyHQGuesser.init(controller);
	}
	public static void loop() throws GameActionException {
		CommunicationProcessor.processAll();
		checkEnemyHQLocation();
		CommunicationProcessor.sendAll();
	}
	private static void checkEnemyHQLocation() {
		if (enemyHQLocation == null) {
			for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
				if (enemy.getType() == RobotType.HQ) {
					SharedInfo.sendEnemyHQ(enemy.getLocation());
					return;
				}
			}
			EnemyHQGuesser.loop();
		}
	}
	public static void sendEnemyHQ(MapLocation location) {
		enemyHQLocation = location;
		int[] message = new int[] {
				ENEMYHQ_SIGNATURE, 0, 0, 0, enemyHQLocation.x, enemyHQLocation.y, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendOurHQ(MapLocation location) {
		setOurHQLocation(location);
		int[] message = new int[] {
				OURHQ_SIGNATURE, 0, 0, 0, ourHQLocation.x, ourHQLocation.y, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendEnemyGuessMode(int mode) {
		int[] message = new int[] {
				ENEMYHQ_MODE_SIGNATURE, 0, 0, 0, 0, mode, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendAttackerMinerId(int id) {
		int[] message = new int[] {
				ATTACKER_MINER_SIGNATURE, 0, 0, 0, 0, id, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void processMessage(int[] message) {
		switch(message[0]) {
			case ENEMYHQ_SIGNATURE:
				if (enemyHQLocation == null) {
					int x = message[4];
					int y = message[5];
					enemyHQLocation = new MapLocation(x, y);
				}
				break;
			case OURHQ_SIGNATURE:
				if (ourHQLocation == null) {
					int x2 = message[4];
					int y2 = message[5];
					setOurHQLocation(new MapLocation(x2, y2));
				}
				break;
			case ENEMYHQ_MODE_SIGNATURE:
				int mode = message[5];
				EnemyHQGuesser.setMode(mode);
				break;
			case ATTACKER_MINER_SIGNATURE:
				attackerMinerId = message[5];
				break;
		}
	}
	public static void setOurHQLocation(MapLocation location) {
		ourHQLocation = location;
		EnemyHQGuesser.setGuesses(location.x, location.y);
	}
	public static MapLocation getOurHQLocation() {
		return ourHQLocation;
	}
	public static MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}
	public static int getAttackerMinerId() {
		return attackerMinerId;
	}
}
