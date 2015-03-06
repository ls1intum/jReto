package de.tum.in.www1.jReto.util;

import java.util.concurrent.Executor;

public class RetryableActionExecutor {
	/**
	 * A RetryableAction should encapsulate some action that might fail or succeed after a certain delay. If it fails, or does not succeed in a specified time interval, it can be retried.
	 * For example, one might try to establish a connection to a server that might not be online. The process should be retried if it fails after a certain amount of time.
	 * 
	 * You have to notify the RetryableActionExecutor about the success or failure of the action by calling onSuccess() or onFail().
 	 */
	public static interface RetryableAction {
		/**
		 * Runs the RetryableAction.
		 * 
		 * @param attemptNumber The number of times the action has been attempted.
		 * */
		void run(int attemptNumber);
	}
	
    /** The action to executed. */
	private final RetryableAction action;
    /** The executor that the action is executed on. */
	private final Executor executor;
    /** The timer settings used to create the timer that triggers a retry. */
	private final Timer.BackoffTimerSettings timerSettings;
    /** The timer used by the RetryableActionExecutor. */
	private Timer timer;
	
	/**
	 * Constructs a new RetryableActionExecutor.
	 * 
	 * @param action: The action that should be retried if it does not succeed in time or fails.
	 * @param timerSettings: Specifies the delay in which the action should be executed. 
	 * @param dispatchQueue: The dispatch queue on which actions should be executed.
	 */
	public RetryableActionExecutor(RetryableAction action, Timer.BackoffTimerSettings timerSettings, Executor executor) {
		this.action = action;
		this.timerSettings = timerSettings;
		this.executor = executor;
	}
	/**
	 * Starts the RetryableActionExecutor. The action is called immediately when calling start. A timer is created with the given settings that retries the action if it does not succeed in time.
     */
	public void start() {
		if (this.timer != null) return;
		
		this.timer = Timer.repeatWithBackoff(
				this.timerSettings.initialDelay, 
				this.timerSettings.backoffFactor, 
				this.timerSettings.maximumDelay, 
				this.executor, 
				new Timer.Action() {
			
			@Override
			public void run(Timer timer, int executionCount) {
				RetryableActionExecutor.this.action.run(executionCount + 1);
			}
		});
		
		RetryableActionExecutor.this.action.run(0);

		//this.executor.execute(new Runnable() {
		//	@Override
		//	public void run() {
		//	}
		//});
	}
    /**
     * Stops trying to execute the RetryableAction.
     */
	public void stop() {
		if (this.timer != null) this.timer.stop();
	}
    /**
     * Call this method when the RetryableAction succeeds. This method causes the executor to stop calling the action.
     */
	public void onSuccess() {
		this.stop();
	}
    /**
     * Call this method when the RetryableAction succeeds. This method causes the executor to stop calling the action.
     */
	public void onFail() {
		if (this.timer == null) { this.start(); }
	}
}
