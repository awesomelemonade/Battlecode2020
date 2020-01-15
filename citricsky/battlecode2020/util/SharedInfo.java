package citricsky.battlecode2020.util;

import battlecode.common.*;

public class SharedInfo {
	public static final int TRANSACTION_COST = 3;

	private static final int ENEMYHQ_SIGNATURE = 2130985;
	private static final int ENEMYHQ_MODE_SIGNATURE = 415912;
	private static final int OURHQ_SIGNATURE = 51351235;

	private static RobotController controller;
	private static MapLocation ourHQLocation;
	private static int ourHQParityX = -1;
	private static int ourHQParityY = -1;
	private static MapLocation enemyHQLocation;
	private static int enemyHQGuesserMode = EnemyHQGuesser.UNKNOWN_MODE;


	public static void init(RobotController controller) {
		SharedInfo.controller = controller;
		EnemyHQGuesser.init(controller);
	}
	private static void checkEnemyHQLocation() {
		// TODO: Remove

	}
	public static void sendEnemyHQ(MapLocation location) {
		setEnemyHQLocation(location);
		int[] message = new int[] {
				ENEMYHQ_SIGNATURE, 0, 0, 0, enemyHQLocation.x, enemyHQLocation.y, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendOurHQ(MapLocation location) {
		setOurHQLocation(location);
		int[] message = new int[] {
				OURHQ_SIGNATURE, 0, 0, 0, location.x, location.y, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendEnemyGuessMode(int mode) {
		setEnemyHQGuesserMode(mode);
		int[] message = new int[] {
				ENEMYHQ_MODE_SIGNATURE, 0, 0, 0, 0, mode, 0
		};
		Communication.hashTransaction(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void processMessage(int[] message) {
		switch(message[0]) {
			case ENEMYHQ_SIGNATURE:
				int x = message[4];
				int y = message[5];
				setEnemyHQLocation(new MapLocation(x, y));
				break;
			case OURHQ_SIGNATURE:
				int x2 = message[4];
				int y2 = message[5];
				setOurHQLocation(new MapLocation(x2, y2));
				break;
			case ENEMYHQ_MODE_SIGNATURE:
				int mode = message[5];
				setEnemyHQGuesserMode(mode);
				break;
		}
	}
	private static void setOurHQLocation(MapLocation location) {
		EnemyHQGuesser.setGuesses(location.x, location.y);
		ourHQLocation = location;
		ourHQParityX = location.x % 3;
		ourHQParityY = location.y % 3;
	}
	public static MapLocation getOurHQLocation() {
		return ourHQLocation;
	}
	public static int getOurHQParityX() {
		return ourHQParityX;
	}
	public static int getOurHQParityY() {
		return ourHQParityY;
	}
	private static void setEnemyHQLocation(MapLocation location) {
		enemyHQLocation = location;
	}
	public static MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}
	private static void setEnemyHQGuesserMode(int mode) {
		enemyHQGuesserMode = mode;
	}
	public static int getEnemyHQGuesserMode() {
		return enemyHQGuesserMode;
	}
}
