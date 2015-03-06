package de.tum.in.www1.jReto.util;

import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.util.RetryableActionExecutor.RetryableAction;

/**
 * A StartStopHelper is a helper class for objects that have a "started" and "stopped" state, where both the transition to the "started" and "failed" state
 * (ie. starting and stopping something) may fail and should be retried. Can also be used when the object switches from the "started" to the "stopped" state unexpectedly, and the "started" state should be restored.
 * 
 * The user of this class should notify the StartStopHelper of state changes by calling onStart() and onStop().
 * */
public class StartStopHelper {
	/**
	 * Represents the desired states this class should help reach.
	 * */
	private static enum State {
		Started, 
		Stopped
	}
	
	/** A RetryableActionExecutor that attempts to exectute the start action */
	private final RetryableActionExecutor starter;
	/** A RetryableActionExecutor that attempts to exectute the stop action */
	private final RetryableActionExecutor stopper;
	
	/** The state that should be reached. 
	 * 
	 * E.g.: When the switching the desired state to the started state (by calling start), the StartStopHelper will call 
	 * the start action until it is notified about a successful start via onStart(). */
	private State desiredState = State.Stopped;
	
	/**
	 * Creates a new StartStopHelper.
	 * 
	 * @param startAction The start action
	 * @param stopAction The stop action
	 * @param timerSettings The timer settings used to retry the start and stop actions
	 * @param executor The executor to execute the start and stop action on.
	 * */
	public StartStopHelper(RetryableAction startAction, RetryableAction stopAction, Timer.BackoffTimerSettings timerSettings, Executor executor) {
		this.starter = new RetryableActionExecutor(startAction, timerSettings, executor);
		this.stopper = new RetryableActionExecutor(stopAction, timerSettings, executor);
	}
	
	/**
	 * Runs the startAction in delays until onStart is called.
	 * */
	public void start() {
		this.desiredState = State.Started;
		
		this.stopper.stop();
		this.starter.start();
	}
	/**
	 * Runs the startAction in delays until onStart is called.
	 * */
	public void stop() {
		this.desiredState = State.Stopped;
		
		this.starter.stop();
		this.stopper.start();
	}

	/**
	 * Call this method when the startAction succeeds, or a start occurs for another reason. Stops calling the start action. Starts calling the stop action if the stop() was called last (as opposed to start()).
	 * */
	public void onStart() {
		this.starter.stop();
		if (this.desiredState == State.Stopped) this.stopper.start();
	}
	/**
	 * Call this method when the stopAction succeeds, or a start occurs for another reason. Stops calling the stop action. Starts calling the start action if the start() was called last (as opposed to stop()).
	 * */
	public void onStop() {
		this.stopper.stop();
		if (this.desiredState == State.Started) this.starter.start();
	}
}
