package citricsky.battlecode2020.util;
import battlecode.common.MapLocation;

public class MapLocationArray {
	private MapLocation[] array;
	private int size;
	private int maxSize;
	
	public MapLocationArray(int arraySize) {
		this.array = new MapLocation[arraySize];
		this.size = 0;
		this.maxSize = 0;
	}
	public boolean isEmpty() {
		return size == 0;
	}
	public void add(MapLocation location) {
		for (int i = array.length; --i >= 0;) {
			if (array[i] == null) {
				array[i] = location;
				size++;
				maxSize = Math.max(maxSize, size);
				break;
			}
		}
	}
	public void remove(MapLocation location) {
		int x = array.length - maxSize;
		for (int i = array.length; --i >= x;) {
			if (location.equals(array[i])) {
				array[i] = null;
				size--;
				break;
			}
		}
	}
	public boolean contains(MapLocation location) {
		int x = array.length - maxSize;
		for (int i = array.length; --i >= x;) {
			if (location.equals(array[i])) {
				return true;
			}
		}
		return false;
	}
	public MapLocation nearestSoup(MapLocation currentLocation) {
		MapLocation nearest = null;
		int smallestDistance = Integer.MAX_VALUE;
		int x = array.length - maxSize;
		for (int i = array.length; --i >= x;) {
			if (array[i] != null) {
				int distanceSquared = currentLocation.distanceSquaredTo(array[i]);
				if (distanceSquared < smallestDistance) {
					nearest = array[i];
					smallestDistance = distanceSquared;
				}
			}
		}
		return nearest;
	}
}
