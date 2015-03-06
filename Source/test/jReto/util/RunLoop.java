package jReto.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class RunLoop implements Executor {
	private BlockingQueue<Runnable> queue;
	private boolean running;
	private boolean executeImmediately;
	
	public RunLoop(boolean executeImmediately) {
		this.queue = new LinkedBlockingQueue<>();
		this.running = true;
		this.executeImmediately = executeImmediately;
	}
	
	public void execute(Runnable runnable) {
		if (this.executeImmediately) {
			runnable.run();
		} else {
			this.queue.add(runnable);
		}
	}
	
	public void start() {
		while (this.running) {
			try {
				this.queue.take().run();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		this.queue.add(new Runnable() {
			
			@Override
			public void run() {
				RunLoop.this.running = false;
			}
		});
	}
}
