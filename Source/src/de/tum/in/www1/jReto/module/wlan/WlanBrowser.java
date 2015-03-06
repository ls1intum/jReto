package de.tum.in.www1.jReto.module.wlan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.niotools.Dispatcher;

public class WlanBrowser implements Browser, ServiceListener {
	private Browser.Handler handler;
	private Executor executor;
	private Dispatcher dispatcher;
	private boolean browsing;
	
	private JmDNS bonjourBrowser;

	private String networkType;
	private Map<String, WlanAddress> addresses;
	
	public WlanBrowser(Executor executor, Dispatcher dispatcher, String networkType) {
		if (executor == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (networkType == null) throw new IllegalArgumentException("networkType may not be null");
		
		this.executor = executor;
		this.dispatcher = dispatcher;
		this.networkType = networkType;
		
		this.addresses = new HashMap<String, WlanAddress>();
	}
	
	@Override
	public void setBrowserHandler(Browser.Handler handler) {
		this.handler = handler;
	}

	@Override
	public Browser.Handler getBrowserHandler() {
		return handler;
	}

	@Override
	public boolean isBrowsing() {
		return this.browsing;
	}

	@Override
	public void startBrowsing() {
		String serviceType = "_" + this.networkType + "wlan._tcp.local.";
		
		try {
			this.bonjourBrowser = JmDNS.create("RetoWlanBrowser");
			this.bonjourBrowser.addServiceListener(serviceType, this);
			this.browsing = true;		
			this.handler.onBrowsingStarted(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void stopBrowsing() {
		String serviceType = "_" + this.networkType + "wlan._tcp.local.";
		this.browsing = false;
		this.bonjourBrowser.removeServiceListener(serviceType, this);
		try {
			this.bonjourBrowser.close();
			
			this.handler.onBrowsingStopped(this, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			this.bonjourBrowser = null;
		}
	}
	
	@Override
	public void serviceResolved(ServiceEvent event) {			
		ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName());
		
		this.onDiscoveredService(info);
	}
	
	@Override
	public void serviceRemoved(final ServiceEvent event) {
		System.out.println("Removed service: " + event.getInfo().getName());
		this.executor.execute(new Runnable() {
			
			@Override
			public void run() {
				String identifier = event.getInfo().getName();
				
				WlanAddress address = WlanBrowser.this.addresses.get(identifier);
				WlanBrowser.this.addresses.remove(identifier);
				WlanBrowser.this.handler.onAddressRemoved(WlanBrowser.this, address, UUID.fromString(identifier));
			}
		});
	}
	
	@Override
	public void serviceAdded(ServiceEvent event) {
		this.bonjourBrowser.requestServiceInfo(event.getType(), event.getName(), 5000);
	}

	private void onDiscoveredService(final ServiceInfo info) {
		if (info.getInetAddresses() == null) {
			return;
		}
		final WlanAddress address = new WlanAddress(this.dispatcher, info.getInetAddresses()[0], info.getPort());
		this.addresses.put(info.getName(), address);
		
		this.executor.execute(new Runnable() {
			public void run() {
				WlanBrowser.this.handler.onAddressDiscovered(WlanBrowser.this, address, UUID.fromString(info.getName()));
			}
		});
	}
}
