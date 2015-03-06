package de.tum.in.www1.jReto.routing.managed_module;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.util.StartStopHelper;

/**
 * A ManagedBrowser automatically attempts to restart a Browser if starting the browser failed. The same concept applies to stopping the Browser.
 * */
public class ManagedBrowser implements Browser, Browser.Handler{
	private final Browser browser;
	private final StartStopHelper startStopHelper;
	
	private Browser.Handler handler;
	
	public ManagedBrowser(Browser browser, Executor executor) {
		this.browser = browser;
		this.browser.setBrowserHandler(this);
		
		this.startStopHelper = new StartStopHelper(
			attemptNumber -> attemptStart(attemptNumber), 
			attemptNumber -> attemptStop(attemptNumber), 
			ManagedModule.DEFAULT_TIMER_SETTINGS, 
			executor
		);
	}
	
	private void attemptStart(int attemptNumber) {
		if (attemptNumber > 1) System.out.println(new Date()+": Retrying to start browser (attempt "+attemptNumber+"): "+ this.browser);
		
		this.browser.startBrowsing();
	}
	private void attemptStop(int attemptNumber) {
		if (attemptNumber > 1) System.out.println(new Date()+": Retrying to stop browser (attempt "+attemptNumber+"): "+ this.browser);
		
		this.browser.stopBrowsing();
	}
	
	@Override
	public void onBrowsingStarted(Browser browser) {
		this.startStopHelper.onStart();
		this.handler.onBrowsingStarted(this);
	}

	@Override
	public void onBrowsingStopped(Browser browser, Object error) {
		this.startStopHelper.onStop();
		this.handler.onBrowsingStopped(this, error);
	}

	@Override
	public void onAddressDiscovered(Browser browser, Address address, UUID identifier) {
		this.handler.onAddressDiscovered(this, address, identifier);
	}

	@Override
	public void onAddressRemoved(Browser browser, Address address, UUID identifier) {
		this.handler.onAddressRemoved(this, address, identifier);
	}

	@Override
	public void setBrowserHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public Handler getBrowserHandler() {
		return this.handler;
	}

	@Override
	public boolean isBrowsing() {
		return this.browser.isBrowsing();
	}

	@Override
	public void startBrowsing() {
		this.startStopHelper.start();
	}

	@Override
	public void stopBrowsing() {
		this.startStopHelper.stop();
	}
}
