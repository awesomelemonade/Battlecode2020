package citricsky.battlecode2020;

import battlecode.common.GameActionException;

public interface RunnableBot {
	public void init();
	public void turn() throws GameActionException;
}
