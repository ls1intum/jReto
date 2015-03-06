package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

import de.tum.in.www1.jReto.module.api.Connection;

/** 
* A MulticastConnection acts like a normal underlying connection, but sends all data written to it using a set of subconnections.
* Data received from any subconnection is reported to the delegate.
*/
public class MulticastConnection implements Connection, Connection.Handler {
	private Connection.Handler handler;
    /** The subconnections used with this connection */
	private final Set<Connection> subconnections = new HashSet<Connection>();
    /** Stores the number of dataSent calls yet to be received. Once all are received, the delegate's didSendData can be called. */
	private int dataSentCallbacksToBeReceived = 0;
    /** The number of data packets that have been sent in total. */
	private int dataPacketsSent = 0;

    /** Adds a subconnection. */
	public void addSubconnection(Connection connection) {
		this.subconnections.add(connection);
		connection.setHandler(this);
	}
	
	public Set<Connection> getSubconnections() {
		return this.subconnections;
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
		boolean isConnected = true;
		
		for (Connection subconnection : this.subconnections) isConnected &= subconnection.isConnected();
		
		return isConnected;
	}
	@Override
	public int getRecommendedPacketSize() {
		int recommendedPacketSize = 32 * 1024;
		
		for (Connection subconnection : this.subconnections) recommendedPacketSize = Math.min(recommendedPacketSize, subconnection.getRecommendedPacketSize());
		
		return recommendedPacketSize;
	}

	@Override
	public void connect() {
		for (Connection subconnection : this.subconnections) subconnection.connect();
	}
	@Override
	public void close() {
		for (Connection subconnection : this.subconnections) subconnection.close();
	}

	@Override
	public void writeData(ByteBuffer data) {
		if (dataSentCallbacksToBeReceived != 0) {
			this.dataPacketsSent++;
		} else {
			this.dataSentCallbacksToBeReceived = this.subconnections.size();
		}
		
		for (Connection subconnection : this.subconnections) {
			subconnection.writeData(data.slice().order(ByteOrder.LITTLE_ENDIAN));
		}
	}

	@Override
	public void onConnect(Connection connection) {
		if (this.isConnected()) handler.onConnect(this);
	}
	@Override
	public void onClose(Connection connection) {
		for (Connection subconnection : this.subconnections) {
			if (subconnection == connection) continue;
			
			subconnection.close();
		}
		
		if (this.handler != null) this.handler.onClose(this);
	}

	@Override
	public void onDataReceived(Connection connection, ByteBuffer data) {
		this.handler.onDataReceived(this, data);
	}

	@Override
	public void onDataSent(Connection connection) {
		if (this.dataSentCallbacksToBeReceived == 0) {
			System.err.println("Received unexpected onDataSent call!");
			return;
		}
		
		this.dataSentCallbacksToBeReceived--;
		
		if (this.dataSentCallbacksToBeReceived == 0) {
			if (this.dataPacketsSent != 0) {
				this.dataPacketsSent--;
				this.dataSentCallbacksToBeReceived = this.subconnections.size();
			}
			
			this.handler.onDataSent(this);
		}
	}
}
