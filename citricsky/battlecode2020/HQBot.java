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
		int[] visited = new int[256];
		int currentLocationHash = hashAbs(0) << 4 | hashAbs(0);
		queue.push(currentLocationHash);
		visited[currentLocationHash] = currentLocationHash + 1;
		int count = 0;
		while (!queue.isEmpty()) {
			count++;
			int sum = 0;
			int before = Clock.getBytecodeNum();
			int polled = queue.poll();
			int dx = unhashAbs(polled >>> 4);
			int dy = unhashAbs(polled & 0b1111);
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
					if (visited[adjacentHash] == 0) {
						queue.push(adjacentHash);
						visited[adjacentHash] = polled + 1;
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
