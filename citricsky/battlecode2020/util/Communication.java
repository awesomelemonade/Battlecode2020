package citricsky.battlecode2020.util;

import battlecode.common.RobotController;
import battlecode.common.Team;

public class Communication {
	// random 32 bit
	private static int SEED = 56908321;

	private static final int SIGNATURE_MASK = 0b11111111111111111111000000000000;
	private static final int ROUND_MASK = 0b00000000000000000000111111111111;
	// random 32 bit.
	private static final int SIGNATURE = 1301462582 & SIGNATURE_MASK;
	// ROTATIONS = [ord(os.urandom(1)) % 32 for i in range(7)]
	private static final int[] ROTATIONS = new int[] {17, 29, 25, 1, 27, 6, 29};

	public static void init(RobotController controller) {
		// xor the seed by map size
		int gameAdjust = (controller.getMapWidth() << 6) | controller.getMapHeight();
		gameAdjust = (gameAdjust << 1) | ((controller.getTeam() == Team.A) ? 0 : 1);
		SEED ^= gameAdjust;
		// Also preloads static variables
	}
	/**
	 * Decrypts the transaction and verifies if it is ours.
	 * @param message
	 * @return whether the transaction is verified to be ours
	 */
	public static boolean decryptMessage(int[] message, int turn) {
		if (message.length < 7) {
			// All of our messages are length 7
			return false;
		}
		message[6] ^= SEED;
		message[6] = Integer.rotateRight(message[6], ROTATIONS[6]);
		message[6] -= SEED;

		if ((message[6] & SIGNATURE_MASK) != SIGNATURE) {
			return false;
		}
		message[6] &= ROUND_MASK;
		if(Math.abs(turn - message[6]) > 2) return false;

		int key = pow(SEED, message[6]);
		for(int i = 0; i < 6; i++) {
			message[i] ^= key;
			message[i] = Integer.rotateRight(message[i], ROTATIONS[i]);
			message[i] -= key;
		}
		return true;
	}

	/**
	 * Adds a signature to the 7th element, and encrypts it.
	 * @param message
	 */
	public static void encryptMessage(int[] message) {
		int key = pow(SEED, message[6]);
		message[6] &= ROUND_MASK;
		message[6] |= SIGNATURE;
		for(int i = 0; i < 6; i++) {
			message[i] += key;
			message[i] = Integer.rotateLeft(message[i], ROTATIONS[i]);
			message[i] ^= key;
		}
		message[6] += SEED;
		message[6] = Integer.rotateLeft(message[6], ROTATIONS[6]);
		message[6] ^= SEED;
	}

	private static int pow(int x, int p) {
		int ans = 1;
		while (p > 0) {
			if (p % 2 == 1) {
				ans = (ans * x);
			}
			p /= 2;
			x = x * x;
		}
		return ans;
	}
}
