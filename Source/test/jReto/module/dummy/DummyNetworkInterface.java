package jReto.module.dummy;

import jReto.util.RunLoop;

import java.util.HashSet;
import java.util.Set;

public class DummyNetworkInterface {
	String interfaceName;
	Set<DummyBrowser> browsers;
	Set<DummyAdvertiser> advertisers;
	RunLoop runloop;
	private int recommendedPacketSize;
	private int priority;
	
	public DummyNetworkInterface(String interfaceName, RunLoop runloop, int recommendedPacketSize, int priority) {
		this.interfaceName = interfaceName;
		this.runloop = runloop;
		this.recommendedPacketSize = recommendedPacketSize;
		this.priority = priority;
		this.browsers = new HashSet<DummyBrowser>();
		this.advertisers = new HashSet<DummyAdvertiser>();
	}
	
	public void registerBrowser(final DummyBrowser browser) {
		this.browsers.add(browser);
		for (DummyAdvertiser advertiser : this.advertisers) notifyAddPeer(browser, advertiser);
	}
	
	public void unregisterBrowser(final DummyBrowser browser) {
		this.browsers.remove(browser);
		for (DummyAdvertiser advertiser : this.advertisers) notifyRemovePeer(browser, advertiser);
	}
	
	public void register(final DummyAdvertiser advertiser) {
		this.advertisers.add(advertiser);
		for (DummyBrowser browser : this.browsers) notifyAddPeer(browser, advertiser);
	}
	
	public void unregister(final DummyAdvertiser advertiser) {
		this.advertisers.remove(advertiser);
		for (DummyBrowser browser : this.browsers) notifyRemovePeer(browser, advertiser);
	}
	
	void notifyAddPeer(final DummyBrowser browser, final DummyAdvertiser advertiser) {
		runloop.execute(new Runnable() {
			@Override
			public void run() {
				browser.onAddPeer(advertiser.getIdentifier(), new DummyAddress(DummyNetworkInterface.this, advertiser, runloop));
			}
		});
	}
	void notifyRemovePeer(final DummyBrowser browser, final DummyAdvertiser advertiser) {
		runloop.execute(new Runnable() {
			@Override
			public void run() {
				browser.onRemovePeer(advertiser.getIdentifier());
			}
		});
	}

	public int getRecommendedPacketSize() {
		return this.recommendedPacketSize;
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public String getInterfaceName() {
		return this.interfaceName;
	}
	
	public static interface DummyConnectionCreatedHook {
		void createdConnection(DummyAddress address, DummyConnection connection);
	}
	public DummyConnectionCreatedHook connectionCreatedHook;
	
	public static interface DummyConnectionClosedHook {
		void closedConnections(DummyAddress address, DummyConnection outConnection, DummyConnection inConnection);
	}
	public DummyConnectionClosedHook connectionClosedHook;
}
