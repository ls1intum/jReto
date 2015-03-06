package de.tum.in.www1.jReto.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {
	public static interface Action {
		public void run(Timer timer, int executionCount);
	}
	public static interface DelayGenerator {
		public double getDelay(int executionCount);
	}
	
	private static class ConstantDelayGenerator implements DelayGenerator {
		private final double delay;
		
		public ConstantDelayGenerator(double delay) {
			this.delay = delay;
		}
		
		@Override
		public double getDelay(int executionCount) {
			return delay;
		}
	}
	private static class BackOffDelayGenerator implements DelayGenerator {
		private final double initialDelay;
		private final double backOffFactor;
		private final double maximumDelay;
		
		public BackOffDelayGenerator(double initialDelay, double backOffFactor, double maximumDelay) {
			this.initialDelay = initialDelay;
			this.backOffFactor = backOffFactor;
			this.maximumDelay = maximumDelay;
		}
		
		@Override
		public double getDelay(int executionCount) {
			double delay = this.initialDelay * Math.pow(backOffFactor, executionCount);
			
			return Math.min(delay, this.maximumDelay);
		}
	}
	
	public static Timer delay(final double delay, Executor executor, final Runnable action) {
		return new Timer(new Timer.Action() {
			@Override
			public void run(Timer timer, int executionCount) {
				action.run();
			}
		}, new ConstantDelayGenerator(delay), 1, executor);
	}
	public static Timer repeat(final double interval, Executor executor, Timer.Action action) {
		return new Timer(action, new ConstantDelayGenerator(interval), executor);
	}
	public static Timer repeat(final double interval, int maximumExecutionCount, Executor executor, Timer.Action action) {
		return new Timer(action, new ConstantDelayGenerator(interval), maximumExecutionCount, executor);
	}
	public static Timer repeatWithBackoff(double initialDelay, double backOffFactor, double maximumDelay, Executor executor, Action action) {
		return new Timer(action, new BackOffDelayGenerator(initialDelay, backOffFactor, maximumDelay), executor);
	}
	public static Timer repeatWithBackoff(double initialDelay, double backOffFactor, double maximumDelay, int maximumExecutionCount, Executor executor, Action action) {
		return new Timer(action, new BackOffDelayGenerator(initialDelay, backOffFactor, maximumDelay), maximumExecutionCount, executor);
	}
	public static class BackoffTimerSettings {
		public final double initialDelay;
		public final double backoffFactor;
		public final double maximumDelay;
		
		public BackoffTimerSettings(double initialDelay, double backoffFactor, double maximumDelay) {
			this.initialDelay = initialDelay;
			this.backoffFactor = backoffFactor;
			this.maximumDelay = maximumDelay;
		}
	}
	public static Timer repeatWithBackoff(BackoffTimerSettings settings, Executor executor, Action action) {
		return Timer.repeatWithBackoff(settings.initialDelay, settings.backoffFactor, settings.maximumDelay, executor, action);
	}
	public static Timer repeat(DelayGenerator delayGenerator, Executor executor, Action action) {
		return new Timer(action, delayGenerator, executor);
	}
	public static Timer repeat(DelayGenerator delayGenerator, Executor executor, int maximumExecutionCount, Action action) {
		return new Timer(action, delayGenerator, maximumExecutionCount, executor);
	}
	
	private static ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private final Executor executor;
	private final Timer.Action action;
	private final Timer.DelayGenerator delayGenerator;
	private final int maximumExecutions;
	private final boolean limitNumberOfExecutions;
	
	private int currentExecutionCount = 0;
	private boolean isDone = false;
	
	private Timer(Timer.Action action, Timer.DelayGenerator delayGenerator, boolean limitNumberOfExecutions, int maximumExecutionCount, Executor executor) {
		this.action = action;
		this.delayGenerator = delayGenerator;
		this.maximumExecutions = maximumExecutionCount;
		this.limitNumberOfExecutions = limitNumberOfExecutions;
		this.executor = executor;
		
		this.startTimer();
	}
	private Timer(Timer.Action action, Timer.DelayGenerator delayGenerator, int maximumExecutionCount, Executor executor) {
		this(action, delayGenerator, true, maximumExecutionCount, executor);
	}
	private Timer(Timer.Action action, Timer.DelayGenerator delayGenerator, Executor executor) {
		this(action, delayGenerator, false, 0, executor);
	}
	
	public void stop() {
		this.isDone = true;
	}
	public void fire() {
		if (this.isDone) return;
		Timer.this.action.run(Timer.this, Timer.this.currentExecutionCount);
		
		this.currentExecutionCount++;
		if (this.limitNumberOfExecutions && this.currentExecutionCount >= this.maximumExecutions) {
			this.isDone = true;
		}
		
		if (!this.isDone) {
			this.startTimer();
		}
	}
	private void startTimer() {
		long delay = (long)(this.delayGenerator.getDelay(this.currentExecutionCount) * 1000);

		Timer.timerExecutor.schedule(() -> Timer.this.executor.execute(() -> Timer.this.fire()), delay, TimeUnit.MILLISECONDS);
	}
}
