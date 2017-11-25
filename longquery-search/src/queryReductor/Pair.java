package queryReductor;

import java.util.Comparator;

public class Pair {
	private final double value;
	private final int index;
	
	public Pair(double x, int y) {
		this.value = x;
		this.index = y;
	}
	
	public double getFirst() {
		return(value);
	}
	
	public int getSecond() {
		return(index);
	}
	
	public Boolean isEqual(Pair P) {
		if (P.getFirst() == this.value) {
			if (P.getSecond() == this.index) {
				return true;
			}
		}
		return false;
	}
}
