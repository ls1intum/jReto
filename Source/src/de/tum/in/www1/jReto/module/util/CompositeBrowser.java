package de.tum.in.www1.jReto.module.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Browser;

/** A CompositeBrowser combines multiple Reto Browsers into a single one. */
public class CompositeBrowser implements Browser {
	private Browser.Handler handler;
	
	private Collection<Browser> browsers;
	private boolean isBrowsing;
	
	public CompositeBrowser(Collection<Browser> browsers, Handler handler) {
		this.browsers = new HashSet<Browser>();
		this.isBrowsing = false;
		this.handler = handler;
		
		for (Browser browser : browsers) this.addBrowser(browser);
	}
	
	public void addBrowser(Browser browser) {
		this.browsers.add(browser);
		browser.setBrowserHandler(new Browser.Handler() {
			@Override
			public void onAddressRemoved(Browser browser, Address address, UUID identifier) {
				CompositeBrowser.this.handler.onAddressRemoved(CompositeBrowser.this, address, identifier);
			}
			
			@Override
			public void onAddressDiscovered(Browser browser, Address address, UUID identifier) {
				CompositeBrowser.this.handler.onAddressDiscovered(CompositeBrowser.this, address, identifier);
			}
			
			@Override
			public void onBrowsingStopped(Browser browser, Object error) {}
			@Override
			public void onBrowsingStarted(Browser browser) {}
		});
		
		if (this.isBrowsing) browser.startBrowsing();
	}
	
	public void removeBrowser(Browser browser) {
		if (!this.browsers.contains(browser)) throw new IllegalArgumentException("Attempted to remove a browser that was not added previously.");
		
		this.browsers.remove(browser);
		browser.stopBrowsing();
	}
	
	public void setBrowserHandler(Handler handler) {
		this.handler = handler;
	}
	
	public Handler getBrowserHandler() {
		return this.handler;
	}
	
	public void startBrowsing() {
		this.isBrowsing = true;
		
		for (Browser browser : this.browsers) browser.startBrowsing();
		
		this.handler.onBrowsingStarted(this);
	}

	public void stopBrowsing() {
		this.isBrowsing = false;

		for (Browser browser : this.browsers) browser.stopBrowsing();
		
		this.handler.onBrowsingStopped(this, null);
	}

	@Override
	public boolean isBrowsing() {
		return this.isBrowsing;
	}
}
