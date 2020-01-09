package citricsky;

import battlecode.common.GameActionException;

public interface RunnableBot {
	public void init();
	public void turn() throws GameActionException;
}
