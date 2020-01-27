package citricsky.battlecode2020;

import battlecode.common.*;
import citricsky.RunnableBot;
import citricsky.battlecode2020.util.Cache;
import citricsky.battlecode2020.util.LatticeUtil;
import citricsky.battlecode2020.util.SharedInfo;
import citricsky.battlecode2020.util.Util;

public class HQBot implements RunnableBot {
	public static final int NO_HELP_NEEDED = 0;
	public static final int NO_ADDITIONAL_HELP_NEEDED = 1;
	public static final int NEEDS_HELP = 2;
	private RobotController controller;
	private int spawnCount;
	private int turnTimer = 0;
	private int turnsToPickupLandscapers = 0;
	private int attackWaves = 0;
	private boolean allNeighborsOccupied = false;
	private MapLocation[] vaporatorLocations = new MapLocation[20];
	private int vaporatorLocationsIndex = 0;
	private boolean hasSeenVaporatorFlooded = false;

	public HQBot(RobotController controller) {
		this.controller = controller;
		this.spawnCount = 0;
	}
	@Override
	public void init() throws GameActionException {
		SharedInfo.sendOurHQ(Cache.CURRENT_LOCATION, controller.senseElevation(Cache.CURRENT_LOCATION));
	}
	@Override
	public void turn() throws GameActionException {
		MapLocation currentLocation = Cache.CURRENT_LOCATION;
		if (controller.getRoundNum() > 800) {
			turnTimer++;
			turnsToPickupLandscapers++;
			int newAttackState = -1;
			if (SharedInfo.getAttackState() == SharedInfo.ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS) {
				if (turnTimer > 70) {
					newAttackState = SharedInfo.ATTACK_STATE_NONE;
				}
			} else if (turnsToPickupLandscapers >= 0 && (SharedInfo.dronesReady >= 30 || SharedInfo.dronesReady >= 15 && turnTimer > 250)) {
				newAttackState = SharedInfo.ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS;
				turnTimer = 0;
				attackWaves++;
			} else if ((SharedInfo.dronesBuilt >= 5 && SharedInfo.getOurHQState() == NO_HELP_NEEDED) || SharedInfo.dronesBuilt >= 10) {
				if (attackWaves % 2 == 1) {
					newAttackState = SharedInfo.ATTACK_STATE_ENEMYHQ;
				} else {
					newAttackState = SharedInfo.ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS;
				}
			}
			if (newAttackState != -1) {
				if (newAttackState != SharedInfo.getAttackState()) {
					SharedInfo.sendAttackState(newAttackState);
					if (newAttackState == SharedInfo.ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS) {
						turnsToPickupLandscapers = -20;
					}
				}
			}
		}
		//If we haven't seen a vaporator flood, then check previous array of vaporator locations for any flooding
		if (!hasSeenVaporatorFlooded) {
			for (int i = 0; i < vaporatorLocationsIndex; i++) {
				if (controller.canSenseLocation(vaporatorLocations[i])) {
					if (controller.senseFlooding(vaporatorLocations[i])) {
						hasSeenVaporatorFlooded = true;
						break;
					}
				}
			}
			//update vaporator locations array
			vaporatorLocationsIndex = 0;
			for (RobotInfo ally : Cache.ALL_NEARBY_FRIENDLY_ROBOTS) {
				if (ally.getType() == RobotType.VAPORATOR) {
					vaporatorLocations[vaporatorLocationsIndex] = ally.getLocation();
					vaporatorLocationsIndex++;
					if (vaporatorLocationsIndex == vaporatorLocations.length) {
						break;
					}
				}
			}
		}
		
		if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) { // If we can see all adjacent locations
			int hqElevation = controller.senseElevation(Cache.CURRENT_LOCATION);
			allNeighborsOccupied = true;
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
		}
		if (controller.getRoundNum() >= 3) { // Makes first turn take less bytecodes
			// Calculates state
			int state;
			if (FulfillmentCenterBot.findEnemyMinerOrLandscaper() != null) {
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
				state = NO_HELP_NEEDED;
			}
			if (SharedInfo.getOurHQState() != state) {
				SharedInfo.sendOurHQState(state);
			}
			//Landscaper wall state
			if (SharedInfo.getVaporatorCount() >= 8 || hasSeenVaporatorFlooded) {
				int newWallState = SharedInfo.wallState;
				if (SharedInfo.wallState == SharedInfo.WALL_STATE_NONE) {
					newWallState = SharedInfo.WALL_STATE_NEEDS;
				}
				if (newWallState == SharedInfo.WALL_STATE_NEEDS) {
					if (controller.canSenseRadiusSquared(Util.ADJACENT_DISTANCE_SQUARED)) {
						boolean allWallSpotsOccupied = true;
						for (Direction direction : Util.ADJACENT_DIRECTIONS) {
							MapLocation location = currentLocation.add(direction);
							if (Util.onTheMap(location)) {
								if (SharedInfo.ourHQNearCorner && Util.isInCorner(location)) {
									continue;
								}
								int elevation = controller.senseElevation(location);
								if (elevation > 200) {
									// TODO
									continue;
								}
								RobotInfo robot = controller.senseRobotAtLocation(location);
								if (robot == null || robot.getTeam() == Cache.OPPONENT_TEAM || robot.getType() != RobotType.LANDSCAPER) {
									allWallSpotsOccupied = false;
									break;
								}
							}
						}
						if (allWallSpotsOccupied) {
							newWallState = SharedInfo.WALL_STATE_STAYS;
						}
					}
				}
				if (SharedInfo.wallState != newWallState) {
					SharedInfo.sendWallState(newWallState);
				}
			}
			
			if (hasSeenVaporatorFlooded) {
				int newWallState = SharedInfo.wallState;
				if (SharedInfo.wallState == SharedInfo.WALL_STATE_STAYS) {
					newWallState = SharedInfo.WALL_STATE_NEEDS_LARGER;
				}
				if (newWallState == SharedInfo.WALL_STATE_NEEDS_LARGER) {
					boolean allOuterNeighborsOccupied = true;
					MapLocation[] outerNeighborLocations = Util.getOuterAdjacentTiles(currentLocation);
					for (MapLocation location : outerNeighborLocations) {
						if (Util.onTheMap(location)) {
							RobotInfo robot = controller.senseRobotAtLocation(location);
							if ((robot == null && !controller.senseFlooding(location) && !LatticeUtil.isPit(location)) || robot != null && (robot.getTeam() == Cache.OPPONENT_TEAM || (robot.getType() != RobotType.LANDSCAPER && !robot.getType().isBuilding()))) {
								System.out.println("Location " + location.x + " " + location.y + " is empty");
								allOuterNeighborsOccupied = false;
								break;
							}
							
						}
					}
					if (allOuterNeighborsOccupied) {
						newWallState = SharedInfo.WALL_STATE_STAYS_LARGER;
					}
				}
				if (SharedInfo.wallState != newWallState) {
					SharedInfo.sendWallState(newWallState);
				}
			}
			
			System.out.printf("Attack=%d; Wall=%d; HQ=%d; DBuilt=%d; DReady=%d; NetGun=%b\n",
					SharedInfo.getAttackState(), SharedInfo.wallState, SharedInfo.getOurHQState(), SharedInfo.dronesBuilt, SharedInfo.dronesReady, SharedInfo.isSavingForNetgun);

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
		}
		if (!controller.isReady()) {
			return;
		}
		if (tryHQShootDrone()) {
			return;
		}
		if (spawnCount < 5) { // Math.min(5, Math.max(2, initialSoupCount * 2 / RobotType.MINER.cost / 3))
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
			tryBuildMiner();
		}
	}
	public void tryBuildMiner() throws GameActionException {
		Direction direction = Util.randomAdjacentDirection();
		if (Util.canSafeBuildRobot(RobotType.MINER, direction)) {
			controller.buildRobot(RobotType.MINER, direction);
			this.spawnCount++;
		}
	}
	public boolean tryHQShootDrone() throws GameActionException {
		RobotInfo[] enemies = controller.senseNearbyRobots(Cache.CURRENT_LOCATION,
				GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, controller.getTeam().opponent());
		int bestPriority = 0;
		int bestDistanceSquared = Integer.MAX_VALUE;
		RobotInfo bestTarget = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.getType() == RobotType.DELIVERY_DRONE) {
				int enemyID = enemy.getID();
				int enemyPriority = getPriority(enemy);
				int enemyDistance = Cache.CURRENT_LOCATION.distanceSquaredTo(enemy.getLocation());
				if (enemyPriority > bestPriority || ((enemyPriority == bestPriority) && enemyDistance < bestDistanceSquared)) {
					if (controller.canShootUnit(enemyID)) {
						bestPriority = enemyPriority;
						bestDistanceSquared = enemyDistance;
						bestTarget = enemy;
					}
				}
			}
		}
		if (bestTarget != null) {
			controller.shootUnit(bestTarget.getID());
			return true;
		}
		return false;
	}
	public int getPriority(RobotInfo enemy) throws GameActionException {
		if (enemy.isCurrentlyHoldingUnit()) {
			if (allNeighborsOccupied) {
				return 1;
			} else {
				return 2;
			}
		} else {
			if (allNeighborsOccupied) {
				return 2;
			} else {
				return 1;
			}
		}
	}
}
