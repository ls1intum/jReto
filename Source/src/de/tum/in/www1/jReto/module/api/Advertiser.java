package de.tum.in.www1.jReto.module.api;

import java.util.UUID;

public interface Advertiser {
	/**
	* The Advertiser.Handler interface allows the Advertiser to inform its delegate about various events.
	*/
	public static interface Handler {
	    /** Called when the advertiser started advertising. */
		void onAdvertisingStarted(Advertiser advertiser);
	    /** Called when the advertiser stopped advertising. */
		void onAdvertisingStopped(Advertiser advertiser);
	    /** Called when the advertiser received an incoming connection from a remote peer. */
		void onConnection(Advertiser advertiser, Connection connection);
	}
	
	/** Sets the advertiser's delegate. */
	void setAdvertiserHandler(Handler handler);
	/** Returns the advertiser's delegate. */
	Handler getAdvertiserHandler();
    /** Whether the advertiser is currently active. */
	boolean isAdvertising();
    /** 
    * Starts advertising.
    * @param identifier A UUID identifying the local peer.
    */
	void startAdvertisingWithPeerIdentifier(UUID identifier);
    /**
    * Stops advertising.
    */
	void stopAdvertising();
}
