package citricsky.battlecode2020.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class UnitsMap {
	private static final long BITMASK_32 = 0b11111111111111111111111111111111L;
	private static RobotController controller;
	private static InfoMap infoMap;
	public static void init(RobotController controller) {
		UnitsMap.controller = controller;
		infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
	}
	public static boolean hasBlockingUnit(MapLocation location) {
		return hasBlockingUnit(location.x, location.y);
	}
	public static boolean hasBlockingUnit(int x, int y) {
		long turn = controller.getRoundNum();
		long data = infoMap.get(x, y);
		long a = data >>> 32;
		long b = data & BITMASK_32;
		return (turn - a) < 2 && (turn - b) < 2;
	}
	public static void loop() {
		long turn = controller.getRoundNum();
		boolean even = (turn % 2) == 0;
		long and = even ? BITMASK_32 : ~BITMASK_32;
		long or = even ? (turn << 32) : turn;
		RobotInfo[] robots = Cache.ALL_NEARBY_ROBOTS;
		for (RobotInfo robot : robots) {
			MapLocation location = robot.getLocation();
			int x = location.x;
			int y = location.y;
			infoMap.set(x, y, infoMap.get(x, y) & and | or);
		}
	}
}
