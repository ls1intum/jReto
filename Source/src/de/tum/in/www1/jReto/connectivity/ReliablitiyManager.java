package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.connectivity.packet.CloseAcknowledge;
import de.tum.in.www1.jReto.connectivity.packet.CloseAnnounce;
import de.tum.in.www1.jReto.connectivity.packet.CloseRequest;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.util.RepeatedExecutor;

/**
* The ReliablityManager is responsible for cleanly closing connections and attempting to reconnect failed connections.
*
* TODO: This class currently assumes that if connecting takes longer than a specific amount of time (1 second), the connection attempt failed and tries to reconnect. 
* This causes issues if establishing the connection takes longer than 1 second, especially with routed/multicast connections, causing the connection establishment process to fail. 
* To fix this, the next attempt should only be started after a generous timeout or if the connection establishment process failed, and not if it is still in progress.
*/
public class ReliablitiyManager implements PacketConnection.Handler {
	/**
	* The ReliablityManager's delegate protocol.
	*/
	public static interface ReliabilityManagerHandler {
	    /** Called when the connection connected successfully. */
		void onConnectionConnected();
	    /** Called when the connection closes expectedly (i.e. when close() was called on the connection) */
		void onExpectedConnectionClose();
	    /** Called when the connection failed and could not be reestablished. */
		void onUnexpectedFinalConnectionClose();
	}
	
	/**
	* A ConnectionManager manages PacketConnections.
	*/
	public static interface PacketConnectionManager {
	    /** 
	     * Called when a connection closed.
		 */
		void notifyConnectionClose(PacketConnection connection);
	    /**
		 * Called when a new underlying connection needs to be established for an packet connection.
		 */
		void establishUnderlyingConnection(PacketConnection connection);
	}
	
    /** The delegate */
	private ReliabilityManagerHandler handler;
    /** The PacketConnection thats reliability is managed. */
	private final PacketConnection packetConnection;
    /** The connection manager */
	private final PacketConnectionManager packetConnectionManager;

    /** The local peer's identifier */
	private final UUID localIdentifier;
	/** Set to true when the underlying connection is expected to close. */
	private boolean isExpectingConnectionToClose;
    /** Set to true if this ReliabilityManager is expected to attempt to reconnect when a connection fails. */
	private final boolean isExpectedToReconnect;
    /** The executor used to repeat reconnect attempts */
	private final RepeatedExecutor reconnectRepeater;
    /** The number of reconnect attempts that have been performed */
	private int reconnectAttempts = 0;
    /** All of the managed connection's destination's identifiers */
	private final Set<UUID> connectionDestinations;
    /** The number of close acknowledge packets received. Necessary since acknowledgements need to be received from all destinations before a connection can be closed safely. */
	private final Set<UUID> receivedCloseAcknowledgements = new HashSet<>();
	
    /** 
    * Constructs a new ReliablityManager.
    * @param packetConnection The packet connection managed
    * @param packetConnectionManager The connection manager responsible for the packet connection.
    * @param localIdentifier The local peer's identifier.
    * @param connectionDestinations The destinations of the connection
    * @param isExpectedToReconnect If set to true, this ReliabilityManager will attempt to reconnect the packet connection if its underlying connection fails.
    * @param executor The Executor on which all delegate method calls are dispatched.
    */
	public ReliablitiyManager(PacketConnection packetConnection, PacketConnectionManager packetConnectionManager, UUID localIdentifier, Set<UUID> connectionDestinations, boolean isExpectedToReconnect, Executor executor) {
		this.packetConnection = packetConnection;
		this.packetConnectionManager = packetConnectionManager;
		this.localIdentifier = localIdentifier;
		this.connectionDestinations = connectionDestinations;
		this.isExpectingConnectionToClose = false;
		this.isExpectedToReconnect = isExpectedToReconnect;
		this.reconnectRepeater = new RepeatedExecutor(new Runnable() {
			@Override
			public void run() {
				ReliablitiyManager.this.attemptConnect();
			}
		}, 2, 1, executor);
		
		this.packetConnection.addDelegate(this);
	}
	
    /** Closes the packet connection cleanly. */
	public void closeConnection() {
		if (this.isExpectedToReconnect) {
			this.packetConnection.writePacket(new CloseAnnounce());
		} else {
			this.packetConnection.writePacket(new CloseRequest());
		}
	}
	private void handleCloseRequest() {
		this.isExpectingConnectionToClose = true;
		this.packetConnection.writePacket(new CloseAnnounce());
	}
	private void handleCloseAnnounce() {
		this.isExpectingConnectionToClose = true;
		this.packetConnection.writePacket(new CloseAcknowledge(this.localIdentifier));
	}
	private void handleCloseAcknowledge(CloseAcknowledge acknowledgePacket) {
		this.isExpectingConnectionToClose = true;
		this.receivedCloseAcknowledgements.add(acknowledgePacket.source);
				
		if (this.receivedCloseAcknowledgements.equals(this.connectionDestinations)) {
			this.receivedCloseAcknowledgements.clear();
			this.packetConnection.disconnectUnderlyingConnection();
		}
	}
	
    /** Attempts to connect a PacketConnection with no or a failed underlying connection. */
	public void attemptConnect() {
		if (this.packetConnection.getIsConnected()) return;
		this.reconnectAttempts++;

		if (this.reconnectAttempts > 8) {
			this.reconnectRepeater.stop();
			this.handler.onUnexpectedFinalConnectionClose();
		} else {
			if (this.reconnectAttempts > 1) System.out.println("Attempting to connect (try "+this.reconnectAttempts+")...");
			this.reconnectRepeater.start();
			this.packetConnectionManager.establishUnderlyingConnection(this.packetConnection);
		}
	}
	
	public void setHandler(ReliabilityManagerHandler handler) {
		this.handler = handler;
	}
	public ReliabilityManagerHandler getHandler() {
		return this.handler;
	}

	@Override
	public Set<PacketType> getHandledPacketTypes() {
		return new HashSet<>(Arrays.asList(PacketType.CLOSE_REQUEST, PacketType.CLOSE_ANNOUNCE, PacketType.CLOSE_ACKNOWLEDGE));
	}
	@Override
	public void handlePacket(ByteBuffer packet, PacketType type) {
		switch (type) {
			case CLOSE_REQUEST: handleCloseRequest(); break;
			case CLOSE_ANNOUNCE: handleCloseAnnounce(); break;
			case CLOSE_ACKNOWLEDGE: handleCloseAcknowledge(CloseAcknowledge.deserialize(packet)); break;
			default: throw new IllegalArgumentException("Invalid type: "+type);
		}
	}
	@Override
	public void onUnderlyingConnectionClose(PacketConnection connection) {
		this.reconnectAttempts = 0;
		
		if (this.isExpectingConnectionToClose) {
			this.reconnectRepeater.stop();
			this.packetConnectionManager.notifyConnectionClose(packetConnection);
			this.handler.onExpectedConnectionClose();
		} else if (this.isExpectedToReconnect) {
			this.reconnectRepeater.start();
		}
	}

	@Override
	public void onWillSwapUnderlyingConnection(PacketConnection connection) {}
	@Override
	public void onUnderlyingConnectionConnected(PacketConnection connection) {
		this.reconnectRepeater.stop();
		this.reconnectAttempts = 0;
		
		this.handler.onConnectionConnected();
	}
	@Override
	public void onNoPacketsLeft(PacketConnection connection) {}
}
