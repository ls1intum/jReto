package de.tum.in.www1.jReto.module.wlan;

import java.net.InetAddress;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.niotools.Dispatcher;

public class WlanAddress implements Address {
	private Dispatcher dispatcher;
	private InetAddress address;
	private int port;
	
	public WlanAddress(Dispatcher dispatcher, InetAddress address, int port) {
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (address == null) throw new IllegalArgumentException("address may not be null");
		
		this.dispatcher = dispatcher;
		this.address = address;
		this.port = port;
	}
	
	@Override
	public Connection createConnection() {
		return new WlanConnection(this.dispatcher, this.address, this.port);
	}

	@Override
	public int getCost() {
		return 10;
	}
}
