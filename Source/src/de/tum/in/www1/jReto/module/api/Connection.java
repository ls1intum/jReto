package de.tum.in.www1.jReto.module.api;

import java.nio.ByteBuffer;

/**
* A moudle.api.Connection has the minimal necessary functionality that allows the implementation of Reto connections on top of it.
* Note that this interface is different from Reto's high-level Connection class, which offers many additional features.
* Reto's users don't interact with this class directly.
*/
public interface Connection {
	/** 
	* The Connection.Handler interface allows the Connection to inform its delegate about various events.
	*/
	public static interface Handler {
	    /** Called when the connection connected successfully.*/
		void onConnect(Connection connection);
	    /** Called when the connection closes. Has an optional error parameter to indicate issues. (Used to report problems to the user). */
		void onClose(Connection connection);
	    /** Called when data was received. */
		void onDataReceived(Connection connection, ByteBuffer data);
	    /** Called for each writeData call, when it is complete. */
		void onDataSent(Connection connection);
	}
	
    /** Sets the connection's delegate. */
	void setHandler(Handler handler);
    /** The connection's delegate. */
	Handler getHandler();
	
    /** Whether this connection is currently connected. */
	boolean isConnected();
    /** Reto sends packets which may vary in size. This property may return an ideal packet size that should be used if possible. */
	int getRecommendedPacketSize();
	
    /** Connects the connection. */
	void connect();
    /** Closes the connection. */
	void close();
	
    /** Sends data using the connection. */
	void writeData(ByteBuffer data);
}
