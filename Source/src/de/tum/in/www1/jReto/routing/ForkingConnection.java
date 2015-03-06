package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.tum.in.www1.jReto.module.api.Connection;

//TODO: ignores didSendData - is this ok?
//TODO: might buffer lots of data if incoming connection is fast and outgoing connection is slow.

/**
* A ForkingConnection acts like the incomingConnection it was constructed with, but additionally forwards any data received 
* from the incoming connection to an additional outgoing connection and vice versa. Delegate methods will not be called for any events related to the outgoing connection.
*/
public class ForkingConnection implements Connection, Connection.Handler {
	public static interface CloseHandler {
		void onClose(ForkingConnection connection);
	}
	
    /** The ForkingConnection's incoming connection. */
	private final Connection incomingConnection;
    /** The ForkingConnection's outgoing connection */
	private final Connection outgoingConnection;
    /** A closure to call when the connection closes. */
	private final CloseHandler closeHandler;
	private Connection.Handler handler;
	
	public ForkingConnection(Connection incomingConnection, Connection outgoingConnection, CloseHandler closeHandler) {
		this.incomingConnection = incomingConnection;
		this.outgoingConnection = outgoingConnection;
		this.closeHandler = closeHandler;
		this.incomingConnection.setHandler(this);
		this.outgoingConnection.setHandler(this);
	}
	
	private Connection counterpart(Connection connection) {
		if (connection == this.incomingConnection) return this.outgoingConnection;
		if (connection == this.outgoingConnection) return this.incomingConnection;
		
		System.err.println("Using invalid connection: "+connection);
		return null;
	}

	@Override
	public void onConnect(Connection connection) {
		System.out.println("Received onConnect in ForwardingConnection; connections should already be connected though.");
	}
	@Override
	public void onClose(Connection connection) {
		this.incomingConnection.setHandler(null);
		this.outgoingConnection.setHandler(null);
		
		this.counterpart(connection).close();
		
		if (this.handler != null) this.handler.onClose(this);
		this.closeHandler.onClose(this);
	}
	@Override
	public void onDataReceived(Connection connection, ByteBuffer data) {
		if (connection == incomingConnection && this.handler != null) this.handler.onDataReceived(this, data.slice().order(ByteOrder.LITTLE_ENDIAN));
		this.counterpart(connection).writeData(data.slice().order(ByteOrder.LITTLE_ENDIAN));
	}
	@Override
	public void onDataSent(Connection connection) {
		if (connection == this.incomingConnection) {
			if (this.handler != null) this.handler.onDataSent(this);
		}
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
		return this.incomingConnection.isConnected() && this.outgoingConnection.isConnected();
	}
	@Override
	public int getRecommendedPacketSize() {
		return this.incomingConnection.getRecommendedPacketSize();
	}
	@Override
	public void connect() {
		System.err.println("Called connect on ForkingConnection. It should already be connected.");
	}
	@Override
	public void close() {
		this.incomingConnection.close();
		this.outgoingConnection.close();
	}
	@Override
	public void writeData(ByteBuffer data) {
		this.incomingConnection.writeData(data);
	}
}
