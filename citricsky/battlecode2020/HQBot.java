package citricsky.battlecode2020;

import battlecode.common.*;

public class HQBot implements RunnableBot {
	private RobotController controller;
	private FastDeque queue;
	public HQBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		queue = new FastDeque(16 * 16);
	}
	@Override
	public void turn() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		queue.reset();
		BooleanArray visited = new BooleanArray(4); // 256
		int currentLocationHash = hashAbs(0) << 4 | hashAbs(0);
		queue.push((currentLocationHash << 8) | currentLocationHash);
		visited.set(currentLocationHash);
		int count = 0;
		while (!queue.isEmpty()) {
			count++;
			int sum = 0;
			int before = Clock.getBytecodeNum();
			int polled = queue.poll();
			int bitmasked = polled & 0b11111111;
			int dx = unhashAbs(bitmasked >>> 4);
			int dy = unhashAbs(bitmasked & 0b1111);
			MapLocation location = currentLocation.translate(dx, dy);
			// stop condition
			if (controller.senseSoup(location) > 0) {
				System.out.println("Ay: " + location);
			}
			// traverses adjacent tiles
			for (Direction direction : Util.ADJACENT_DIRECTIONS) {
				MapLocation adjacent = location.add(direction);
				if (controller.canSenseLocation(adjacent)) {
					int adjacentHash =
							hashAbs(dx + direction.getDeltaX()) << 4 |
							hashAbs(dy + direction.getDeltaY());
					if (!visited.get(adjacentHash)) {
						queue.push((bitmasked << 8) | adjacentHash);
						visited.set(adjacentHash);
					}
				}
			}
			sum += (Clock.getBytecodeNum() - before);
			System.out.println("!!: " + sum + " - " + Clock.getBytecodesLeft());
		}
		System.out.println("Count: " + count);
	}
	public static int hashAbs(int offset) {
		return offset + 7;
	}
	public static int unhashAbs(int hash) {
		return hash - 7;
	}
}
