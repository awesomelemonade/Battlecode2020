package citricsky.battlecode2020.util;

import battlecode.common.*;

import java.util.Arrays;

public class CommunicationProcessor {
	private static RobotController controller;
	private static int turn;
	// Simulates ArrayDeque
	private static final int QUEUE_LENGTH = 100;
	private static int[][] messageQueue;
	private static int[] costQueue;
	private static int queueIndex;
	private static int queueSize;
	private static int minTransactionsCost;

	public static void init(RobotController controller) {
		CommunicationProcessor.controller = controller;
		turn = 1;
		messageQueue = new int[QUEUE_LENGTH][];
		costQueue = new int[QUEUE_LENGTH];
		queueIndex = 0;
		queueSize = 0;
		minTransactionsCost = 1;
	}
	public static void queueMessage(int[] message) {
		queueMessage(message, minTransactionsCost);
	}
	public static void queueMessage(int[] message, int cost) {
		if (message == null) {
			return;
		}
		int index = (queueIndex + queueSize) % QUEUE_LENGTH;
		messageQueue[index] = message;
		costQueue[index] = cost;
		queueSize++;
	}
	public static void sendAll() throws GameActionException {
		while (queueSize > 0) {
			int index = queueIndex % QUEUE_LENGTH;
			int cost = costQueue[index];
			if (controller.getTeamSoup() >= costQueue[index]) {
				int[] message = messageQueue[index];
				if (controller.canSubmitTransaction(message, cost)) {
					controller.submitTransaction(message, cost);
				} else {
					Cache.controller.setIndicatorDot(Cache.CURRENT_LOCATION, 255, 0, 0);
					System.out.println("Unable to send message?" + Arrays.toString(message) + " w/ cost=" + cost);
				}
				queueIndex++;
				queueSize--;
			} else {
				break;
			}
		}
	}
	public static void processAll() throws GameActionException {
		while (turn < controller.getRoundNum()) {
			Transaction[] transactions = controller.getBlock(turn);
			minTransactionsCost = transactions.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK ? Integer.MAX_VALUE : 1;
			for (int i = transactions.length; --i >= 0;) {
				processTransaction(transactions[i]);
				minTransactionsCost = Math.min(minTransactionsCost, transactions[i].getCost());
			}
			turn++;
		}
	}
	private static int immediateReplayAttackCount = 0;
	public static void processTransaction(Transaction transaction) {
		int[] message = transaction.getMessage();
		int verifyState = Communication.verifyMessage(message);
		switch (verifyState) {
			case Communication.VERIFY_STATE_UNKNOWN_HASH:
				if (Cache.ROBOT_TYPE == RobotType.HQ) {
					CommunicationAttacks.addEnemyMessage(message, transaction.getCost());
					if (SharedInfo.getVaporatorCount() >= 5) {
						if (CommunicationAttacks.getAttackCount() < 10) {
							controller.setIndicatorDot(Cache.CURRENT_LOCATION, 128, 128, 128);
							if (immediateReplayAttackCount < 5) {
								CommunicationAttacks.sendRecentAttack();
								immediateReplayAttackCount++;
							} else {
								CommunicationAttacks.sendAttack();
							}
						}
					}
				}
				break;
			case Communication.VERIFY_STATE_SUCCESS:
				SharedInfo.processMessage(message);
				break;
		}
	}
	public static int getMinTransactionsCost() {
		return minTransactionsCost;
	}
}
