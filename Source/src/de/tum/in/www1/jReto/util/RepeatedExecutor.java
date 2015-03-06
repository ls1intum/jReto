package de.tum.in.www1.jReto.util;

import java.util.concurrent.Executor;

/**
 * A RepeatedExecutor executes an action repeatedly with a certain delay. On demand, the action can also be triggered immediately or after a short delay.
 * */
public class RepeatedExecutor {
	private final Runnable action;
	private final double regularDelay;
	private final double shortDelay;
	private final Executor executor;
	
	private boolean isStarted = false;
	private Timer timer;
	
	/**
	 * Constructs a new RepeatableExecutor.
	 * 
	 * @param action The action to execute.
	 * @param regularDelay The delay in which the action is executed by default.
	 * @param shortDelay The delay used when runActionInShortDelay is called.
	 * @param executor The executor to execute the action with.
	 * */
	public RepeatedExecutor(Runnable action, double regularDelay, double shortDelay, Executor executor) {
		this.action = action;
		this.regularDelay = regularDelay;
		this.shortDelay = shortDelay;
		this.executor = executor;
	}
	
	/**
	 * Starts executing the action in regular delays.
	 * */
	public void start() {
		if (this.isStarted) return;
		
		this.isStarted = true;
		this.resume();
	}
	/**
	 * Stops executing the action in regular delays.
	 * */
	public void stop() {
		if (!this.isStarted) return;
		
		this.isStarted = false;
		this.interrupt();
	}
	
	/**
	 * Runs the action immediately. Resets the timer; the next execution of the action will occur after the regular delay.
	 * */
	public void runActionNow() {
		this.resetTimer();
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				RepeatedExecutor.this.action.run();
			}
		});
	}
	/**
	 * Runs the action after the short delay. After this, actions are executed in regular intervals again.
	 * */
	public void runActionInShortDelay() {
		this.interrupt();
		this.timer = Timer.delay(this.shortDelay, this.executor, new Runnable() {
			@Override
			public void run() {
				RepeatedExecutor.this.action.run();
				RepeatedExecutor.this.resume();
			}
		});
	}

	private void interrupt() {
		if (this.timer != null) this.timer.stop();
		this.timer = null;
	}
	private void resume() {
		if (!this.isStarted) return;

		this.timer = Timer.repeat(this.regularDelay, this.executor, new Timer.Action() {
			@Override
			public void run(Timer timer, int executionCount) {
				RepeatedExecutor.this.action.run();
			}
		});
	}
	private void resetTimer() {
		this.interrupt();
		this.resume();
	}
}
