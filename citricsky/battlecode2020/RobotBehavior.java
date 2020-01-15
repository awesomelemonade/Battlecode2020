package citricsky.battlecode2020;

import battlecode.common.GameActionException;

@FunctionalInterface
public interface RobotBehavior {
	boolean execute() throws GameActionException;
}
