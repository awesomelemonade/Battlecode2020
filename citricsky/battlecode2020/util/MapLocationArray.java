package citricsky.battlecode2020.util;
import battlecode.common.MapLocation;

public class MapLocationArray {
	private MapLocation[] array;
	private int size;
	
	public MapLocationArray(int arraySize) {
		this.array = new MapLocation[arraySize];
		size = 0;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public boolean isFull() {
		return size == array.length;
	}
	
	public void add(MapLocation location) {
		if(!isFull()) {
			for(int i = 0; i < array.length; i ++) {
				if(array[i] == null) {
					array[i] = location;
					size++;
					break;
				}
			}
		}
	}
	
	public void remove(MapLocation location) {
		for(int i = 0; i < array.length; i++) {
			if(location.equals(array[i])) {
				array[i] = null;
				size--;
				break;
			}
		}
	}
	
	public boolean contains(MapLocation location) {
		for(int i = 0; i < array.length; i ++) {
			if(location.equals(array[i])) {
				return true;
			}
		}
		return false;
	}
	
	public MapLocation nearestSoup(MapLocation currentLocation) {
		MapLocation nearest = null;
		int smallestDistance = 0;
		for(int i = 0; i < array.length; i++) {
			if(array[i] != null) {
				if(nearest == null || currentLocation.distanceSquaredTo(array[i]) < smallestDistance) {
					nearest = array[i];
					smallestDistance = currentLocation.distanceSquaredTo(array[i]);
				}
			}
		}
		return nearest;
	}
	
	
}
