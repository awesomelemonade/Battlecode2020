package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class HQBot implements RunnableBot {
	public static final int NO_HELP_NEEDED = 0;
	public static final int NO_ADDITIONAL_HELP_NEEDED = 1;
	public static final int NEEDS_HELP = 2;
	private RobotController controller;
	private int spawnCount;
	private int initialSoupCount = 0;
	private int turnTimer = 0;
	private int attackWaves = 0;


	public HQBot(RobotController controller) {
		this.controller = controller;
		this.spawnCount = 0;
	}
	@Override
	public void init() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		for (int i = 0; i < Util.FLOOD_FILL_DX.length; i++) {
			int dx = Util.FLOOD_FILL_DX[i];
			int dy = Util.FLOOD_FILL_DY[i];
			MapLocation location = currentLocation.translate(dx, dy);
			if (controller.canSenseLocation(location)) {
				initialSoupCount += controller.senseSoup(location);
			} else {
				break;
			}
		}
		SharedInfo.sendOurHQ(currentLocation, controller.senseElevation(currentLocation));
	}
	@Override
	public void turn() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (controller.getRoundNum() > 1000) {
			turnTimer++;
			if (SharedInfo.attackState == SharedInfo.ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS) {
				if (turnTimer > 70) {
					SharedInfo.sendAttackState(SharedInfo.ATTACK_STATE_NONE);
					SharedInfo.dronesBuilt = 0;
				}
			} else if (SharedInfo.dronesBuilt >= 30 || SharedInfo.dronesBuilt >= 15 && turnTimer > 250) {
				SharedInfo.sendAttackState(SharedInfo.ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS);
				SharedInfo.dronesBuilt = 0;
				turnTimer = 0;
				attackWaves++;
			} else if (SharedInfo.dronesBuilt >= 5) {
				if (attackWaves % 2 == 1) {
					SharedInfo.sendAttackState(SharedInfo.ATTACK_STATE_ENEMYHQ);
				} else {
					SharedInfo.sendAttackState(SharedInfo.ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS);
				}
			}
		}
		System.out.println("Attack State: " + SharedInfo.attackState);
		// Calculates state
		int state;
		if (Cache.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
			if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) { // If we can see all adjacent locations
				int hqElevation = controller.senseElevation(Cache.CURRENT_LOCATION);
				boolean allNeighborsOccupied = true;
				for (Direction direction : Util.ADJACENT_DIRECTIONS) {
					MapLocation location = currentLocation.add(direction);
					if (controller.onTheMap(location)) {
						int elevation = controller.senseElevation(location);
						if (!controller.isLocationOccupied(location) && Math.abs(hqElevation - elevation) <= 15) {
							allNeighborsOccupied = false;
							break;
						}
					}
				}
				if (allNeighborsOccupied) {
					state = NO_ADDITIONAL_HELP_NEEDED;
				} else {
					int count = 0;
					for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
						if (robot.getLocation().isAdjacentTo(currentLocation) && robot.getType() == RobotType.LANDSCAPER) {
							count++;
						}
					}
					state = count >= 4 ? NO_ADDITIONAL_HELP_NEEDED : NEEDS_HELP;
				}
			} else {
				// This really shouldn't happen
				state = NO_ADDITIONAL_HELP_NEEDED;
			}
		}
		else {
			state = NO_HELP_NEEDED;
		}
		if (SharedInfo.getOurHQState() != state) {
			SharedInfo.sendOurHQState(state);
		}
		//Landscaper wall state
		if (SharedInfo.getVaporatorCount() >= 8) {
			if(SharedInfo.wallState == SharedInfo.WALL_STATE_NONE) {
				if(controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
					for(Direction direction : Util.ADJACENT_DIRECTIONS) {
						MapLocation location = currentLocation.add(direction);
						if(controller.onTheMap(location)) {
							if(!controller.isLocationOccupied(location)) {
								SharedInfo.sendWallState(SharedInfo.WALL_STATE_NEEDS);
								break;
							}
						}
					}
				}
			}
			else if(SharedInfo.wallState == SharedInfo.WALL_STATE_NEEDS) {
				boolean allNeighborsOccupied = true;
				for(Direction direction : Util.ADJACENT_DIRECTIONS) {
					MapLocation location = currentLocation.add(direction);
					if(controller.onTheMap(location)) {
						if(!controller.isLocationOccupied(location)) {
							allNeighborsOccupied = false;
						}
					}
				}
				if(allNeighborsOccupied) {
					SharedInfo.sendWallState(SharedInfo.WALL_STATE_STAYS);
				}
			}
		}
		
		int designSchoolCount = 0;
		int fulfillmentCenterCount = 0;
		for (RobotInfo robot : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
			if (robot.getType() == RobotType.DESIGN_SCHOOL) {
				designSchoolCount++;
			}
			if (robot.getType() == RobotType.FULFILLMENT_CENTER) {
				fulfillmentCenterCount++;
			}
		}
		if (designSchoolCount != SharedInfo.getDesignSchoolCount() ||
				fulfillmentCenterCount != SharedInfo.getFulfillmentCenterCount()) {
			SharedInfo.sendOurHQUnitCount(designSchoolCount, fulfillmentCenterCount);
		}
		if (!controller.isReady()) {
			return;
		}
		if (Util.tryShootDrone()) {
			return;
		}
		if (spawnCount < Math.min(5, Math.max(2, initialSoupCount * 2 / RobotType.MINER.cost / 3))) {
			for (int i = 1; i < Util.FLOOD_FILL_DX.length; i++) { // Skip first index to prevent idealDirection = CENTER
				MapLocation location = currentLocation.translate(Util.FLOOD_FILL_DX[i], Util.FLOOD_FILL_DY[i]);
				if (!controller.canSenseLocation(location)) {
					break;
				}
				if (controller.senseSoup(location) > 0) {
					Direction idealDirection = Util.getDirection(Util.FLOOD_FILL_DX_CLAMPED[i], Util.FLOOD_FILL_DY_CLAMPED[i]);
					for (Direction direction : Util.getAttemptOrder(idealDirection)) {
						if (Util.canSafeBuildRobot(RobotType.MINER, direction)) {
							controller.buildRobot(RobotType.MINER, direction);
							this.spawnCount++;
							break;
						}
					}
					return;
				}
			}
			if (spawnCount < 3) {
				tryBuildMiner();
			}
		}
	}
	public void tryBuildMiner() throws GameActionException {
		Direction direction = Util.randomAdjacentDirection();
		if (Util.canSafeBuildRobot(RobotType.MINER, direction)) {
			controller.buildRobot(RobotType.MINER, direction);
			this.spawnCount++;
		}
	}
}
