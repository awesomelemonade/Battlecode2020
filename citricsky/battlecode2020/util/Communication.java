package citricsky.battlecode2020.util;

import battlecode.common.RobotController;
import battlecode.common.Team;

public class Communication {
	private static int SEED = 51178234;

	public static void init(RobotController controller) {
		// xor the seed by map size
		int gameAdjust = (controller.getMapWidth() << 6) | controller.getMapHeight();
		gameAdjust = (gameAdjust << 1) | ((controller.getTeam() == Team.A) ? 0 : 1);
		SEED ^= gameAdjust;
		// Also preloads static variables
	}
	public static boolean verifyMessage(int[] message) {
		int signature = message[6];
		if (Hash.hash(SEED, message[5]) != signature) {
			return false;
		}
		boolean verified = false; // ret = !mayContains
		for (int i = BLOOM_FILTER_SEEDS.length; --i >= 0;) {
			int index = Math.abs(Hash.hash(BLOOM_FILTER_SEEDS[i], signature) % BLOOM_FILTER_SIZE);
			if (!BLOOM_FILTER.get(index)) {
				BLOOM_FILTER.set(index);
				verified = true;
			}
		}
		return verified;
	}
	private static final int RANDOM_MASK = 0b11111111111111111111000000000000;
	public static void encryptMessage(int[] message) {
		int footer = Util.getRandom().nextInt() % RANDOM_MASK ^ Cache.controller.getRoundNum();
		message[5] = footer; // footer
		message[6] = Hash.hash(SEED, footer); // signature
	}
	// Bloom filter
	private static final int BLOOM_FILTER_SIZE = 4096 * 64;
	private static final int BITS_PER_LONG = 64;
	private static final BooleanArray BLOOM_FILTER = new BooleanArray(BLOOM_FILTER_SIZE / BITS_PER_LONG);
	private static final int[] BLOOM_FILTER_SEEDS = new int[] {
			707946, 784205, 847237
	}; // 3 hash functions
}
