package de.tum.in.www1.jReto.module.remoteP2P;

import java.net.URI;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Connection;

public class RemoteP2PAddress implements Address {
	private final Executor executor;
	private final URI requestConnectionUri;
	
	public RemoteP2PAddress(Executor executor, URI requestConnectionUri) {
		this.executor = executor;
		this.requestConnectionUri = requestConnectionUri;
	}
	
	@Override
	public Connection createConnection() {
		return new RemoteP2PConnection(executor, this.requestConnectionUri, true);
	}
	@Override
	public int getCost() {
		return 50;
	}
}
