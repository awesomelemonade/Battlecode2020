package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private RobotController controller;
	//private InfoMap infoMap;
	private boolean attacking;
	private boolean wallCreator;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
		this.attacking = false;
		this.wallCreator = false;
	}
	@Override
	public void init() {
		//infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		// Turn logic
		MapLocation currentLocation = controller.getLocation();
		MapLocation enemyHQ = SharedInfo.getEnemyHQLocation();
		MapLocation ourHQ = SharedInfo.getOurHQLocation();

		// Try healing buildings
		if (Util.tryHealBuildings()) {
			return;
		}
	}
}
