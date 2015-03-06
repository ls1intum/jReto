package jReto.module.dummy;

import java.util.UUID;

import de.tum.in.www1.jReto.module.api.Advertiser;
import jReto.util.RunLoop;

public class DummyAdvertiser implements Advertiser {
	private DummyNetworkInterface networkInterface;
	private RunLoop runloop;
	private boolean isAdvertising;
	private Advertiser.Handler handler;
	private UUID identifier;
	
	public DummyAdvertiser(DummyNetworkInterface networkInterface, RunLoop runloop) {
		this.networkInterface = networkInterface;
		this.runloop = runloop;
		this.isAdvertising = false;
	}

	@Override
	public void setAdvertiserHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public Handler getAdvertiserHandler() {
		return this.handler;
	}

	@Override
	public boolean isAdvertising() {
		return isAdvertising;
	}
	
	public UUID getIdentifier() {
		return this.identifier;
	}

	@Override
	public void startAdvertisingWithPeerIdentifier(UUID identifier) {
		this.identifier = identifier;
		this.isAdvertising = true;
		this.networkInterface.register(this);
		
		this.runloop.execute(new Runnable() {
			@Override
			public void run() {
				DummyAdvertiser.this.handler.onAdvertisingStarted(DummyAdvertiser.this);
			}
		});
	}

	@Override
	public void stopAdvertising() {
		this.isAdvertising = false;
		this.networkInterface.unregister(this);
		this.handler.onAdvertisingStopped(this);
	}
	
	public void onConnection(final DummyConnection connection) {
		this.runloop.execute(new Runnable() {
			@Override
			public void run() {
				DummyAdvertiser.this.handler.onConnection(DummyAdvertiser.this, connection);
			}
		});
	}
}
