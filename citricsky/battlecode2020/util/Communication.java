package citricsky.battlecode2020.util;

import battlecode.common.RobotController;
import battlecode.common.Team;

public class Communication {
	public static final int VERIFY_STATE_SUCCESS = 0;
	public static final int VERIFY_STATE_UNKNOWN_HASH = 1;
	public static final int VERIFY_STATE_REPLAY_ATTACK = 2;
	private static int SEED = 20851893;

	public static void init(RobotController controller) {
		// xor the seed by map size
		int gameAdjust = (Cache.MAP_WIDTH << 6) | Cache.MAP_HEIGHT;
		gameAdjust = (gameAdjust << 1) | ((controller.getTeam() == Team.A) ? 0 : 1);
		SEED ^= gameAdjust;
		// Also preloads static variables
	}
	public static int verifyMessage(int[] message) {
		int signature = message[6];
		if (Hash.hash(SEED, message[5]) != signature) {
			return VERIFY_STATE_UNKNOWN_HASH;
		}
		boolean verified = false; // ret = !mayContains
		for (int i = BLOOM_FILTER_SEEDS.length; --i >= 0;) {
			int index = Math.abs(Hash.hash(BLOOM_FILTER_SEEDS[i], signature) % BLOOM_FILTER_SIZE);
			if (!BLOOM_FILTER.get(index)) {
				BLOOM_FILTER.set(index);
				verified = true;
			}
		}
		return verified ? VERIFY_STATE_SUCCESS : VERIFY_STATE_REPLAY_ATTACK;
	}
	private static final int RANDOM_MASK = 0b11111111111111111111000000000000;
	public static void encryptMessage(int[] message) {
		int footer = (Util.getRandom().nextInt() & RANDOM_MASK) ^ Cache.controller.getRoundNum();
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
