package de.tum.in.www1.jReto.util;

public class CountDown {
	private int counter;
	private Runnable runnable;
	
	public CountDown(int target, Runnable runnable) {
		this.counter = target;
		this.runnable = runnable;
	}
	
	public void countDown() {
		if (this.counter == 0) throw new IllegalStateException("Tried to count down below 0");
		
		this.counter--;
		
		if (this.counter == 0) this.runnable.run();
	}
}
