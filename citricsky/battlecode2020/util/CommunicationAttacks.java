package citricsky.battlecode2020.util;

public class CommunicationAttacks {
	private static final int QUEUE_LENGTH = 20;
	private static int[][] messageQueue;
	private static int[] costQueue;
	private static int queueSize;
	private static int queueIndex;
	public static void init() {
		messageQueue = new int[QUEUE_LENGTH][];
		costQueue = new int[QUEUE_LENGTH];
	}
	private static int randomOneBit() {
		return 0b1 << Util.getRandom().nextInt(32);
	}
	public static void sendAttacks() {
		//choose some random attack
		int[] message = messageQueue[(queueIndex++) % QUEUE_LENGTH];
		queueSize--;
		//send newMessage
		CommunicationProcessor.queueMessage(getBitFlipAttack(message), 1);
	}
	public static void addEnemyMessage(int[] message, int cost) {
		if (queueSize < QUEUE_LENGTH) {
			messageQueue[(queueIndex + queueSize) & QUEUE_LENGTH] = message;
			costQueue[(queueIndex + queueSize++) & QUEUE_LENGTH] = cost;
		}
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
