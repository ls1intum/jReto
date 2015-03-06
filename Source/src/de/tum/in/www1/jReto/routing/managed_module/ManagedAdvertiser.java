package de.tum.in.www1.jReto.routing.managed_module;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.util.StartStopHelper;

/**
 * A ManagedAdvertiser automatically attempts to restart an Advertiser if starting the advertiser failed. The same concept applies to stopping the advertiser.
 * */
public class ManagedAdvertiser implements Advertiser, Advertiser.Handler {	
	private final Advertiser advertiser;
	private final StartStopHelper startStopHelper;
	private UUID advertisedUuid;
	
	private Advertiser.Handler handler;
	
	public ManagedAdvertiser(Advertiser advertiser, Executor executor) {
		this.advertiser = advertiser;
		
		this.advertiser.setAdvertiserHandler(this);
		
		this.startStopHelper = new StartStopHelper(
			attemptNumber -> attemptStart(attemptNumber), 
			attemptNumber -> attemptStop(attemptNumber), 
			ManagedModule.DEFAULT_TIMER_SETTINGS, 
			executor
		);
	}
	
	private void attemptStart(int attemptNumber) {
		if (attemptNumber > 1) System.out.println(new Date()+": Retrying to start advertiser (attempt #"+attemptNumber+": "+ this.advertiser);
		
		if (this.advertisedUuid != null) this.advertiser.startAdvertisingWithPeerIdentifier(this.advertisedUuid);
	}
	private void attemptStop(int attemptNumber) {
		if (attemptNumber > 1) System.out.println(new Date()+": Retrying to stop advertiser (attempt #"+attemptNumber+": "+ this.advertiser);
		
		this.advertiser.stopAdvertising();
	}
	
	@Override
	public void onAdvertisingStarted(Advertiser advertiser) {
		this.startStopHelper.onStart();
		
		this.handler.onAdvertisingStarted(this);
	}

	@Override
	public void onAdvertisingStopped(Advertiser advertiser) {
		this.startStopHelper.onStop();
		
		this.handler.onAdvertisingStopped(this);
	}

	@Override
	public void onConnection(Advertiser advertiser, Connection connection) {
		this.handler.onConnection(this, connection);
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
		return this.advertiser.isAdvertising();
	}

	@Override
	public void startAdvertisingWithPeerIdentifier(UUID identifier) {
		this.advertisedUuid = identifier;
		this.startStopHelper.start();
	}

	@Override
	public void stopAdvertising() {
		this.startStopHelper.stop();
	}
}
