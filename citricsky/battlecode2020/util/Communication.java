package citricsky.battlecode2020.util;

import battlecode.common.RobotController;
import battlecode.common.Team;

public class Communication {
	private static int seed = 154820275;

	private static final int UPPER_BITMASK = 0b11111111000000000000000000000000;
	private static final int LOWER_BITMASK = 0b00000000111111111111111111111111;
	private static final int[] PRIMES = new int[] {5113, 15919, 18671, 42101, 61441, 78571, 101531};

	public static void init(RobotController controller) {
		// xor the seed by map size
		int gameAdjust = (controller.getMapWidth() << 6) | controller.getMapHeight();
		gameAdjust = (gameAdjust << 1) | ((controller.getTeam() == Team.A) ? 0 : 1);
		seed ^= gameAdjust;
		// Also preloads static variables (~5k bytecodes for bloom filter)
	}
	/**
	 * Each transaction can be only verified once
	 * @param message
	 * @return whether the transaction is verified to be ours
	 */
	public static boolean verifyTransaction(int[] message) {
		if (message.length < 7) {
			// All of our messages are length 7
			return false;
		}
		int verify = message[6] & LOWER_BITMASK;
		message[6] ^= verify; // Zeros out LOWER_BITMASK
		int hash = hashArray(message) & LOWER_BITMASK;
		message[6] |= verify;
		return hash == verify && (!mayContainsAndAdd(message[6]));
	}

	/**
	 * Creates a signature, setting the 7th element in the message
	 * @param message
	 */
	public static void hashTransaction(int[] message) {
		// Generate 8 random bits
		message[6] = Util.randomInt() & UPPER_BITMASK;
		message[6] |= hashArray(message) & LOWER_BITMASK;
	}
	private static int hashArray(int[] message) {
		int hash = 1;
		hash = hash * PRIMES[0] + message[0];
		hash = hash * PRIMES[1] + message[1];
		hash = hash * PRIMES[2] + message[2];
		hash = hash * PRIMES[3] + message[3];
		hash = hash * PRIMES[4] + message[4];
		hash = hash * PRIMES[5] + message[5];
		hash = hash * PRIMES[6] + message[6];
		return Hash.hash(seed, hash);
	}

	private static final int BLOOM_FILTER_SIZE = 4096 * 64;
	private static final int BITS_PER_LONG = 64;
	private static final BooleanArray BLOOM_FILTER = new BooleanArray(BLOOM_FILTER_SIZE / BITS_PER_LONG);
	private static final int[] BLOOM_FILTER_SEEDS = new int[] {
			509485, 302772, 154030, 364385, 958342, 180307, 368404, 200874
	}; // 8 hash functions

	/**
	 * Returns whether x is in the bloom filter
	 * If it isn't, x will be added to the bloom filter
	 * @param x
	 * @return whether x is in the bloom filter
	 */
	private static boolean mayContainsAndAdd(int x) {
		boolean mayContains = true;
		for (int i = BLOOM_FILTER_SEEDS.length; --i >= 0;) {
			int index = Math.abs(Hash.hash(BLOOM_FILTER_SEEDS[i], x) % BLOOM_FILTER_SIZE);
			if (!BLOOM_FILTER.get(index)) {
				BLOOM_FILTER.set(index);
				mayContains = false;
			}
		}
		return mayContains;
	}
}
