package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.*;

public class LandscaperBot implements RunnableBot {
	private static final int LANDSCAPING_THRESHOLD = 100; // If the dirt difference is this high, we won't terraform
	private RobotController controller;
	//private InfoMap infoMap;
	private RobotBehavior[] behaviors;

	public LandscaperBot(RobotController controller) {
		this.controller = controller;
	}
	@Override
	public void init() {
		//infoMap = new InfoMap(controller.getMapWidth(), controller.getMapHeight());
		// Order of behaviors to be executed
		behaviors = new RobotBehavior[] {
				//try attack,
				//Util::tryHealHQ,
				//Util::tryHealBuildings,
				this::tryTerraform
		};
	}
	private int targetElevation = 1;
	@Override
	public void turn() throws GameActionException {
		while (Util.getTurnsToFlooded(targetElevation) - controller.getRoundNum() < 100) {
			targetElevation++;
		}
		if (!controller.isReady()) {
			return;
		}
		for (int i = 0; i < behaviors.length; i++) {
			if (behaviors[i].execute()) {
				// Executed successfully
				return;
			}
		}
	}
	public boolean tryTerraform() throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		int targetElevation = this.targetElevation + 5;
		int upperTargetElevation = targetElevation + GameConstants.MAX_DIRT_DIFFERENCE;
		int hqParityX = SharedInfo.getOurHQParityX();
		int hqParityY = SharedInfo.getOurHQParityY();
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (!controller.canSenseLocation(location)) {
				return false;
			}
			RobotInfo robot = controller.senseRobotAtLocation(location);
			if (robot != null && robot.getTeam() == Cache.OUR_TEAM && robot.getType().isBuilding()) {
				continue;
			}
			if (!(location.x % 3 == hqParityX && location.y % 3 == hqParityY)) {
				int elevation = controller.senseElevation(location);
				// Fill up to target elevation or dig to targetElevation + 3
				if (elevation < targetElevation) {
					if (targetElevation - elevation <= LANDSCAPING_THRESHOLD) {
						// Try to raise elevation
						tryToRaise(location);
						return true;
					}
				} else if (elevation > upperTargetElevation) {
					if (elevation - upperTargetElevation <= LANDSCAPING_THRESHOLD) {
						// Try to lower elevation
						tryToLower(location);
						return true;
					}
				}
			}
		}
		return false;
	}
	public void tryToRaise(MapLocation location) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (currentLocation.isWithinDistanceSquared(location, 2)) {
			Direction direction = currentLocation.directionTo(location);
			if (controller.canDepositDirt(direction)) {
				controller.depositDirt(direction);
			} else {
				Direction pitDirection = LatticeUtil.getPitDirection(currentLocation);
				if (controller.canDigDirt(pitDirection)) {
					controller.digDirt(pitDirection);
				}
			}
		} else {
			Pathfinding.execute(location);
		}
	}
	public void tryToLower(MapLocation location) throws GameActionException {
		MapLocation currentLocation = controller.getLocation();
		if (currentLocation.isWithinDistanceSquared(location, 2)) {
			Direction direction = currentLocation.directionTo(location);
			if (controller.canDigDirt(direction)) {
				controller.digDirt(direction);
			} else {
				Direction pitDirection = LatticeUtil.getPitDirection(currentLocation);
				if (controller.canDepositDirt(pitDirection)) {
					controller.depositDirt(pitDirection);
				}
			}
		} else {
			Pathfinding.execute(location);
		}
	}
}
