package queryReductor;

public class Pair<X,Y> {
	private final X first;
	private final Y second;
	
	public Pair(X x, Y y) {
		this.first = x;
		this.second = y;
	}
	
	public X getFirst() {
		return(first);
	}
	
	public Y getSecond() {
		return(second);
	}
	
	public Boolean isEqual(Pair<X,Y> P) {
		if (P.getFirst() == this.first) {
			if (P.getSecond() == this.second) {
				return true;
			}
		}
		return false;
	}
}
