package citricsky.battlecode2020.util;

import battlecode.common.GameConstants;

public class CommunicationAttacks {
	private static final int QUEUE_LENGTH = 20;
	private static int[][] messageQueue;
	private static int[] costQueue;
	private static int queueSize;
	private static int queueIndex;
	public static void init() {
		messageQueue = new int[QUEUE_LENGTH][];
		costQueue = new int[QUEUE_LENGTH];
		attackCounter = 0;
	}
	private static int randomOneBit() {
		return 0b1 << Util.getRandom().nextInt(32);
	}
	private static int attackCounter;
	public static void sendAttacks() {
		// Communication attack in a set order
		switch (attackCounter) {
			case 0:
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(0)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(0)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(0)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(1)), 1);
				CommunicationProcessor.queueMessage(getBitFlipAttack(getNthEnemyMessage(1)), 1);
				sendRandomBitFlipAttack();
				sendRandomBitFlipAttack();
				break;
			case 1:
				sendBytecodeDDOS();
			case 2:
				sendRandomBitFlipAttack();
				sendRandomBitFlipAttack();
			case 3:
				sendBytecodeDDOS();
			default:
				if (Util.getRandom().nextBoolean()) {
					sendBytecodeDDOS();
				} else {
					sendRandomBitFlipAttack();
					sendRandomBitFlipAttack();
					sendRandomBitFlipAttack();
				}
		}
		attackCounter++;
	}
	public static void sendRandomBitFlipAttack() {
		CommunicationProcessor.queueMessage(getBitFlipAttack(getRandomEnemyMessage()), 1);
	}
	public static void sendBytecodeDDOS() {
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
	}
	public static int[] getNthEnemyMessage(int n) {
		return messageQueue[(queueIndex + n) % QUEUE_LENGTH];
	}
	public static int[] getRandomEnemyMessage() {
		return messageQueue[(queueIndex + Util.getRandom().nextInt(queueSize)) % QUEUE_LENGTH];
	}
	public static int[] getBitFlipAttack(int[] message) {
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
