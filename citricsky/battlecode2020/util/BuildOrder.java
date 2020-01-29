package citricsky.battlecode2020.util;

import battlecode.common.RobotType;
import citricsky.battlecode2020.HQBot;

public class BuildOrder {
	public static final int MASS_SPAWN_VAPORATOR_THRESHOLD = 15;
	public static RobotType getNextRobotType() {
		if (SharedInfo.isSavingForNetgun) {
			return RobotType.NET_GUN;
		}
		switch (SharedInfo.getOurHQState()) {
			case HQBot.NO_ADDITIONAL_HELP_NEEDED:
				return RobotType.DELIVERY_DRONE;
			case HQBot.NEEDS_HELP:
				if (SharedInfo.landscapersBuilt >= 10 && SharedInfo.totalDronesBuilt == 0) {
					return RobotType.DELIVERY_DRONE;
				} else {
					return RobotType.LANDSCAPER;
				}
		}
		if (SharedInfo.wallState == SharedInfo.WALL_STATE_NEEDS) {
			return RobotType.LANDSCAPER;
		}
		if (SharedInfo.landscapersBuilt < 2) {
			// Includes Design School
			return RobotType.LANDSCAPER;
		}
		if (SharedInfo.getVaporatorCount() < 3) {
			return RobotType.VAPORATOR;
		}
		if (SharedInfo.landscapersBuilt < 4) {
			return RobotType.LANDSCAPER;
		}
		if (SharedInfo.totalDronesBuilt < 3) {
			return RobotType.DELIVERY_DRONE;
		}
		if (SharedInfo.getVaporatorCount() < MASS_SPAWN_VAPORATOR_THRESHOLD) {
			return RobotType.VAPORATOR;
		}
		// Mass create
		// Alternate between landscapers and drones based on totalDronesBuilt and landscapersBuilt
		if (SharedInfo.landscapersBuilt <= SharedInfo.totalDronesBuilt) {
			return RobotType.LANDSCAPER;
		} else {
			return RobotType.DELIVERY_DRONE;
		}
	}
	public static int getSoupThreshold(RobotType type) {
		int threshold = 0;
		if (SharedInfo.isSavingForNetgun) {
			threshold += RobotType.NET_GUN.cost;
			if (type == RobotType.NET_GUN) {
				return threshold;
			}
		}
		boolean addedDroneCost = false;
		boolean addedLandscaperCost = false;
		boolean addedVaporatorCost = false;
		switch (SharedInfo.getOurHQState()) {
			case HQBot.NO_ADDITIONAL_HELP_NEEDED:
				if (!addedDroneCost) {
					threshold += RobotType.DELIVERY_DRONE.cost;
					addedDroneCost = true;
				}
				if (type == RobotType.DELIVERY_DRONE) {
					return threshold;
				}
				break;
			case HQBot.NEEDS_HELP:
				if (SharedInfo.landscapersBuilt >= 10 && SharedInfo.totalDronesBuilt == 0) {
					if (!addedDroneCost) {
						threshold += RobotType.DELIVERY_DRONE.cost;
						addedDroneCost = true;
					}
					if (type == RobotType.DELIVERY_DRONE) {
						return threshold;
					}
				} else {
					if (!addedLandscaperCost) {
						threshold += RobotType.LANDSCAPER.cost;
						addedLandscaperCost = true;
					}
					if (type == RobotType.LANDSCAPER) {
						return threshold;
					}
				}
				break;
		}
		if (SharedInfo.wallState == SharedInfo.WALL_STATE_NEEDS) {
			if (!addedLandscaperCost) {
				threshold += RobotType.LANDSCAPER.cost;
				addedLandscaperCost = true;
			}
			if (type == RobotType.LANDSCAPER) {
				return threshold;
			}
		}
		if (SharedInfo.landscapersBuilt < 2) {
			// Includes Design School
			if (!addedLandscaperCost) {
				threshold += RobotType.LANDSCAPER.cost;
				addedLandscaperCost = true;
			}
			if (type == RobotType.LANDSCAPER) {
				return threshold;
			}
		}
		if (SharedInfo.getVaporatorCount() < 3) {
			if (!addedVaporatorCost) {
				threshold += RobotType.VAPORATOR.cost;
				addedVaporatorCost = true;
			}
			if (type == RobotType.VAPORATOR) {
				return threshold;
			}
		}
		if (SharedInfo.landscapersBuilt < 4) {
			if (!addedLandscaperCost) {
				threshold += RobotType.LANDSCAPER.cost;
				addedLandscaperCost = true;
			}
			if (type == RobotType.LANDSCAPER) {
				return threshold;
			}
		}
		if (SharedInfo.totalDronesBuilt < 3) {
			if (!addedDroneCost) {
				threshold += RobotType.DELIVERY_DRONE.cost;
				addedDroneCost = true;
			}
			if (type == RobotType.DELIVERY_DRONE) {
				return threshold;
			}
		}
		if (SharedInfo.getVaporatorCount() < MASS_SPAWN_VAPORATOR_THRESHOLD) {
			if (!addedVaporatorCost) {
				threshold += RobotType.VAPORATOR.cost;
				addedVaporatorCost = true;
			}
			if (type == RobotType.VAPORATOR) {
				return threshold;
			}
		}
		// Mass create
		// Alternate between landscapers and drones based on totalDronesBuilt and landscapersBuilt
		if (SharedInfo.landscapersBuilt <= SharedInfo.totalDronesBuilt) {
			if (!addedLandscaperCost) {
				threshold += RobotType.LANDSCAPER.cost;
				addedLandscaperCost = true;
			}
			if (type == RobotType.LANDSCAPER) {
				return threshold;
			}
		} else {
			if (!addedDroneCost) {
				threshold += RobotType.DELIVERY_DRONE.cost;
				addedDroneCost = true;
			}
			if (type == RobotType.DELIVERY_DRONE) {
				return threshold;
			}
		}
		// We must have so much money that we can build more vaporators
		if (SharedInfo.getVaporatorCount() < 120) {
			// Don't make more vaporators if we have a ton of money already
			if (Cache.controller.getTeamSoup() < 800) {
				if (!addedVaporatorCost) {
					threshold += RobotType.VAPORATOR.cost;
					addedVaporatorCost = true;
				}
				if (type == RobotType.VAPORATOR) {
					return threshold;
				}
			}
		}
		// mooooore landscapers
		if (!addedLandscaperCost) {
			threshold += RobotType.LANDSCAPER.cost;
			addedLandscaperCost = true;
		}
		if (type == RobotType.LANDSCAPER) {
			return threshold;
		}
		// mooooooore drones
		if (!addedDroneCost) {
			threshold += RobotType.DELIVERY_DRONE.cost;
			addedDroneCost = true;
		}
		if (type == RobotType.DELIVERY_DRONE) {
			return threshold;
		}
		// ?????????
		System.out.println("Why are you trying to build " + type + "?");
		return threshold;
	}
}
