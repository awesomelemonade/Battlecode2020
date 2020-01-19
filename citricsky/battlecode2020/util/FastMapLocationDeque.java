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
	public MapLocation get(int index) {
		return array[(this.index + index) % array.length];
	}
	public int size() {
		return size;
	}
}

