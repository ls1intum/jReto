package de.tum.in.www1.jReto.module.remoteP2P;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;


import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.module.api.Module;

/**
 * Using a RemoteP2PModule with the LocalPeer allows it to discover and connect with other peers over the internet using a RemoteP2P server.
 * 
 * To use this module, you need to first deploy the RemoteP2P server (it can be found in the RemoteP2P directory in Reto's repository).
 * 
 * Besides that, if you wish to use the RemoteP2P module, all you need to do is construct an instance and pass it to the LocalPeer either in the constructor or using the addModule method.
 * */
public class RemoteP2PModule implements Module, Advertiser, Browser {
	private Executor executor;
	private Session webSocket;
	private UUID localPeerIdentifier;
	private final URI baseServerUri;
	private Browser.Handler browserHandler;
	private Advertiser.Handler advertiserHandler;
	private boolean isAdvertising = false;
	private boolean isBrowsing = false;
	private boolean startedDiscoverySocket = false;
	
	private Map<UUID, Address> addresses = new HashMap<>();
	
	public RemoteP2PModule(URI baseURI) {
		this.baseServerUri = baseURI;
	}

	private void startDiscoveryWebSocket() {
		if (this.webSocket != null) return;
		if (this.startedDiscoverySocket) return;

		this.startedDiscoverySocket = true;
		
        new Thread(() -> {
        	try {
        		URI discoveryURI = this.baseServerUri.resolve("RemoteP2P/discovery");
        		ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
			
                ClientManager client = ClientManager.createClient();
		client.getProperties().put(ClientProperties.LOG_HTTP_UPGRADE, true);	
        		
    			client.connectToServer(new Endpoint() {
    			    @Override
    			    public void onOpen(Session session, EndpointConfig config) {
    			    	RemoteP2PModule.this.webSocket = session;    			    	
    			    	RemoteP2PModule.this.executor.execute(() -> RemoteP2PModule.this.onOpen());

    			        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
    						@Override
    						public void onMessage(final ByteBuffer arg0) {
    							RemoteP2PModule.this.executor.execute(() -> RemoteP2PModule.this.onMessage(arg0));
    						}
    					});
    	    	    }
    			    
    			    @Override
    			    public void onClose(Session session, final CloseReason closeReason) {
    			    	System.out.println("CLOSE: "+closeReason);
    			    	RemoteP2PModule.this.executor.execute(() -> RemoteP2PModule.this.onClose(closeReason));
    			    }
    			    
    			    @Override
    			    public void onError(Session session, final Throwable thr) {
    			    	System.out.println("ERROR: "+thr);
    			    	RemoteP2PModule.this.executor.execute(() -> RemoteP2PModule.this.onError(thr));
    			    }
    			}, configuration, discoveryURI);
    		} catch (DeploymentException e) {
    			RemoteP2PModule.this.executor.execute(() -> { this.startedDiscoverySocket = false; });
    			System.err.println("Failed to establish web socket discovery connection: "+e);
    		} catch (IOException e) {
    			RemoteP2PModule.this.executor.execute(() -> { this.startedDiscoverySocket = false; });
    			System.err.println("Failed to establish web socket discovery connection: "+e);
    		}
        }).start();
	}
	private void stopDiscoveryWebSocket() {
		if (!this.isAdvertising && !this.isBrowsing) {
			try {
				this.startedDiscoverySocket = false;
				this.webSocket.close();
			} catch (IOException e) {
				System.err.println("Failed to close the web socket discovery connection: "+e);
			}
			
			this.webSocket = null;
		}
	}
	public void onOpen() {
		System.out.println("WebSocket connection opened ...");
		if (this.isAdvertising) this.sendRemoteP2PPacket(RemoteP2PPacket.START_ADVERTISEMENT);
		if (this.isBrowsing) this.sendRemoteP2PPacket(RemoteP2PPacket.START_BROWSING);
	
		if (this.advertiserHandler != null) this.advertiserHandler.onAdvertisingStarted(this);
		if (this.browserHandler != null) this.browserHandler.onBrowsingStarted(this);
	}
	public void onMessage(ByteBuffer data) {
		RemoteP2PPacket packet = RemoteP2PPacket.packetFromData(data.array());
		if (packet == null) {
			System.err.println("Received invalid RemoteP2P packet.");
			return;
		}
				
		switch (packet.type) {
			case RemoteP2PPacket.PEER_ADDED: this.addPeer(packet.uuid); break;
			case RemoteP2PPacket.PEER_REMOVED: this.removePeer(packet.uuid); break;
			case RemoteP2PPacket.CONNECTION_REQUEST: this.acceptConnection(packet.uuid); break;
			default: System.err.println("Received invalid packet of type: "+packet.type+" uuid: "+packet.uuid);
		}
	}
	public void onClose(CloseReason closeReason) {
		System.out.println("Closed with reason: "+closeReason);
		this.webSocket = null;
		this.startedDiscoverySocket = false;
		this.advertiserHandler.onAdvertisingStopped(this);
		this.browserHandler.onBrowsingStopped(this, null);
		if (closeReason.getCloseCode().equals(CloseReason.CloseCodes.GOING_AWAY)){ //idle timeout.
			System.out.println("Reconnecting  ...");
			startDiscoveryWebSocket();
		}
	}
	public void onError(Throwable exception) {
		System.err.println("Closed with exception: "+exception);
		exception.printStackTrace();
		this.startedDiscoverySocket = false;
		this.webSocket = null;
		
		this.advertiserHandler.onAdvertisingStopped(this);
		this.browserHandler.onBrowsingStopped(this, null);
	}
	private void sendRemoteP2PPacket(int type) {		
		if (this.webSocket != null && this.webSocket.isOpen()) {
			RemoteP2PPacket packet = new RemoteP2PPacket(type, this.localPeerIdentifier);
			this.webSocket.getAsyncRemote().sendBinary(packet.serialize());
		}
	}
	private void addPeer(UUID identifier) {
		if (this.isBrowsing) {
			URI connectionRequestUri = this.baseServerUri.resolve("RemoteP2P/connection/request/").resolve(this.localPeerIdentifier + "/").resolve(identifier.toString());
			RemoteP2PAddress address = new RemoteP2PAddress(this.executor, connectionRequestUri);
			this.addresses.put(identifier, address);
			this.browserHandler.onAddressDiscovered(this, address, identifier);
		}
	}
	private void removePeer(UUID identifier) {
		if (this.isBrowsing) {
			Address address = this.addresses.remove(identifier);
			this.browserHandler.onAddressRemoved(this, address, identifier);
		}
	}
	private void acceptConnection(UUID identifier) {
		URI acceptConnectionRequestUri = this.baseServerUri.resolve("RemoteP2P/connection/accept/").resolve(this.localPeerIdentifier + "/").resolve(identifier.toString());
		final RemoteP2PConnection connection = new RemoteP2PConnection(this.executor, acceptConnectionRequestUri, false);
		
		connection.setHandler(new Connection.Handler() {
			@Override
			public void onDataSent(Connection connection) {}
			@Override
			public void onDataReceived(Connection connection, ByteBuffer data) {}
			@Override
			public void onConnect(Connection connection) {
				RemoteP2PModule.this.advertiserHandler.onConnection(RemoteP2PModule.this, connection);
			}
			@Override
			public void onClose(Connection connection) {
				System.out.println("Connection was not able to open; could there for not be accepted");
			}
		});
		
		connection.connect();
	}
	
	@Override
	public Advertiser getAdvertiser() {
		return this;
	}
	@Override
	public Browser getBrowser() {
		return this;
	}
	
	@Override
	public void setAdvertiserHandler(Advertiser.Handler handler) {
		this.advertiserHandler = handler;
	}
	@Override
	public Advertiser.Handler getAdvertiserHandler() {
		return this.advertiserHandler;
	}
	@Override
	public boolean isAdvertising() {
		return isAdvertising;
	}
	@Override
	public void startAdvertisingWithPeerIdentifier(UUID identifier) {
		this.localPeerIdentifier = identifier;
		this.startDiscoveryWebSocket();
		this.isAdvertising = true;
		this.sendRemoteP2PPacket(RemoteP2PPacket.START_ADVERTISEMENT);
	}
	@Override
	public void stopAdvertising() {
		this.stopDiscoveryWebSocket();
		this.isAdvertising = false;
		this.sendRemoteP2PPacket(RemoteP2PPacket.STOP_ADVERTISEMENT);
	}
	
	@Override
	public void setBrowserHandler(Browser.Handler handler) {
		this.browserHandler = handler;
	}
	@Override
	public Browser.Handler getBrowserHandler() {
		return this.browserHandler;
	}
	@Override
	public boolean isBrowsing() {
		return isBrowsing;
	}
	@Override
	public void startBrowsing() {
		this.startDiscoveryWebSocket();
		this.isBrowsing = true;
		this.sendRemoteP2PPacket(RemoteP2PPacket.START_BROWSING);
	}
	@Override
	public void stopBrowsing() {
		this.stopDiscoveryWebSocket();
		this.isBrowsing = false;
		this.sendRemoteP2PPacket(RemoteP2PPacket.STOP_BROWSING);
	}
	@Override
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
}
