package jReto.module.dummy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.tum.in.www1.jReto.module.api.Browser;
import jReto.util.RunLoop;

public class DummyBrowser implements Browser {
	private DummyNetworkInterface networkInterface;
	private RunLoop runloop;
	private Browser.Handler handler;
	private boolean isBrowsing;
	private Map<UUID, DummyAddress> addresses;
	
	public DummyBrowser(DummyNetworkInterface networkInterface, RunLoop runloop) {
		this.networkInterface = networkInterface;
		this.runloop = runloop;
		this.isBrowsing = false;
		this.addresses = new HashMap<UUID, DummyAddress>();
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
		return this.isBrowsing;
	}

	@Override
	public void startBrowsing() {
		this.networkInterface.registerBrowser(this);
		this.isBrowsing = true;
		this.runloop.execute(new Runnable() {
			
			@Override
			public void run() {
				handler.onBrowsingStarted(DummyBrowser.this);
			}
		});
	}

	@Override
	public void stopBrowsing() {
		this.networkInterface.unregisterBrowser(this);
		this.isBrowsing = false;
		
		this.runloop.execute(new Runnable() {
			
			@Override
			public void run() {
				handler.onBrowsingStopped(DummyBrowser.this, null);
			}
		});	
	}
	
	void onAddPeer(UUID identifier, DummyAddress address) {
		this.addresses.put(identifier, address);
		this.handler.onAddressDiscovered(this, address, identifier);
	}
	void onRemovePeer(UUID identifier) {
		this.handler.onAddressRemoved(this, this.addresses.get(identifier), identifier);
		this.addresses.remove(identifier);
	}
}
