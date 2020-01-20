package citricsky.battlecode2020.util;

import battlecode.common.*;

public class Cache {
	private static RobotController controller;
	public static Team OUR_TEAM;
	public static Team OPPONENT_TEAM;
	public static RobotInfo[] ALL_NEARBY_ROBOTS;
	public static RobotInfo[] ALL_NEARBY_FRIENDLY_ROBOTS;
	public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
	public static MapLocation MAP_CENTER_LOCATION;
	public static MapLocation CURRENT_LOCATION;
	public static RobotType ROBOT_TYPE;
	// RobotType specific
	public static MapLocation[] ALL_NEARBY_ENEMY_NET_GUNS;
	public static int ALL_NEARBY_ENEMY_NET_GUNS_SIZE;
	public static void init(RobotController controller) {
		Cache.controller = controller;
		OUR_TEAM = controller.getTeam();
		OPPONENT_TEAM = OUR_TEAM.opponent();
		MAP_CENTER_LOCATION = new MapLocation(controller.getMapWidth() / 2, controller.getMapHeight() / 2);
		ROBOT_TYPE = controller.getType();
		CURRENT_LOCATION = controller.getLocation();
		if (ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			ALL_NEARBY_ENEMY_NET_GUNS = new MapLocation[68]; // max number of net guns in vision range
			ALL_NEARBY_ENEMY_NET_GUNS_SIZE = 0;
		}
	}
	public static void loop() {
		Cache.ALL_NEARBY_ROBOTS = controller.senseNearbyRobots();
		Cache.ALL_NEARBY_FRIENDLY_ROBOTS = controller.senseNearbyRobots(-1, OUR_TEAM);
		Cache.ALL_NEARBY_ENEMY_ROBOTS = controller.senseNearbyRobots(-1, OPPONENT_TEAM);
		Cache.CURRENT_LOCATION = controller.getLocation();
		if (ROBOT_TYPE == RobotType.DELIVERY_DRONE) {
			ALL_NEARBY_ENEMY_NET_GUNS_SIZE = 0;
			for (RobotInfo robot : Cache.ALL_NEARBY_ENEMY_ROBOTS) {
				if (robot.getType() == RobotType.DELIVERY_DRONE) {
					ALL_NEARBY_ENEMY_NET_GUNS[ALL_NEARBY_ENEMY_NET_GUNS_SIZE++] = robot.getLocation();
				}
			}
			MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
			if (enemyHQ != null) {
				ALL_NEARBY_ENEMY_NET_GUNS[ALL_NEARBY_ENEMY_NET_GUNS_SIZE++] = enemyHQ;
			}
		}
	}
}
