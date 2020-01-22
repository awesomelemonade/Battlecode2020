package citricsky.battlecode2020.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MapTracker {
	private RobotController controller;
	
	public MapLocationArray soupLocations = new MapLocationArray(100);
	public MapLocationArray waterLocations = new MapLocationArray(100);
	//public static MapLocationArray enemyLocations = new MapLocationArray(100);
	
	public MapTracker(RobotController controller) {
		this.controller = controller;
	}
	
	//loop every round and update adjacent locations
	public void updateLocations() throws GameActionException {
		for (Direction direction : Util.ADJACENT_DIRECTIONS) {
			MapLocation adjacentLocation = Cache.CURRENT_LOCATION.add(direction);
			if (controller.canSenseLocation(adjacentLocation)) {
				//if there is soup, add to array if not already
				if (controller.senseSoup(adjacentLocation) > 0) {
					if (!soupLocations.contains(adjacentLocation)) {
						soupLocations.add(adjacentLocation);
					}
				}//if no soup, remove from array if exists
				else {
					if (soupLocations.contains(adjacentLocation)) {
							soupLocations.remove(adjacentLocation);
					}
				}
				
				//if there is water, add to array if not already
				if (controller.senseFlooding(adjacentLocation)) {
					if (!waterLocations.contains(adjacentLocation)) {
						waterLocations.add(adjacentLocation);
					}
				}//if no water, remove from array if exists
				else {
					if (waterLocations.contains(adjacentLocation)) {
						waterLocations.remove(adjacentLocation);
					}
				}
				
				/*//if there is an enemy
				if (controller.senseRobotAtLocation(adjacentLocation).getTeam() == Cache.OPPONENT_TEAM) {
					if(!enemyLocations.contains(adjacentLocation)) {
						enemyLocations.add(adjacentLocation);
					}
				} //if no enemy, remove from array if exists
				else {
					if(enemyLocations.contains(adjacentLocation)) {
						enemyLocations.remove(adjacentLocation);
					}
				}*/
			}
		}
	}
	
}
