package citricsky.battlecode2020.util;
import battlecode.common.MapLocation;

public class FastMapLocationDeque {
	private MapLocation[] array;
	private int index;
	private int size;
	public FastMapLocationDeque(int arraySize) {
		this.array = new MapLocation[arraySize];
	}
	public void push(MapLocation i) {
		array[(index + (size++)) % array.length] = i;
	}
	public MapLocation poll() {
		size--;
		return array[(index++) % array.length];
	}
	public boolean isEmpty() {
		return size == 0;
	}
	public void reset() {
		size = 0;
	}
	public MapLocation peek() {
        return array[index % array.length];
    }
	public boolean contains(MapLocation location) {
		for(int i = index; i < index + size; i++) {
			if(array[i % array.length].equals(location)) {
				return true;
			}
		}
		return false;
	}
	public int size() {
		return size;
	}
}

