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
	public static void init(RobotController controller) {
		Cache.controller = controller;
		OUR_TEAM = controller.getTeam();
		OPPONENT_TEAM = OUR_TEAM.opponent();
		MAP_CENTER_LOCATION = new MapLocation(controller.getMapWidth() / 2, controller.getMapHeight() / 2);
		ROBOT_TYPE = controller.getType();
		CURRENT_LOCATION = controller.getLocation();
	}
	public static void loop() {
		Cache.ALL_NEARBY_ROBOTS = controller.senseNearbyRobots();
		Cache.ALL_NEARBY_FRIENDLY_ROBOTS = controller.senseNearbyRobots(-1, OUR_TEAM);
		Cache.ALL_NEARBY_ENEMY_ROBOTS = controller.senseNearbyRobots(-1, OPPONENT_TEAM);
		Cache.CURRENT_LOCATION = controller.getLocation();
	}
}
