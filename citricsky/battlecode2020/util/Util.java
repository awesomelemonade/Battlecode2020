package citricsky.battlecode2020.util;

import battlecode.common.*;
import citricsky.battlecode2020.HQBot;

import java.util.Random;

public class Util {
	private static RobotController controller;
	private static Random random;
	public static void init(RobotController controller) {
		Util.controller = controller;
		random = new Random(Hash.hash(1532135, controller.getID()));
		Cache.init(controller);
		SharedInfo.init(controller);
		Communication.init(controller); // 5k bytecodes
		CommunicationProcessor.init(controller);
		CommunicationAttacks.init();
		if (!Cache.ROBOT_TYPE.isBuilding()) {
			Pathfinding.init(controller);
			UnitsMap.init(controller);
		}
	}
	public static boolean hasLattice = false; // Estimation of whether we have a lattice
	private static int designSchoolSpawnedTurn = -1; // Helps with hasLattice
	public static void loop() throws GameActionException {
		Cache.loop();
		CommunicationProcessor.processAll();
		if (SharedInfo.getEnemyHQLocation() == null) {
			for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
				if (enemy.getType() == RobotType.HQ) {
					SharedInfo.sendEnemyHQ(enemy.getLocation());
					return;
				}
			}
			EnemyHQGuesser.loop();
		}
		if (!Cache.ROBOT_TYPE.isBuilding()) {
			UnitsMap.loop();
		}
		MapTracker.checkValidityOfWaterLocations();
		if (!hasLattice) {
			if (SharedInfo.getDesignSchoolCount() > 0) {
				if (designSchoolSpawnedTurn == -1) {
					designSchoolSpawnedTurn = controller.getRoundNum();
				} else {
					hasLattice = controller.getRoundNum() - designSchoolSpawnedTurn > 100;
				}
			}
		}
	}
	public static void postLoop() throws GameActionException {
		CommunicationProcessor.sendAll();
	}
	public static Random getRandom() {
		return random;
	}
	public static final int ADJACENT_DISTANCE_SQUARED = 2;
	public static final Direction[] ALL_DIRECTIONS = Direction.values();
	public static final Direction[] ADJACENT_DIRECTIONS = new Direction[] {
			Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
	};
	public static final Direction[] CARDINAL_DIRECTIONS = new Direction[] {
			Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
	};
	public static final int[] FLOOD_FILL_DX = {0, -1, 0, 0, 1, -1, -1, 1, 1, -2, 0, 0, 2, -2, -2, -1, -1, 1, 1, 2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -3, -3, -1, -1, 1, 1, 3, 3, -3, -3, -2, -2, 2, 2, 3, 3, -4, 0, 0, 4, -4, -4, -1, -1, 1, 1, 4, 4, -3, -3, 3, 3, -4, -4, -2, -2, 2, 2, 4, 4, -5, -4, -4, -3, -3, 0, 0, 3, 3, 4, 4, 5, -5, -5, -1, -1, 1, 1, 5, 5, -5, -5, -2, -2, 2, 2, 5, 5, -4, -4, 4, 4, -5, -5, -3, -3, 3, 3, 5, 5, -6, 0, 0, 6, -6, -6, -1, -1, 1, 1, 6, 6, -6, -6, -2, -2, 2, 2, 6, 6, -5, -5, -4, -4, 4, 4, 5, 5, -6, -6, -3, -3, 3, 3, 6, 6};
	public static final int[] FLOOD_FILL_DY = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -2, 2, 0, -1, 1, -2, 2, -2, 2, -1, 1, -2, 2, -2, 2, 0, -3, 3, 0, -1, 1, -3, 3, -3, 3, -1, 1, -2, 2, -3, 3, -3, 3, -2, 2, 0, -4, 4, 0, -1, 1, -4, 4, -4, 4, -1, 1, -3, 3, -3, 3, -2, 2, -4, 4, -4, 4, -2, 2, 0, -3, 3, -4, 4, -5, 5, -4, 4, -3, 3, 0, -1, 1, -5, 5, -5, 5, -1, 1, -2, 2, -5, 5, -5, 5, -2, 2, -4, 4, -4, 4, -3, 3, -5, 5, -5, 5, -3, 3, 0, -6, 6, 0, -1, 1, -6, 6, -6, 6, -1, 1, -2, 2, -6, 6, -6, 6, -2, 2, -4, 4, -5, 5, -5, 5, -4, 4, -3, 3, -6, 6, -6, 6, -3, 3};
	public static final int[] FLOOD_FILL_DX_CLAMPED = {0, -1, 0, 0, 1, -1, -1, 1, 1, -1, 0, 0, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1, -1, 0, 0, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, 0, 0, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, -1, 0, 0, 1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, 0, 0, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1, -1, -1, -1, -1, 1, 1, 1, 1};
	public static final int[] FLOOD_FILL_DY_CLAMPED = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, 0, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, 0, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, 0, -1, 1, 0, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1};
	private static final Direction[][] ATTEMPT_ORDER = {
			{Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.WEST, Direction.EAST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.SOUTH},
			{Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.WEST, Direction.SOUTH, Direction.SOUTHWEST},
			{Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTH, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.WEST},
			{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.NORTH, Direction.WEST, Direction.NORTHWEST},
			{Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.NORTH},
			{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.SOUTHEAST, Direction.NORTHWEST, Direction.EAST, Direction.NORTH, Direction.NORTHEAST},
			{Direction.WEST, Direction.SOUTHWEST, Direction.NORTHWEST, Direction.SOUTH, Direction.NORTH, Direction.SOUTHEAST, Direction.NORTHEAST, Direction.EAST},
			{Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.SOUTH, Direction.EAST, Direction.SOUTHEAST}
	};
	public static Direction[] getAttemptOrder(Direction direction) {
		return ATTEMPT_ORDER[direction.ordinal()];
	}
	public static Direction getDirection(int dx, int dy) {
		switch(dx) {
			case -1:
				switch(dy) {
					case -1:
						return Direction.SOUTHWEST;
					case 0:
						return Direction.WEST;
					case 1:
						return Direction.NORTHWEST;
				}
			case 0:
				switch(dy) {
					case -1:
						return Direction.SOUTH;
					case 0:
						return Direction.CENTER;
					case 1:
						return Direction.NORTH;
				}
			case 1:
				switch(dy) {
					case -1:
						return Direction.SOUTHEAST;
					case 0:
						return Direction.EAST;
					case 1:
						return Direction.NORTHEAST;
				}
		}
		return Direction.CENTER;
	}
	public static Direction randomAdjacentDirection() {
		return ADJACENT_DIRECTIONS[random.nextInt(8)];
	}

	public static boolean onTheMap(MapLocation location) {
		return controller.onTheMap(location);
	}
	/**
	 * Whether the given direction is flooding
	 */
	public static boolean isFlooding(Direction direction) throws GameActionException {
		return isFlooding(Cache.CURRENT_LOCATION.add(direction));
	}
	public static boolean isFlooding(MapLocation location) throws GameActionException {
		return !controller.canSenseLocation(location) || controller.senseFlooding(location);
	}
	public static boolean isBlocked(MapLocation location) throws GameActionException {
		return !onTheMap(location) || UnitsMap.hasBlockingUnit(location) ||
				(Cache.ROBOT_TYPE != RobotType.DELIVERY_DRONE && isFlooding(location));
	}
	public static boolean canSafeMove(Direction direction) throws GameActionException {
		return controller.canMove(direction) && (Cache.ROBOT_TYPE == RobotType.DELIVERY_DRONE || (!isFlooding(direction)));
	}
	public static boolean canSafeBuildRobot(RobotType type, Direction direction) throws GameActionException {
		return controller.canBuildRobot(type, direction) && (type == RobotType.DELIVERY_DRONE || (!isFlooding(direction)));
	}
	public static boolean trySafeBuildTowardsEnemyHQ(RobotType type) throws GameActionException {
		MapLocation location = SharedInfo.getEnemyHQLocation();
		if (location == null) {
			location = Cache.MAP_CENTER_LOCATION;
		}
		return trySafeBuildTowards(type, location);
	}
	public static boolean trySafeBuildTowards(RobotType type, MapLocation location) throws GameActionException {
		for (Direction direction : Util.getAttemptOrder(Cache.CURRENT_LOCATION.directionTo(location))) {
			if (Util.canSafeBuildRobot(type, direction)) {
				controller.buildRobot(type, direction);
				return true;
			}
		}
		return false;
	}
	private static Direction lastRandomDirection;
	public static boolean randomWalk() throws GameActionException {
		if (lastRandomDirection == null) {
			lastRandomDirection = Util.randomAdjacentDirection();
		}
		boolean success = false;
		for (int i = 0; i < 16 && (!(success = Pathfinding.naiveMove(lastRandomDirection))); i++) {
			lastRandomDirection = Util.randomAdjacentDirection();
		}
		return success;
	}
	private static MapLocation lastExploreTarget;
	private static int lastExploreHQIndex;
	public static boolean randomExplore() throws GameActionException {
		if (SharedInfo.getEnemyHQLocation() != null) {
			return Util.randomWalk();
		}
		// Let's try to find the HQ
		if (lastExploreTarget == null || controller.canSenseLocation(lastExploreTarget) || SharedInfo.getEnemyHQGuesserMode() == lastExploreHQIndex) {
			lastExploreHQIndex = EnemyHQGuesser.getRandomGuessIndex();
			lastExploreTarget = EnemyHQGuesser.getEnemyHQGuess(lastExploreHQIndex);
		}
		if (lastExploreTarget == null) {
			controller.setIndicatorDot(lastExploreTarget, 255, 128, 0);
			return Util.randomWalk();
		}
		// Go towards exploreTarget
		Pathfinding.execute(lastExploreTarget);
		return true;
	}
	public static void tryKiteAwayFrom(MapLocation location, boolean pathfind) throws GameActionException {
		controller.setIndicatorDot(Cache.CURRENT_LOCATION, 255, 255, 0);
		Direction awayDirection = location.directionTo(Cache.CURRENT_LOCATION);
		if (pathfind) {
			Pathfinding.execute(Cache.CURRENT_LOCATION.add(awayDirection).add(awayDirection)
					.add(awayDirection).add(awayDirection).add(awayDirection));
		} else {
			for (Direction direction : Util.getAttemptOrder(awayDirection)) {
				if (controller.canMove(direction)) {
					controller.move(direction);
				}
			}
		}
	}
	public static final int[] TURNS_TO_FLOODED = {
			0, 256, 464, 677, 931, 1210, 1413, 1546, 1640, 1713, 1771, 1819, 1861, 1897, 1929, 1957, 1983, 2007, 2028,
			2048, 2067, 2084, 2100, 2115, 2129, 2143, 2155, 2168, 2179, 2190, 2201, 2211, 2220, 2230, 2239, 2247, 2256,
			2264, 2271, 2279, 2286, 2293, 2300, 2307, 2313, 2319, 2325, 2331, 2337, 2343, 2348, 2353, 2359, 2364, 2369,
			2374, 2379, 2383, 2388, 2392, 2397, 2401, 2405, 2409, 2413, 2417, 2421, 2425, 2429, 2433, 2436, 2440, 2443,
			2447, 2450, 2454, 2457, 2460, 2464, 2467, 2470, 2473, 2476, 2479, 2482, 2485, 2488, 2491, 2493, 2496, 2499,
			2502, 2504, 2507, 2509, 2512, 2514, 2517, 2519, 2522, 2524, 2527, 2529, 2531, 2534, 2536, 2538, 2540, 2543,
			2545, 2547, 2549, 2551, 2553, 2555, 2557, 2559, 2561, 2563, 2565, 2567, 2569, 2571, 2573, 2575, 2577, 2579,
			2581, 2582, 2584, 2586, 2588, 2590, 2591, 2593, 2595, 2596, 2598, 2600, 2601, 2603, 2605, 2606, 2608, 2610,
			2611, 2613, 2614, 2616, 2617, 2619, 2620, 2622, 2623, 2625, 2626, 2628, 2629, 2631, 2632, 2633, 2635, 2636,
			2638, 2639, 2640, 2642, 2643, 2644, 2646, 2647, 2648, 2650, 2651, 2652, 2654, 2655, 2656, 2657, 2659, 2660,
			2661, 2662, 2664, 2665, 2666, 2667, 2668, 2670, 2671, 2672, 2673, 2674, 2675, 2677, 2678, 2679, 2680, 2681,
			2682, 2683, 2684, 2685, 2687, 2688, 2689, 2690, 2691, 2692, 2693, 2694, 2695, 2696, 2697, 2698, 2699, 2700,
			2701, 2702, 2703, 2704, 2705, 2706, 2707, 2708, 2709, 2710, 2711, 2712, 2713, 2714, 2715, 2716, 2717, 2718,
			2719, 2719, 2720, 2721, 2722, 2723, 2724, 2725, 2726, 2727, 2728, 2728, 2729, 2730, 2731, 2732, 2733, 2734,
			2735, 2735, 2736, 2737, 2738, 2739, 2740, 2740, 2741, 2742, 2743, 2744, 2744, 2745, 2746, 2747, 2748, 2749,
			2749, 2750, 2751, 2752, 2752, 2753, 2754, 2755, 2756, 2756, 2757, 2758, 2759, 2759, 2760, 2761, 2762, 2762,
			2763, 2764, 2765, 2765, 2766, 2767, 2767, 2768, 2769, 2770, 2770, 2771, 2772, 2772, 2773, 2774, 2775, 2775,
			2776, 2777, 2777, 2778, 2779, 2779, 2780, 2781, 2781, 2782, 2783, 2783, 2784, 2785, 2785, 2786, 2787, 2787,
			2788, 2789, 2789, 2790, 2791, 2791, 2792, 2793, 2793, 2794, 2794, 2795, 2796, 2796, 2797, 2798, 2798, 2799,
			2799, 2800, 2801, 2801, 2802, 2803, 2803, 2804, 2804, 2805, 2806, 2806, 2807, 2807, 2808, 2808, 2809, 2810,
			2810, 2811, 2811, 2812, 2813, 2813, 2814, 2814, 2815, 2815, 2816, 2817, 2817, 2818, 2818, 2819, 2819, 2820,
			2820, 2821, 2822, 2822, 2823, 2823, 2824, 2824, 2825, 2825, 2826, 2826, 2827, 2828, 2828, 2829, 2829, 2830,
			2830, 2831, 2831, 2832, 2832, 2833, 2833, 2834, 2834, 2835, 2835, 2836, 2836, 2837, 2837, 2838, 2838, 2839,
			2839, 2840, 2840, 2841, 2841, 2842, 2842, 2843, 2843, 2844, 2844, 2845, 2845, 2846, 2846, 2847, 2847, 2848,
			2848, 2849, 2849, 2850, 2850, 2851, 2851, 2852, 2852, 2853, 2853, 2853, 2854, 2854, 2855, 2855, 2856, 2856,
			2857, 2857, 2858, 2858, 2859, 2859, 2859, 2860, 2860, 2861, 2861, 2862, 2862, 2863, 2863, 2864, 2864, 2864,
			2865, 2865, 2866, 2866, 2867, 2867, 2867, 2868, 2868, 2869, 2869, 2870, 2870, 2871, 2871, 2871, 2872, 2872,
			2873, 2873, 2873, 2874, 2874, 2875, 2875, 2876, 2876, 2876, 2877, 2877, 2878, 2878, 2879, 2879, 2879, 2880,
			2880, 2881, 2881, 2881, 2882, 2882, 2883, 2883, 2883, 2884, 2884, 2885, 2885, 2885, 2886, 2886, 2887, 2887,
			2887, 2888, 2888, 2889, 2889, 2889, 2890, 2890, 2890, 2891, 2891, 2892, 2892, 2892, 2893, 2893, 2894, 2894,
			2894, 2895, 2895, 2895, 2896, 2896, 2897, 2897, 2897, 2898, 2898, 2898, 2899, 2899, 2900, 2900, 2900, 2901,
			2901, 2901, 2902, 2902, 2903, 2903, 2903, 2904, 2904, 2904, 2905, 2905, 2905, 2906, 2906, 2907, 2907, 2907,
			2908, 2908, 2908, 2909, 2909, 2909, 2910, 2910, 2910, 2911, 2911, 2911, 2912, 2912, 2912, 2913, 2913, 2914,
			2914, 2914, 2915, 2915, 2915, 2916, 2916, 2916, 2917, 2917, 2917, 2918, 2918, 2918, 2919, 2919, 2919, 2920,
			2920, 2920, 2921, 2921, 2921, 2922, 2922, 2922, 2923, 2923, 2923, 2924, 2924, 2924, 2925, 2925, 2925, 2926,
			2926, 2926, 2927, 2927, 2927, 2928, 2928, 2928, 2928, 2929, 2929, 2929, 2930, 2930, 2930, 2931, 2931, 2931,
			2932, 2932, 2932, 2933, 2933, 2933, 2934, 2934, 2934, 2934, 2935, 2935, 2935, 2936, 2936, 2936, 2937, 2937,
			2937, 2938, 2938, 2938, 2938, 2939, 2939, 2939, 2940, 2940, 2940, 2941, 2941, 2941, 2941, 2942, 2942, 2942,
			2943, 2943, 2943, 2944, 2944, 2944, 2944, 2945, 2945, 2945, 2946, 2946, 2946, 2946, 2947, 2947, 2947, 2948,
			2948, 2948, 2949, 2949, 2949, 2949, 2950, 2950, 2950, 2951, 2951, 2951, 2951, 2952, 2952, 2952, 2953, 2953,
			2953, 2953, 2954, 2954, 2954, 2955, 2955, 2955, 2955, 2956, 2956, 2956, 2956, 2957, 2957, 2957, 2958, 2958,
			2958, 2958, 2959, 2959, 2959, 2959, 2960, 2960, 2960, 2961, 2961, 2961, 2961, 2962, 2962, 2962, 2962, 2963,
			2963, 2963, 2964, 2964, 2964, 2964, 2965, 2965, 2965, 2965, 2966, 2966, 2966, 2966, 2967, 2967, 2967, 2968,
			2968, 2968, 2968, 2969, 2969, 2969, 2969, 2970, 2970, 2970, 2970, 2971, 2971, 2971, 2971, 2972, 2972, 2972,
			2972, 2973, 2973, 2973, 2973, 2974, 2974, 2974, 2974, 2975, 2975, 2975, 2975, 2976, 2976, 2976, 2976, 2977,
			2977, 2977, 2977, 2978, 2978, 2978, 2978, 2979, 2979, 2979, 2979, 2980, 2980, 2980, 2980, 2981, 2981, 2981,
			2981, 2982, 2982, 2982, 2982, 2983, 2983, 2983, 2983, 2984, 2984, 2984, 2984, 2985, 2985, 2985, 2985, 2986,
			2986, 2986, 2986, 2987, 2987, 2987, 2987, 2987, 2988, 2988, 2988, 2988, 2989, 2989, 2989, 2989, 2990, 2990,
			2990, 2990, 2991, 2991, 2991, 2991, 2991, 2992, 2992, 2992, 2992, 2993, 2993, 2993, 2993, 2994, 2994, 2994,
			2994, 2994, 2995, 2995, 2995, 2995, 2996, 2996, 2996, 2996, 2997, 2997, 2997, 2997, 2997, 2998, 2998, 2998,
			2998, 2999, 2999, 2999, 2999, 2999, 3000, 3000, 3000, 3000};
	public static int getTurnsToFlooded(int elevation) {
		if (elevation < 0) {
			return 0;
		}
		if (elevation >= TURNS_TO_FLOODED.length) {
			return TURNS_TO_FLOODED[TURNS_TO_FLOODED.length - 1];
		}
		return TURNS_TO_FLOODED[elevation];
	}

	public static boolean isAdjacentToFlooding(MapLocation location) throws GameActionException {
		for (int i = Util.ADJACENT_DIRECTIONS.length; --i >= 0;) {
			MapLocation adjacent = location.add(Util.ADJACENT_DIRECTIONS[i]);
			if (controller.onTheMap(adjacent) && (!controller.canSenseLocation(adjacent) || controller.senseFlooding(adjacent))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFloodingOrAdjacentToFlooding(MapLocation center) throws GameActionException {
		for (int i = Util.ALL_DIRECTIONS.length; --i >= 0;) {
			MapLocation location = center.add(Util.ALL_DIRECTIONS[i]);
			if (controller.onTheMap(location) && (!controller.canSenseLocation(location) || controller.senseFlooding(location))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the location will be flooded next turn
	 * Returns true if location cannot be sensed
	 */
	public static boolean isGoingToBeFloodedNextTurn(MapLocation location) throws GameActionException {
		if (controller.canSenseLocation(location)) {
			int elevation = controller.senseElevation(location);
			if (canPotentiallyBeFlooded(elevation)) {
				// Check if there is water in the surroundings
				return isAdjacentToFlooding(location);
			} else {
				// Elevation is high enough
				return false;
			}
		} else {
			// Assume it will be flooded if you can't see it
			return true;
		}
	}
	public static boolean canPotentiallyBeFlooded(int elevation) {
		return controller.getRoundNum() + 1 >= getTurnsToFlooded(elevation);
	}
	public static boolean tryShootDrone() throws GameActionException {
		RobotInfo[] enemies = controller.senseNearbyRobots(Cache.CURRENT_LOCATION,
				GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, controller.getTeam().opponent());
		int bestDistance = 1738;
		RobotInfo bestTarget = null;
		for (RobotInfo enemy : enemies) {
			int enemyID = enemy.getID();
			int enemyDistance = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
			if(enemyDistance < bestDistance) {
				if (controller.canShootUnit(enemyID)) {
					bestDistance = enemyDistance;
					bestTarget = enemy;
				}
			}
		}
		if(bestTarget != null) {
			controller.shootUnit(bestTarget.getID());
			return true;
		}
		return false;
	}
	public static boolean tryKiteFromAdjacentDrones() throws GameActionException {
		//more efficient to search Cache if not a lot nearby
		if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 8) {
			for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
				//if the enemy is a drone and adjacent
				if (enemy.getType() == RobotType.DELIVERY_DRONE) {
					if (Cache.CURRENT_LOCATION.isAdjacentTo(enemy.getLocation())) {
						Util.tryKiteAwayFrom(enemy.getLocation(), true);
						return true;
					}
				}
			}
		} else { //otherwise more efficient to check adjacent tiles
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation adjacentLocation = Cache.CURRENT_LOCATION.add(direction);
				if (Util.onTheMap(adjacentLocation) && controller.canSenseLocation(adjacentLocation)) {
					//if an enemy drone exists in adjacent tile
					RobotInfo robot = controller.senseRobotAtLocation(adjacentLocation);
					if (robot != null && robot.getType() == RobotType.DELIVERY_DRONE && robot.getTeam() == Cache.OPPONENT_TEAM) {
						Util.tryKiteAwayFrom(robot.getLocation(), true);
						return true;
					}
				}
			}
		}
		return false;
	}
	private static final Direction[][] DIRECTIONS_TO = new Direction[][] {
			{Direction.CENTER},
			{Direction.NORTH},
			{Direction.NORTHEAST, Direction.NORTH, Direction.EAST},
			{Direction.EAST},
			{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH},
			{Direction.SOUTH},
			{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST},
			{Direction.WEST},
			{Direction.NORTHWEST, Direction.WEST, Direction.NORTH}
	};
	public static Direction[] directionsTo(MapLocation a, MapLocation b) {
		int dx = b.x - a.x;
		int dy = b.y - a.y;
		if (dy < 0) {
			if (dx < 0) {
				// SOUTHWEST
				return DIRECTIONS_TO[6];
			} else if (dx == 0) {
				// SOUTH
				return DIRECTIONS_TO[5];
			} else {
				// SOUTHEAST
				return DIRECTIONS_TO[4];
			}
		} else if (dy == 0) {
			if (dx < 0) {
				// WEST
				return DIRECTIONS_TO[7];
			} else if (dx == 0) {
				// CENTER
				return DIRECTIONS_TO[0];
			} else {
				// EAST
				return DIRECTIONS_TO[3];
			}
		} else {
			if (dx < 0) {
				// NORTHWEST
				return DIRECTIONS_TO[8];
			} else if (dx == 0) {
				// NORTH
				return DIRECTIONS_TO[1];
			} else {
				// NORTHEAST
				return DIRECTIONS_TO[2];
			}
		}
	}
	public static boolean isNearCorner(MapLocation location) {
		return (location.x <= 1 || location.x >= Cache.MAP_WIDTH - 2) &&
				(location.y <= 1 || location.y >= Cache.MAP_HEIGHT - 2);
	}
	public static boolean isInCorner(MapLocation location) {
		return (location.x == 0 || location.x == Cache.MAP_WIDTH - 1) &&
				(location.y == 0 || location.y == Cache.MAP_HEIGHT - 1);
	}
	public static Direction directionToNearestCorner(MapLocation location) {
		return location.directionTo(
				new MapLocation((location.x / Cache.MAP_CENTER_LOCATION.x) * Cache.MAP_WIDTH,
						(location.y / Cache.MAP_CENTER_LOCATION.y) * Cache.MAP_HEIGHT));
	}
}
