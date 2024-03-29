package citricsky.battlecode2020.util;

import battlecode.common.GameConstants;

public class CommunicationAttacks {
	private static final int QUEUE_LENGTH = 20;
	private static int[][] messageQueue;
	private static int[] mostRecentMessage;
	private static int mostRecentCost;
	private static int[] costQueue;
	private static int queueSize;
	private static int queueIndex;
	public static void init() {
		messageQueue = new int[QUEUE_LENGTH][];
		costQueue = new int[QUEUE_LENGTH];
		attackCounter = 0;
		mostRecentMessage = null;
		mostRecentCost = Integer.MAX_VALUE;
		queueSize = 0;
		queueIndex = 0;
	}
	private static int randomOneBit() {
		return 0b1 << Util.getRandom().nextInt(32);
	}
	private static int attackCounter;
	public static int getAttackCount() {
		return attackCounter;
	}
	public static void sendAttack() {
		// Communication attack in a set order
		switch (attackCounter) {
			case 0:
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(0)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(1)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(2)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(3)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(4)), 1);
				sendRandomBitFlipAttack();
				sendRandomBitFlipAttack();
				break;
			case 1:
				sendRecentDOSAttack();
			case 2:
				sendRandomBitFlipAttack();
				sendRandomBitFlipAttack();
			case 3:
				sendBytecodeDOS();
			default:
				switch(Util.getRandom().nextInt(6)) {
					case 0:
						sendRecentDOSAttack();
						break;
					case 1:
						sendBytecodeDOS();
						break;
					case 2:
					case 3:
					case 4:
						sendRandomBitFlipAttack();
						sendRandomBitFlipAttack();
						sendRandomBitFlipAttack();
						break;
					case 5:
						sendRecentAttack();
						break;
				}
		}
		attackCounter++;
	}
	public static void sendRecentAttack() {
		if (mostRecentCost <= 2) {
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
		}
	}
	public static void sendRecentDOSAttack() {
		if (mostRecentCost <= 2) {
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
			CommunicationProcessor.queueMessage(mostRecentMessage, mostRecentCost);
		}
	}
	public static void sendRandomBitFlipAttack() {
		CommunicationProcessor.queueMessage(getBitFlipAttack(getRandomEnemyMessage()), 1);
	}
	public static void sendBytecodeDOS() {
		// Sends a bunch of replay attacks - block from random offset
		if (queueSize >= GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
			int randomOffset = Util.getRandom().nextInt(queueSize);
			for (int i = 0; i < GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK; i++) {
				int index = (queueIndex + (randomOffset + i) % queueSize) % QUEUE_LENGTH;
				CommunicationProcessor.queueMessage(messageQueue[index], Math.min(costQueue[index], 3));
			}
		}
	}
	public static void addEnemyMessage(int[] message, int cost) {
		if (queueSize < QUEUE_LENGTH) {
			messageQueue[(queueIndex + queueSize) % QUEUE_LENGTH] = message;
			costQueue[(queueIndex + queueSize++) % QUEUE_LENGTH] = cost;
		}
		mostRecentMessage = message;
		mostRecentCost = cost;
	}
	public static int[] getNthEnemyMessage(int n) {
		return messageQueue[(queueIndex + n) % QUEUE_LENGTH];
	}
	public static int[] getRandomEnemyMessage() {
		return messageQueue[(queueIndex + Util.getRandom().nextInt(queueSize)) % QUEUE_LENGTH];
	}
	public static int[] getBitFlipAttack(int[] message) {
		if (message == null) {
			return null;
		}
		int i = Util.getRandom().nextInt(message.length);
		int[] newMessage = copy(message);
		newMessage[i] = newMessage[i] ^ randomOneBit();
		return newMessage;
	}
	public static int[] copy(int[] message) {
		int[] newMessage = new int[message.length];
		for (int i = message.length; --i >= 0;) {
			newMessage[i] = message[i];
		}
		return newMessage;
	}
}
