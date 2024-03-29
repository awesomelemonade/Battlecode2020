package citricsky.battlecode2020.util;

import battlecode.common.*;
import citricsky.battlecode2020.LandscaperBot;
import citricsky.battlecode2020.MinerBot;

import java.util.Arrays;
import java.util.Comparator;

public class Pathfinding {
	public static boolean ignoreNetGuns = false;
	private static RobotController controller;
	private static FastIntCounter2D visitedSet;
	private static MapLocation lastTarget;
	// max 8 directions, heap ignores first element
	// min heap of counters
	// private static Direction[] directionsHeap = new Direction[9];
	// private static int[] countersHeap = new int[9];

	public static void init(RobotController controller) {
		Pathfinding.controller = controller;
		visitedSet = new FastIntCounter2D(Cache.MAP_WIDTH, Cache.MAP_HEIGHT);
	}
	public static int getTurnsSpentSoFar() {
		return visitedSet.getCounter();
	}
	public static void execute(MapLocation target) throws GameActionException {
		if (lastTarget == null || !lastTarget.equals(target)) {
			lastTarget = target;
			visitedSet.reset();
		}
		if (Cache.ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			visitedSet.updateBaseTrail(5); // Drones only care about the last 5 visited tiles
		}
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		controller.setIndicatorLine(currentLocation, target, 0, 255, 0);
		if (!controller.isReady()) {
			return;
		}
		if (currentLocation.equals(target)) {
			// We're already there
			return;
		}
		visitedSet.add(currentLocation.x, currentLocation.y);
		Direction idealDirection = currentLocation.directionTo(target);
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = currentLocation.add(direction);
			if (!Util.onTheMap(location)) {
				continue;
			}
			if (visitedSet.contains(location.x, location.y)) {
				continue;
			}
			if (naiveMove(direction)) {
				return;
			}
		}
		// We stuck bois - let's look for the lowest non-negative
		Direction[] directions = Util.getAttemptOrder(idealDirection);
		int[] counters = new int[8];
		Integer[] indices = new Integer[8];
		for (int i = counters.length; --i >= 0;) {
			MapLocation location = currentLocation.add(directions[i]);
			if (Util.onTheMap(location)) {
				counters[i] = visitedSet.get(location.x, location.y);
			} else {
				counters[i] = Integer.MAX_VALUE;
			}
			indices[i] = i;
		}
		Arrays.sort(indices, Comparator.comparingInt(i -> counters[i]));
		for (int i = 0; i < indices.length; i++) {
			if (naiveMove(directions[indices[i]])) {
				return;
			}
		}
		// should never happen
		/*int size = 0;
		for (Direction direction : Util.getAttemptOrder(idealDirection)) {
			MapLocation location = currentLocation.add(direction);
			if (!Util.onTheMap(location)) {
				continue;
			}
			directionsHeap[++size] = direction;
			countersHeap[size] = visitedSet.get(location.x, location.y);
			if (naiveMove(direction)) {
				return;
			}
		}
		// Build heap
		for (int i = size / 2; --i >= 0;) {
			// percolate down
			int index = i;
			while (index < size / 2) {
				int current = countersHeap[index];
				int leftIndex = 2 * index;
				int rightIndex = 2 * index + 1;
				int left = countersHeap[leftIndex];
				int right = countersHeap[rightIndex];
				if (countersHeap[index] > countersHeap[2 * index] ||
						countersHeap[index] > countersHeap[2 * index + 1]) {
					if (countersHeap[2 * index] > countersHeap[2 * index + 1]) {
						// Swap with left
						int temp = countersHeap[index];
						int leftIndex = 2 * index;
						index = 2 * index;
					} else {
						// Swap with right

						index = 2 * index + 1;
					}
				}
			}
		}
		// Poll from heap
		while (size > 0) {
			Direction peeked = directionsHeap[0];
			if (naiveMove(peeked)) {
				return;
			}
			// Remove
			directionsHeap[1] = directionsHeap[size];
			countersHeap[1] = countersHeap[size];
			size--;
			// Percolate down from 1
			int index = 1;

		}*/
	}
	public static boolean naiveMove(Direction direction) throws GameActionException {
		MapLocation location = Cache.CURRENT_LOCATION.add(direction);
		if (!controller.canSenseLocation(location)) {
			return false;
		}
		if (Util.isBlocked(location)) {
			return false;
		}
		if (Cache.ROBOT_TYPE != RobotType.DELIVERY_DRONE && LatticeUtil.isPit(location)) {
			if (Cache.ROBOT_TYPE != RobotType.MINER || Util.hasLattice) {
				return false;
			}
		}
		// Don't move towards lots of pollution
		if (controller.sensePollution(location) > 24000) {
			return false;
		}
		if (Cache.ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			if (!ignoreNetGuns) {
				switch (direction) {
					case NORTHEAST:
					case NORTHWEST:
					case SOUTHEAST:
					case SOUTHWEST:
						return false; // don't allow diagonal moves if not ignoring net guns
				}
				// Don't move towards net guns
				for (int i = Cache.ALL_NEARBY_ENEMY_NET_GUNS_SIZE; --i >= 0;) {
					MapLocation netgunLocation = Cache.ALL_NEARBY_ENEMY_NET_GUNS[i];
					if (location.isWithinDistanceSquared(netgunLocation, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
						return false;
					}
				}
			}
		} else {
			int currentElevation = controller.senseElevation(Cache.CURRENT_LOCATION);
			int toElevation = controller.senseElevation(location);
			int lower = LandscaperBot.getRealTargetElevation();
			int upper = lower + GameConstants.MAX_DIRT_DIFFERENCE;
			if (Math.abs(currentElevation - toElevation) > GameConstants.MAX_DIRT_DIFFERENCE) {
				if (Cache.ROBOT_TYPE == RobotType.LANDSCAPER) {
					// Try terraform
					// Figure out which one is more out of line - currentElevation or toElevation
					int currentDifference = calculateDifference(currentElevation, lower, upper);
					int toDifference = calculateDifference(toElevation, lower, upper);
					int threshold = (controller.getRoundNum() < 200 ? 20 : LandscaperBot.LANDSCAPING_THRESHOLD);
					if (currentDifference > threshold || toDifference > threshold) {
						// Not worth terraforming
						return false;
					}
					if (toDifference > currentDifference) {
						// Deposit/dig from to, then deposit/dig from current
						if (tryTerraform(location, toElevation, lower, upper)) {
							return true;
						}
						if (tryTerraform(Cache.CURRENT_LOCATION, currentElevation, lower, upper)) {
							return true;
						}
					} else {
						// Deposit/dig from current, then deposit/dig from to
						if (tryTerraform(Cache.CURRENT_LOCATION, currentElevation, lower, upper)) {
							return true;
						}
						if (tryTerraform(location, toElevation, lower, upper)) {
							return true;
						}
					}
					// We're either too full of dirt, or have no dirt
					// So deposit/dig from pit
					if (currentElevation < lower || toElevation < lower) {
						LandscaperBot.tryDigFromPit();
						return true;
					}
					if (currentElevation > upper || toElevation > upper) {
						LandscaperBot.tryDepositToPit();
						return true;
					}
				} else {
					return false;
				}
			}
			// Miners should not move outside of union(lattice, hq vision range) - or at least it used to
			if (Cache.ROBOT_TYPE == RobotType.MINER) {
				// Don't move lower
				if (toElevation < currentElevation) {
					if (controller.getRoundNum() > 180) {
						// Check if toElevation is lower than lower bound
						if (toElevation < GameConstants.getWaterLevel(Cache.controller.getRoundNum()) + 1) {
							// If it's not within vision range AND elevation is lower
							return false;
						}
					}
				}
			}
			// Miners and Landscapers should not move next to drones
			if (Cache.ROBOT_TYPE == RobotType.MINER || Cache.ROBOT_TYPE == RobotType.LANDSCAPER) {
				for (RobotInfo enemy : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
					if (enemy.getType() == RobotType.DELIVERY_DRONE && enemy.getLocation().isAdjacentTo(location)) {
						return false;
					}
				}
			}
		}
		if (controller.canMove(direction)) {
			controller.move(direction);
		}
		return true;
	}
	private static boolean tryTerraform(MapLocation location, int elevation, int lower, int upper) throws GameActionException {
		Direction direction = Cache.CURRENT_LOCATION.directionTo(location);
		if (elevation < lower) {
			if (controller.canDepositDirt(direction)) {
				controller.depositDirt(direction);
				return true;
			}
		} else if (elevation > upper) {
			if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
				return true;
			}
		}
		return false;
	}
	private static int calculateDifference(int elevation, int lower, int upper) {
		if (elevation < lower) {
			return lower - elevation;
		}
		if (elevation > upper) {
			return elevation - upper;
		}
		return 0;
	}
}
