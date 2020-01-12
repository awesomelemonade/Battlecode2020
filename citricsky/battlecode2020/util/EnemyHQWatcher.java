package citricsky.battlecode2020.util;

import battlecode.common.*;

public class EnemyHQWatcher {
	private static RobotController controller;
	private static boolean[] blocked;
	private static FastIntDeque potentialLocations;
	private static int bestDistanceSquared;
	public static void init(RobotController controller) {
		EnemyHQWatcher.controller = controller;
		blocked = new boolean[Util.FLOOD_FILL_DX.length];
		potentialLocations = new FastIntDeque(Util.FLOOD_FILL_DX.length);
	}
	public static void loop() throws GameActionException {
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		if (enemyHQ == null) {
			return;
		}
		bestDistanceSquared = Integer.MAX_VALUE;
		potentialLocations.reset();
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = enemyHQ.translate(dx, dy);
			if (controller.canSenseLocation(location)) {
				RobotInfo robot = controller.senseRobotAtLocation(location);
				if (robot == null) {
					blocked[i] = false;
				} else {
					blocked[i] = robot.getTeam() == Cache.OPPONENT_TEAM || robot.getType() == RobotType.LANDSCAPER;
				}
			}
			if (!blocked[i] && enemyHQ.isWithinDistanceSquared(location, bestDistanceSquared)) {
				bestDistanceSquared = enemyHQ.distanceSquaredTo(location);
				potentialLocations.push(location.x << 16 | location.y);
			}
		}
	}
	public static MapLocation findClosestPotentialLocation(MapLocation location) {
		MapLocation bestLocation = null;
		int bestDistanceSquared = Integer.MAX_VALUE;
		for (int i = 0; i < potentialLocations.size(); i++) {
			int x = potentialLocations.get(i);
			MapLocation temp = new MapLocation(x >>> 16, x & 0b1111111111111111);
			int distanceSquared = location.distanceSquaredTo(temp);
			if (distanceSquared < bestDistanceSquared) {
				bestDistanceSquared = distanceSquared;
				bestLocation = temp;
			}
		}
		return bestLocation;
	}
}
