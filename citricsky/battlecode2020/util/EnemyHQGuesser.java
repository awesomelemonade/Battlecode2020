package citricsky.battlecode2020.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class EnemyHQGuesser {
	// Lots of hardcoded stuff in here
	private static RobotController controller;
	private static MapLocation[] guesses;

	// how many scenarios?
	// mode = 0: know #1 is not hq
	// mode = 1: know #2 is not hq
	// mode = 2: know #3 is not hq
	// mode = 3: don't know anything
	private static int mode;

	public static final int UNKNOWN_MODE = 3;

	public static void init(RobotController controller) {
		EnemyHQGuesser.controller = controller;
		EnemyHQGuesser.mode = UNKNOWN_MODE;
	}

	/**
	 * Input: Our HQ Location. Creates guesses based on our hq location and symmetry
	 */
	public static void setGuesses(int x, int y) {
		guesses = new MapLocation[3];
		int width = controller.getMapWidth();
		int height = controller.getMapHeight();
		int a = width - x - 1;
		int b = height - y - 1;
		// Symmetric around x axis
		guesses[0] = new MapLocation(x, b);
		// Symmetric around y axis
		guesses[1] = new MapLocation(a, y);
		// Rotationally Symmetric (180 degrees)
		// Equivalent to symmetric on both axes
		guesses[2] = new MapLocation(a, b);
	}
	public static void loop() {
		if (guesses == null) {
			// We haven't received our hq coordinates
			return;
		}
		// Let's check if any guesses can be sensed
		for (int i = guesses.length; --i >= 0;) {
			if (mode == i) {
				continue;
			}
			MapLocation guess = guesses[i];
			if (controller.canSenseLocation(guess)) {
				// HQ is not at this location, or else we would've sensed it in SharedInfo
				if (markUnseen(i)) {
					return;
				}
			}
		}
	}
	public static MapLocation getRandomEnemyHQGuess() {
		if (guesses == null) {
			return null;
		}
		return guesses[EnemyHQGuesser.getRandomGuessIndex()];
	}
	public static MapLocation getEnemyHQGuess(int index) {
		if (index < 0) {
			return null;
		}
		return guesses[index];
	}
	public static boolean markUnseen(int index) {
		if (mode == UNKNOWN_MODE) {
			mode = index;
			SharedInfo.sendEnemyGuessMode(mode);
			return false;
		} else {
			int other = getOtherMode(mode, index);
			// We can deduce the HQ location is at guesses[other]
			SharedInfo.sendEnemyHQ(guesses[other]);
			return true;
		}
	}
	private static int getOtherMode(int a, int b) {
		// Not very friendly looking code :(
		// Bytecode friendly tho :)
		switch(a) {
			case 0:
				switch(b) {
					case 1:
						return 2;
					case 2:
						return 1;
					default:
						throw new IllegalArgumentException("Unknown");
				}
			case 1:
				switch(b) {
					case 0:
						return 2;
					case 2:
						return 0;
					default:
						throw new IllegalArgumentException("Unknown");
				}
			case 2:
				switch(b) {
					case 0:
						return 1;
					case 1:
						return 0;
					default:
						throw new IllegalArgumentException("Unknown");
				}
			default:
				throw new IllegalArgumentException("Unknown");
		}
	}
	public static int getRandomGuessIndex() {
		// Yay premature optimization
		switch(mode) {
			case 0:
				if (Math.random() < 0.5) {
					return 1;
				} else {
					return 2;
				}
			case 1:
				if (Math.random() < 0.5) {
					return 0;
				} else {
					return 2;
				}
			case 2:
				if (Math.random() < 0.5) {
					return 0;
				} else {
					return 1;
				}
			case UNKNOWN_MODE:
				return (int) (Math.random() * 3);
			default:
				return -1;
		}
	}
	public static void setMode(int mode) {
		EnemyHQGuesser.mode = mode;
	}
	public static int getMode() {
		return mode;
	}
}
