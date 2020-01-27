package citricsky.battlecode2020.util;

import battlecode.common.*;
import citricsky.battlecode2020.DroneBot;
import citricsky.battlecode2020.HQBot;

public class SharedInfo {
	public static final int OURHQ_TRANSACTION_COST = 3;

	private static final int OURHQ_STATE_SIGNATURE = 8963124;
	private static final int ENEMYHQ_SIGNATURE = 2130985;
	private static final int ENEMYHQ_MODE_SIGNATURE = 415912;
	private static final int OURHQ_SIGNATURE = 51351235;
	private static final int NEWSOUP_SIGNATURE = -1352350;
	private static final int SOUPGONE_SIGNATURE = 72952835;
	private static final int NEWLANDSCAPER_SIGNATURE = -8892517;
	private static final int NEWDRONE_SIGNATURE = -2951958;
	private static final int DRONE_READY_SIGNATURE = 5392350;
	private static final int ATTACK_STATE_SIGNATURE = 1295952;
	private static final int OURHQ_UNITCOUNT_SIGNATURE = 695318;
	private static final int VAPORATOR_COUNT_INCREMENT_SIGNATURE = 3431285;
	private static final int TOGGLE_SAVEFOR_NETGUN_SIGNATURE = -9988235;
	private static final int WALLSTATE_CHANGE_SIGNATURE = 35289359;
	private static final int WATER_SIGNATURE = -8529682;

	private static RobotController controller;
	private static MapLocation ourHQLocation;
	private static int ourHQElevation;
	private static int ourHQParityX = -1;
	private static int ourHQParityY = -1;
	public static boolean ourHQNearCorner = false;
	private static MapLocation enemyHQLocation;
	private static int enemyHQGuesserMode = EnemyHQGuesser.UNKNOWN_MODE;
	private static int ourHQState = HQBot.NO_HELP_NEEDED;

	public static int landscapersBuilt = 0;
	
	// Attack with drones info
	public static int dronesBuilt = 0;
	public static int dronesReady = 0;
	public static final int ATTACK_STATE_NONE = 0;
	public static final int ATTACK_STATE_ENEMYHQ = 1;
	public static final int ATTACK_STATE_ENEMYHQ_WITH_LANDSCAPERS = 2;
	public static final int ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS = 3;
	private static int attackState = ATTACK_STATE_NONE;

	// Build order info
	private static int designSchoolCount = 0;
	private static int fulfillmentCenterCount = 0;
	private static int vaporatorCount = 0;
	
	public static boolean isSavingForNetgun = false;
	
	// Wall state for HQ
	public static int wallState = 0;
	public static final int WALL_STATE_NONE = 0;
	public static final int WALL_STATE_NEEDS = 1;
	public static final int WALL_STATE_STAYS = 2;

	public static void init(RobotController controller) {
		SharedInfo.controller = controller;
		EnemyHQGuesser.init(controller);
	}
	public static void sendEnemyHQ(MapLocation location) {
		setEnemyHQLocation(location);
		int[] message = new int[] {
				ENEMYHQ_SIGNATURE, enemyHQLocation.x, enemyHQLocation.y, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendOurHQ(MapLocation location, int elevation) {
		setOurHQLocation(location, elevation);
		int[] message = new int[] {
				OURHQ_SIGNATURE, location.x, location.y, elevation, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message, OURHQ_TRANSACTION_COST);
	}
	public static void sendEnemyGuessMode(int mode) {
		setEnemyHQGuesserMode(mode);
		int[] message = new int[] {
				ENEMYHQ_MODE_SIGNATURE, mode, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendOurHQState(int state) {
		setOurHQState(state);
		int[] message = new int[] {
				OURHQ_STATE_SIGNATURE, state, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendSoup(MapLocation location) {
		int[] message = new int[] {
				NEWSOUP_SIGNATURE, location.x, location.y, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendSoupGone(MapLocation location) {
		int[] message = new int[] {
				SOUPGONE_SIGNATURE, location.x, location.y, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void builtNewLandscaper() {
		int[] message = new int[] {
				NEWLANDSCAPER_SIGNATURE, 0, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void builtNewDrone() {
		int[] message = new int[] {
				NEWDRONE_SIGNATURE, 0, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendDroneReady() {
		int[] message = new int[] {
				DRONE_READY_SIGNATURE, 0, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendAttackState(int attackState) {
		setAttackState(attackState);
		int[] message = new int[] {
				ATTACK_STATE_SIGNATURE, attackState, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendOurHQUnitCount(int designSchoolCount, int fulfillmentCenterCount) {
		setOurHQUnitCount(designSchoolCount, fulfillmentCenterCount);
		int[] message = new int[] {
				OURHQ_UNITCOUNT_SIGNATURE, designSchoolCount, fulfillmentCenterCount, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendVaporatorCountIncrement() {
		int[] message = new int[] {
				VAPORATOR_COUNT_INCREMENT_SIGNATURE, 0, 0, 0, 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendSaveForNetgunSignal(boolean saving) {
		int[] message = new int[] {
				TOGGLE_SAVEFOR_NETGUN_SIGNATURE, 0, 0, (saving ? 1 : 0), 0, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendWallState(int state) {
		int[] message = new int[] {
				WALLSTATE_CHANGE_SIGNATURE, 0, 0, 0, state, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void sendWaterState(boolean flooding, MapLocation location) {
		int[] message = new int[] {
				WATER_SIGNATURE, flooding ? 1 : 0, 0, location.x, location.y, 0, 0
		};
		Communication.encryptMessage(message);
		CommunicationProcessor.queueMessage(message);
	}
	public static void processMessage(int[] message) {
		switch(message[0]) {
			case ENEMYHQ_SIGNATURE:
				setEnemyHQLocation(new MapLocation(message[1], message[2]));
				break;
			case OURHQ_SIGNATURE:
				setOurHQLocation(new MapLocation(message[1], message[2]), message[3]);
				break;
			case ENEMYHQ_MODE_SIGNATURE:
				setEnemyHQGuesserMode(message[1]);
				break;
			case OURHQ_STATE_SIGNATURE:
				setOurHQState(message[1]);
				break;
			case NEWSOUP_SIGNATURE:
				MapTracker.sharedSoupLocations.add(new MapLocation(message[1], message[2]));
				break;
			case SOUPGONE_SIGNATURE:
				MapTracker.sharedSoupLocations.remove(new MapLocation(message[1], message[2]));
				break;
			case NEWLANDSCAPER_SIGNATURE:
				landscapersBuilt++;
				break;
			case NEWDRONE_SIGNATURE:
				dronesBuilt++;
				break;
			case DRONE_READY_SIGNATURE:
				dronesReady++;
				break;
			case ATTACK_STATE_SIGNATURE:
				setAttackState(message[1]);
				break;
			case OURHQ_UNITCOUNT_SIGNATURE:
				setOurHQUnitCount(message[1], message[2]);
				break;
			case VAPORATOR_COUNT_INCREMENT_SIGNATURE:
				vaporatorCount++;
				break;
			case TOGGLE_SAVEFOR_NETGUN_SIGNATURE:
				isSavingForNetgun = message[3] == 1;
				break;
			case WALLSTATE_CHANGE_SIGNATURE:
				wallState = message[4];
				break;
			case WATER_SIGNATURE:
				MapLocation location = new MapLocation(message[3], message[4]);
				// water state
				if (message[1] == 1) {
					// add water state
					if (MapTracker.sharedClosestWaterToHQ == null) {
						MapTracker.sharedClosestWaterToHQ = location;
					} else {
						MapLocation hqLocation = SharedInfo.getOurHQLocation();
						if (hqLocation != null) {
							if (location.distanceSquaredTo(hqLocation) < MapTracker.sharedClosestWaterToHQ.distanceSquaredTo(hqLocation)) {
								MapTracker.sharedClosestWaterToHQ = location;
							}
						}
					}
					if (MapTracker.sharedClosestWaterToEnemyHQ == null) {
						MapTracker.sharedClosestWaterToEnemyHQ = location;
					} else {
						MapLocation enemyHQLocation = SharedInfo.getEnemyHQLocation();
						if (enemyHQLocation != null) {
							if (location.distanceSquaredTo(enemyHQLocation) < MapTracker.sharedClosestWaterToEnemyHQ.distanceSquaredTo(enemyHQLocation)) {
								MapTracker.sharedClosestWaterToEnemyHQ = location;
							}
						}
					}
				} else {
					// clear water state
					if (location.equals(MapTracker.sharedClosestWaterToHQ)) {
						MapTracker.sharedClosestWaterToHQ = null;
					}
					if (location.equals(MapTracker.sharedClosestWaterToEnemyHQ)) {
						MapTracker.sharedClosestWaterToEnemyHQ = null;
					}
				}
				break;
		}
	}
	private static void setOurHQLocation(MapLocation location, int elevation) {
		EnemyHQGuesser.setGuesses(location.x, location.y);
		ourHQLocation = location;
		ourHQParityX = location.x % 2;
		ourHQParityY = location.y % 2;
		ourHQElevation = elevation;
		ourHQNearCorner = Util.isNearCorner(location);
	}
	public static MapLocation getOurHQLocation() {
		return ourHQLocation;
	}
	public static int getOurHQParityX() {
		return ourHQParityX;
	}
	public static int getOurHQParityY() {
		return ourHQParityY;
	}
	public static int getOurHQElevation() {
		return ourHQElevation;
	}
	private static void setEnemyHQLocation(MapLocation location) {
		enemyHQLocation = location;
	}
	public static MapLocation getEnemyHQLocation() {
		return enemyHQLocation;
	}
	private static void setEnemyHQGuesserMode(int mode) {
		enemyHQGuesserMode = mode;
	}
	public static int getEnemyHQGuesserMode() {
		return enemyHQGuesserMode;
	}
	private static void setOurHQState(int state) {
		ourHQState = state;
	}
	public static int getOurHQState() {
		return ourHQState;
	}
	private static void setOurHQUnitCount(int designSchoolCount, int fulfillmentCenterCount) {
		SharedInfo.designSchoolCount = designSchoolCount;
		SharedInfo.fulfillmentCenterCount = fulfillmentCenterCount;
	}
	public static int getDesignSchoolCount() {
		return designSchoolCount;
	}
	public static int getFulfillmentCenterCount() {
		return fulfillmentCenterCount;
	}
	public static int getVaporatorCount() {
		return vaporatorCount;
	}
	public static int getMissingBuildingsCost() {
		return (SharedInfo.getDesignSchoolCount() == 0 ? RobotType.DESIGN_SCHOOL.cost : 0) +
				(SharedInfo.getFulfillmentCenterCount() == 0 ? RobotType.FULFILLMENT_CENTER.cost : 0) +
				(SharedInfo.isSavingForNetgun ? RobotType.NET_GUN.cost : 0);
	}
	private static void setAttackState(int attackState) {
		SharedInfo.attackState = attackState;
		if (DroneBot.isReadyForAttack) {
			// We should only rush if we're there
			Pathfinding.ignoreNetGuns = (attackState == ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS);
		}
		if (attackState == ATTACK_STATE_ENEMYHQ_IGNORE_NETGUNS) {
			dronesBuilt -= dronesReady;
			dronesReady = 0;
		}
	}
	public static int getAttackState() {
		return attackState;
	}
}
