package citricsky.battlecode2020.util;

import battlecode.common.*;
import citricsky.battlecode2020.HQBot;

public class SharedInfo {
	public static final int TRANSACTION_COST = 3;

	private static final int OURHQ_STATE_SIGNATURE = 8963124;
	private static final int ENEMYHQ_SIGNATURE = 2130985;
	private static final int ENEMYHQ_MODE_SIGNATURE = 415912;
	private static final int OURHQ_SIGNATURE = 51351235;

	private static RobotController controller;
	private static MapLocation ourHQLocation;
	private static int ourHQParityX = -1;
	private static int ourHQParityY = -1;
	private static MapLocation enemyHQLocation;
	private static int enemyHQGuesserMode = EnemyHQGuesser.UNKNOWN_MODE;
	private static int ourHQState = HQBot.NO_HELP_NEEDED;

	public static void init(RobotController controller) {
		SharedInfo.controller = controller;
		EnemyHQGuesser.init(controller);
	}
	public static void sendEnemyHQ(MapLocation location) {
		setEnemyHQLocation(location);
		int[] message = new int[] {
				ENEMYHQ_SIGNATURE, 0, 0, 0, enemyHQLocation.x, enemyHQLocation.y, controller.getRoundNum()
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendOurHQ(MapLocation location) {
		setOurHQLocation(location);
		int[] message = new int[] {
				OURHQ_SIGNATURE, 0, 0, 0, location.x, location.y, controller.getRoundNum()
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendEnemyGuessMode(int mode) {
		setEnemyHQGuesserMode(mode);
		int[] message = new int[] {
				ENEMYHQ_MODE_SIGNATURE, 0, 0, 0, 0, mode, controller.getRoundNum()
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void sendOurHQState(int state) {
		setOurHQState(state);
		int[] message = new int[] {
				OURHQ_STATE_SIGNATURE, 0, 0, 0, 0, state, controller.getRoundNum()
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message, TRANSACTION_COST);
	}
	public static void processMessage(int[] message) {
		switch(message[0]) {
			case ENEMYHQ_SIGNATURE:
				setEnemyHQLocation(new MapLocation(message[4], message[5]));
				break;
			case OURHQ_SIGNATURE:
				setOurHQLocation(new MapLocation(message[4], message[5]));
				break;
			case ENEMYHQ_MODE_SIGNATURE:
				setEnemyHQGuesserMode(message[5]);
				break;
			case OURHQ_STATE_SIGNATURE:
				setOurHQState(message[5]);
				break;
		}
	}
	private static void setOurHQLocation(MapLocation location) {
		EnemyHQGuesser.setGuesses(location.x, location.y);
		ourHQLocation = location;
		ourHQParityX = location.x % 2;
		ourHQParityY = location.y % 2;
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
	private static void setOurHQState(int state) {
		ourHQState = state;
	}
	public static int getOurHQState() {
		return ourHQState;
	}
}
