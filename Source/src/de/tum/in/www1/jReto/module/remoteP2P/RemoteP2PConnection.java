package de.tum.in.www1.jReto.module.remoteP2P;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

import de.tum.in.www1.jReto.module.api.Connection;


public class RemoteP2PConnection implements Connection {
	private final Executor executor;
	private Connection.Handler handler;
	private URI serverUri;
	private boolean awaitConfirmation;
	private Session dataSession;

	public RemoteP2PConnection(Executor executor, URI serverUri, boolean awaitConfirmation) {
		this.executor = executor;
		this.serverUri = serverUri;
		this.awaitConfirmation = awaitConfirmation;
	}
	
	@Override
	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	@Override
	public Handler getHandler() {
		return this.handler;
	}
	@Override
	public boolean isConnected() {
		return this.dataSession != null && this.dataSession.isOpen() && !this.awaitConfirmation;
	}
	@Override
	public int getRecommendedPacketSize() {
		return 2048;
	}
	@Override
	public void writeData(ByteBuffer data) {
		if (!this.isConnected()) {
			System.err.println("attempted to write before connection is open.");
			return;
		}
		new Thread(() -> 
		this.dataSession.getAsyncRemote().sendBinary(data, new SendHandler() {
			@Override
			public void onResult(SendResult arg0) {
				RemoteP2PConnection.this.executor.execute(new Runnable() {
					@Override
					public void run() {
						if (RemoteP2PConnection.this.handler != null) RemoteP2PConnection.this.handler.onDataSent(RemoteP2PConnection.this);
					}
				});
			}
		})).start();
	}
	
	private void onOpen() {		
		if (this.handler != null && this.isConnected()) this.handler.onConnect(RemoteP2PConnection.this);
	}
	private void onMessage(ByteBuffer data) {
		data.order(ByteOrder.LITTLE_ENDIAN);
		
		if (this.awaitConfirmation) {
			int remaining = data.remaining();
			int result = 0;
			if (remaining == 4) result = data.getInt(); 
			if (remaining != 4 || result != 1) {
				System.err.println("Expected web socket connection open confirmation, received invalid data: length: "+remaining+" result: "+result);
				RemoteP2PConnection.this.close();
			} else {
				RemoteP2PConnection.this.awaitConfirmation = false;
				if (RemoteP2PConnection.this.handler != null) this.handler.onConnect(this);
			}
		} else {
			if (RemoteP2PConnection.this.handler != null) RemoteP2PConnection.this.handler.onDataReceived(RemoteP2PConnection.this, data);
		}
	}
	@Override
	public void connect() {		
		if (this.dataSession != null) return;

		// client.connect is blocking, therefore we need to start a new thread.
		new Thread(new Runnable() {
			@Override
			public void run() {
				ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
		        ClientManager client = ClientManager.createClient();
		        
		        try {
					client.connectToServer(new Endpoint() {
					    @Override
					    public void onOpen(final Session session, EndpointConfig config) {							
							RemoteP2PConnection.this.dataSession = session;
					 
							RemoteP2PConnection.this.executor.execute(new Runnable() {
								@Override
								public void run() {
									RemoteP2PConnection.this.onOpen();
								}
							});
							
					        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
								@Override
								public void onMessage(final ByteBuffer data) {
							    	RemoteP2PConnection.this.executor.execute(new Runnable() {
										@Override
										public void run() {
											RemoteP2PConnection.this.onMessage(data);
										}
							    	});
								}
							});
					    }
					    
					    @Override
					    public void onClose(Session session, final CloseReason closeReason) {
					    	RemoteP2PConnection.this.executor.execute(new Runnable() {
								@Override
								public void run() {
									System.err.println("WebSocket data connection closed with reason: "+closeReason.getCloseCode()+" / "+closeReason.getReasonPhrase());
									RemoteP2PConnection.this.dataSession = null;
									if (RemoteP2PConnection.this.handler != null) RemoteP2PConnection.this.handler.onClose(RemoteP2PConnection.this);
								}
							});
						}
					    
					    @Override
					    public void onError(Session session, final Throwable thr) {
					    	RemoteP2PConnection.this.executor.execute(new Runnable() {
								@Override
								public void run() {
									System.err.println("WebSocket data connection closed with error: "+thr);
									RemoteP2PConnection.this.dataSession = null;
									if (RemoteP2PConnection.this.handler != null) RemoteP2PConnection.this.handler.onClose(RemoteP2PConnection.this);
								}
							});
					    }
					}, configuration, RemoteP2PConnection.this.serverUri);
					
				} catch (DeploymentException | IOException e) {
					System.err.println("Failed to establish web socket data connection: "+e);
				}
			}
		}).start();
	}

	@Override
	public void close() {
		new Thread(() -> {
			try {
				this.dataSession.close();
				this.dataSession = null;
			} catch (IOException e) {
				System.err.println("Failed to close data websocket connection: "+e);
			}
		}).start();
	}
}
