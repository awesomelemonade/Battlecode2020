package citricsky.battlecode2020.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class UnitsMap {
	private static final long BITMASK_32 = 0b11111111111111111111111111111111L;
	private static final long BITMASK_16 = 0b1111111111111111L;
	private static RobotController controller;
	private static InfoMap infoMap;
	public static void init(RobotController controller) {
		UnitsMap.controller = controller;
		infoMap = new InfoMap(Cache.MAP_WIDTH, Cache.MAP_HEIGHT);
	}
	public static boolean hasBlockingUnit(MapLocation location) {
		return hasBlockingUnit(location.x, location.y);
	}
	public static boolean hasBlockingUnit(int x, int y) {
		long turn = controller.getRoundNum();
		long data = infoMap.get(x, y);
		long a = data >>> 48;
		long b = (data >>> 32) & BITMASK_16;
		boolean building = (data & BITMASK_32) == 1;
		return ((turn - a) < 2 && (turn - b) < 2) || building;
	}
	public static void loop() {
		long turn = controller.getRoundNum();
		boolean even = (turn % 2) == 0;
		long and = even ? (~(BITMASK_16 << 48)) : (~(BITMASK_16 << 32));
		long or = even ? (turn << 48) : (turn << 32);
		RobotInfo[] robots = Cache.ALL_NEARBY_ROBOTS;
		for (RobotInfo robot : robots) {
			MapLocation location = robot.getLocation();
			int x = location.x;
			int y = location.y;
			int buildingCode = robot.getType().isBuilding() ? 1 : 0;
			infoMap.set(x, y, ((infoMap.get(x, y) & and | or) & ~0b1) | buildingCode);
		}
	}
}
