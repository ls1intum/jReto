package de.tum.in.www1.jReto.module.util;

import java.util.Collection;
import java.util.UUID;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Connection;

/** A CompositeAdvertiser combines multiple Reto Advertisers into a single one. */
public class CompositeAdvertiser implements Advertiser {
	private Collection<Advertiser> advertisers;
	private Advertiser.Handler handler;
	private UUID localPeerIdentifier;
	private boolean isAdvertising = false;
	
	private Advertiser.Handler advertiserDelegate = new Advertiser.Handler() {
		public void onConnection(Advertiser advertiser, Connection connection) {
			CompositeAdvertiser.this.handleIncomingConnection(connection);
		}
		
		public void onAdvertisingStopped(Advertiser advertiser) {
			CompositeAdvertiser.this.advertiserStopped();
		}

		public void onAdvertisingStarted(Advertiser advertiser) {
			CompositeAdvertiser.this.advertiserStarted();
		}
	};
	
	public CompositeAdvertiser(UUID localPeerIdentifier, Collection<Advertiser> advertisers, Handler handler) {
		if (localPeerIdentifier == null) throw new IllegalArgumentException();
		if (advertisers == null) throw new IllegalArgumentException();
		if (handler == null) throw new IllegalArgumentException();
		
		this.localPeerIdentifier = localPeerIdentifier;
		this.advertisers = advertisers;
		this.handler = handler;
		
		for (Advertiser advertiser : advertisers) this.addAdvertiser(advertiser);
	}
	
	public void addAdvertiser(Advertiser advertiser) {
		this.advertisers.add(advertiser);
		advertiser.setAdvertiserHandler(this.advertiserDelegate);

		if (this.isAdvertising) advertiser.startAdvertisingWithPeerIdentifier(this.localPeerIdentifier);
	}
	
	public void removeAdvertiser(Advertiser advertiser) {
		if (!this.advertisers.contains(advertiser)) throw new IllegalArgumentException("Tried to remove an advertiser that was not previously added.");
		
		this.advertisers.remove(advertiser);
		advertiser.stopAdvertising();
	}
	
	public void stopAdvertising() {
		this.isAdvertising = false;
		
		for (Advertiser advertiser : advertisers) advertiser.stopAdvertising();
	}
	
	public void setAdvertiserHandler(Handler handler) {
		this.handler = handler;
	}
	
	public Handler getAdvertiserHandler() {
		return this.handler;
	}
	
	public UUID getLocalPeerIdentifier() {
		return this.localPeerIdentifier;
	}
	
	private void handleIncomingConnection(final Connection connection) {	
		this.handler.onConnection(this, connection);
	}
	
	private void advertiserStopped() {}
	private void advertiserStarted() {}

	@Override
	public boolean isAdvertising() {
		return this.isAdvertising;
	}

	@Override
	public void startAdvertisingWithPeerIdentifier(UUID identifier) {
		this.isAdvertising = true;
		
		for (Advertiser advertiser : advertisers) {
			advertiser.startAdvertisingWithPeerIdentifier(identifier);
		}
	}
}
