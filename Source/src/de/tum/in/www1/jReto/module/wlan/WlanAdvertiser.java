package de.tum.in.www1.jReto.module.wlan;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.niotools.Dispatcher;

public class WlanAdvertiser implements Advertiser {
	private Handler handler;
	private Dispatcher dispatcher;
	private ServerSocketChannel serverSocketChannel;
	private boolean advertising;
	
	String networkType;
	
	private JmDNS bonjourServer;
	private ServiceInfo serviceInfo;

	
	public WlanAdvertiser(Dispatcher dispatcher, String networkType) {
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (networkType == null) throw new IllegalArgumentException("networkType may not be null");

		this.advertising = false;
		this.networkType = networkType;
		
		this.dispatcher = dispatcher;
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		       	try {
					bonjourServer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		});
	}
	
	protected void onAccept(ServerSocketChannel serverChannelSocket) {
		try {
			SocketChannel channel = serverChannelSocket.accept();
			if (channel != null) {
				this.handler.onConnection(this, new WlanConnection(this.dispatcher, channel));
			}
		} catch (IOException e) {
			System.err.println("Failed to accept socket.");
		}
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
		return this.advertising;
	}

	@Override
	public void startAdvertisingWithPeerIdentifier(UUID identifier) {
		if (identifier == null) throw new IllegalArgumentException();

		if (this.advertising) return;
		
		// Configure & open the server socket
		try {
			this.serverSocketChannel = ServerSocketChannel.open();
			this.serverSocketChannel.socket().bind(null);
			this.serverSocketChannel.configureBlocking(false);

			this.dispatcher.registerAcceptHandler(
					new Dispatcher.AcceptHandler<ServerSocketChannel>() {
						public void onAcceptable(ServerSocketChannel serverChannelSocket) {
							WlanAdvertiser.this.onAccept(serverChannelSocket);
						}
					}, this.serverSocketChannel);

			// Starting a new thread to start advertising using jmdns, since jmdns may block the thread for several seconds.
			new Thread(() -> {
				try {
					String serviceType = "_" + this.networkType + "wlan._tcp.local.";
					this.serviceInfo = ServiceInfo.create(serviceType, identifier.toString(), serverSocketChannel.socket().getLocalPort(), "");

					this.bonjourServer = JmDNS.create("RetoWlanAdvertiser");
					WlanAdvertiser.this.bonjourServer.registerService(serviceInfo);
				} catch (Exception e) {
					System.err.println("Error occurred: "+e);
				}
			}).start();
			
			this.advertising = true;
			if (this.handler != null && this.advertising) handler.onAdvertisingStarted(this);
		} catch(IOException e) {
			System.err.println("Error occurred: "+e);
		}
	}
	
	@Override
	public void stopAdvertising() {
		try {
			this.dispatcher.unregister(this.serverSocketChannel);
			this.serverSocketChannel.close();
			this.bonjourServer.unregisterService(this.serviceInfo);
			this.bonjourServer.close();
		} catch (IOException e) {
			System.err.println("Failed to stop bonjour advertising.");
		} finally {
			this.serverSocketChannel = null;
			this.advertising = false;
			
			if (this.handler != null) handler.onAdvertisingStopped(this);
		}
	}
}
