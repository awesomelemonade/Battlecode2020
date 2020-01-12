package citricsky.battlecode2020.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

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

	public static void init(RobotController controller) {
		CommunicationProcessor.controller = controller;
		turn = 1;
		messageQueue = new int[QUEUE_LENGTH][];
		costQueue = new int[QUEUE_LENGTH];
		queueIndex = 0;
		queueSize = 0;
	}
	public static void queueMessage(int[] message, int cost) {
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
					queueIndex++;
					queueSize--;
				} else {
					System.out.println("Unable to send message?" + Arrays.toString(message) + " w/ cost=" + cost);
					break;
				}
			} else {
				break;
			}
		}
	}
	public static void processAll() throws GameActionException {
		while (process()) {}
	}
	public static boolean process() throws GameActionException {
		if (turn >= controller.getRoundNum()) {
			// Not yet available
			return false;
		}
		Transaction[] transactions = controller.getBlock(turn);
		for (int i = 0; i < transactions.length; i++) {
			processTransaction(transactions[i]);
		}
		turn++;
		return true;
	}
	public static void processTransaction(Transaction transaction) {
		if (transaction.getCost() < SharedInfo.TRANSACTION_COST) {
			return; // Not worth bytecode (for now)
		}
		int[] message = transaction.getMessage();
		if (Communication.verifyTransaction(message)) {
			SharedInfo.processMessage(message);
		}
	}
}
