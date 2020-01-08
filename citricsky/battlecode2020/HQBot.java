package citricsky.battlecode2020;

import battlecode.common.*;

public class HQBot implements RunnableBot {
	private RobotController controller;
	private FastDeque queue;
	public HQBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		Communication.preload();
	}
	@Override
	public void turn() throws GameActionException {
		if (!controller.isReady()) {
			return;
		}
		MapLocation currentLocation = controller.getLocation();
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			MapLocation location = currentLocation.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
			if (!controller.canSenseLocation(location)) {
				break;
			}
			if (controller.senseSoup(location) > 0) {
				Direction direction = Util.getDirection(Util.FLOOD_FILL_DX_CLAMPED[i], Util.FLOOD_FILL_DY_CLAMPED[i]);
				System.out.println("Sensed Soup at: " + location);
				System.out.println("Direction: " + direction);
				if (controller.canBuildRobot(RobotType.MINER, direction)) {
					controller.buildRobot(RobotType.MINER, direction);
				}
				break;
			}
		}
	}
	public static int hashAbs(int offset) {
		return offset + 7;
	}
	public static int unhashAbs(int hash) {
		return hash - 7;
	}
}
