package citricsky.battlecode2020.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

public class CommunicationProcessor {
	private static RobotController controller;
	private static StringBuilder builder;

	private static int turn;
	public static void init(RobotController controller) {
		CommunicationProcessor.controller = controller;
		turn = 1;
		builder = new StringBuilder();
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
		if (transaction.getCost() == 0) {
			return; // Not worth bytecode
		}
		int[] message = transaction.getMessage();
		if (Communication.verifyTransaction(message)) {
			builder.append("[");
			builder.append(message[0]);
			builder.append("|");
			builder.append(message[1]);
			builder.append("|");
			builder.append(message[2]);
			builder.append("|");
			builder.append(message[3]);
			builder.append("|");
			builder.append(message[4]);
			builder.append("|");
			builder.append(message[5]);
			builder.append("]");
		}
	}
	public static StringBuilder getStringBuilder() {
		return builder;
	}
}
