package jReto.util;

public class OrderVerifier {
	private int current = 0;
	
	public void check(int value) {
		if (value < current) throw new IllegalArgumentException("Wrong order ("+current+" called before "+value+")");
		
		this.current = value;
	}
}
