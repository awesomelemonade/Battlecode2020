package citricsky.battlecode2020.util;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Cache {
	private static RobotController controller;
	public static Team OPPONENT_TEAM;
	public static RobotInfo[] ALL_NEARBY_ROBOTS;
	public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
	public static void init(RobotController controller) {
		Cache.controller = controller;
		OPPONENT_TEAM = controller.getTeam().opponent();
	}
	public static void loop() {
		Cache.ALL_NEARBY_ROBOTS = controller.senseNearbyRobots();
		Cache.ALL_NEARBY_ENEMY_ROBOTS = controller.senseNearbyRobots(-1, OPPONENT_TEAM);
	}
}
