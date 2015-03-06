package de.tum.in.www1.jReto.module.api;

import java.util.UUID;

/** A Browser attempts to discover other peers; it is the counterpart to the same module's advertiser. */
public interface Browser {
	/**
	* The Browser.Handler interface allows an implementation of the Browser protocol to inform it's delegate about various events.
	*/
	public static interface Handler {
	    /** Called when the Browser started to browse. */
		void onBrowsingStarted(Browser browser);
	    /** Called when the Browser stopped to browse. */
		void onBrowsingStopped(Browser browser, Object error);
	    /** Called when the Browser discovered an address. */
		void onAddressDiscovered(Browser browser, Address address, UUID identifier);
	    /** Called when the Browser lost an address, i.e. when that address becomes invalid for any reason. */
		void onAddressRemoved(Browser browser, Address address, UUID identifier);
	}
	
    /** Sets the Browser's delegate */
	void setBrowserHandler(Handler handler);
    /** The Browser's delegate */
	Handler getBrowserHandler();
	
    /** Whether the Browser is currently active. */
	boolean isBrowsing();
	
    /** Starts browsing for other peers. */
	void startBrowsing();
    /** Stops browsing for other peers. */
	void stopBrowsing();
}
