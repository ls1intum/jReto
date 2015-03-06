package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.Node;

/**
* PacketConnections are used to send and receive packets. Packets used by Reto can be any data of any fixed length, 
* but the first four bytes always contain the packet's type.
*
* A PacketConnection encapsulates the idea of a logical connection. It adds an additional indirection between a Connection as provided by 
* the Reto API and underlying connections. This allows the PacketConnection to switch between different underlying connections, and even exist without
* an underlying connection. This is the basis of features such as automatic reconnect, and offering automatic connection upgrades.
*
* Packets can be sent by calling the write() method. If data is currently written, or no underlying connection is available, the packet is buffered
* until it can be sent. 
*
* A PacketConnection can have multiple delegates. Amongst other events, the PacketConnection delegates the handling of packets to multiple delegates that implement
* the PacketHandler protocol. The PacketHandlers may specify the packet types they are able to handler; the packet connection will call the according handler's
* handlePacket method.
*
* The PacketConnection itself is agnostic of the different packet types; the introduction of new packet types and handlers does not require any changes
* in the PacketConnection class. This allows Reto to split up different functionality of Connections into different classes, 
* e.g. the TransferManager and ReliabilityManager, which are both PacketHandlers.
*/
public class PacketConnection {
	/**
	* The delegate protocol used with the PacketConnection.
	* It allows delegates to specify which types of packets they wish to handle.
	*/
	public static interface Handler {
	    /** Called when the underlying connection closed. */
		void onUnderlyingConnectionClose(PacketConnection connection);
	    /** Called when the underlying connection is about to be switched to a different one. */
		void onWillSwapUnderlyingConnection(PacketConnection connection);
	    /** Called when confirmation that the underlying connection did connect is received. */
		void onUnderlyingConnectionConnected(PacketConnection connection);
	    /** Called whenever all packets that were queued have been sent, i.e. the connection is ready for more data if available. */
		void onNoPacketsLeft(PacketConnection connection);
	    /** An array of packet types that are handled by this PacketHandler. */
		Set<PacketType> getHandledPacketTypes();
	    /** Called when a packet is received that should be handled */
		void handlePacket(ByteBuffer data, PacketType type);
	}
	
    /** The underlying connection used by this PacketConnection */
	private de.tum.in.www1.jReto.module.api.Connection underlyingConnection;
    /** The PacketConnection's delegates. */
	private Set<Handler> delegates = new HashSet<>();
    /**
    * The PacketConnection's delegates, organized by the packet type they handle.
    */
	private Map<PacketType, Handler> packetHandlers = new HashMap<>();
    /** The connection's identifier. This identifier is used to associate new incoming underlying connections with exising packet connections. */
	private final UUID connectionIdentifier;
    /** This connection's destinations. */
	private final Set<Node> destinations;
    /** Buffer for unsent packets. */
	private Queue<Packet> unsentPackets;
    /** Whether a packet is currently being sent. */
	private boolean isSendingPacket = false;
    /** Whether a connection is currently being established. */
	private boolean isEstablishingConnection = false;

	/** Handles events from the underlying connection and calls appropriate methods. */
	private final de.tum.in.www1.jReto.module.api.Connection.Handler underlyingConnectionHandler = new de.tum.in.www1.jReto.module.api.Connection.Handler() {
		@Override
		public void onDataReceived(de.tum.in.www1.jReto.module.api.Connection connection,
				ByteBuffer data) {
			if (connection != PacketConnection.this.underlyingConnection) {
				System.out.println("Received data from unused underlying connection.");
				return;
			}

			PacketType packetType = PacketType.fromData(data);
			Handler handler = PacketConnection.this.packetHandlers.get(packetType);

			if (handler == null) {
				System.err.println("Warning: There is no handler for packets of type "+ packetType+ ". Registered handlers: "+ PacketConnection.this.packetHandlers);
				return;
			}

			handler.handlePacket(data, packetType);
		}

		@Override
		public void onDataSent(de.tum.in.www1.jReto.module.api.Connection connection) {
			if (connection != PacketConnection.this.underlyingConnection) {
				System.out.println("Received onDataSent from unused underlying connection.");
				return;
			}
			PacketConnection.this.isSendingPacket = false;
			PacketConnection.this.write();
		}

		@Override
		public void onConnect(de.tum.in.www1.jReto.module.api.Connection connection) {
			if (connection != PacketConnection.this.underlyingConnection) {
				System.out.println("Received onConnect from unused underlying connection.");
				return;
			}

			PacketConnection.this.onConnect();
		}

		@Override
		public void onClose(de.tum.in.www1.jReto.module.api.Connection connection) {
			if (connection != PacketConnection.this.underlyingConnection) {
				System.out.println("Previous underlying connection closed.");
				connection.setHandler(null);
				return;
			}

			PacketConnection.this.delegates.forEach(delegate -> delegate.onUnderlyingConnectionClose(PacketConnection.this));
		}
	};

    /** 
    * Initializes a new PacketConnection.
    * 
    * @param connection An underlying connection to use with this packet connection. May be nil and set later.
    * @param connectionIdentifier This connection's identifier.
    * @param destinations The connection's destinations.
    */
	public PacketConnection(de.tum.in.www1.jReto.module.api.Connection underlyingConnection, UUID connectionIdentifier, Set<Node> destinations) {
		this.unsentPackets = new LinkedBlockingQueue<Packet>();
		this.underlyingConnection = underlyingConnection;
		this.connectionIdentifier = connectionIdentifier;
		this.destinations = destinations;

		if (this.underlyingConnection != null) {
			this.underlyingConnection.setHandler(this.underlyingConnectionHandler);
			if (this.underlyingConnection.isConnected()) this.onConnect();
		}
	}

	private void onConnect() {
		this.delegates.forEach(delegate -> delegate.onUnderlyingConnectionConnected(this));

		this.write();
	}
	
    /** Add a delegate */
	public void addDelegate(Handler delegate) {
		this.delegates.add(delegate);

		for (PacketType type : delegate.getHandledPacketTypes()) {
			this.packetHandlers.put(type, delegate);
		}
	}
	/** Remove a delegate*/
	public void removeDelegate(Handler delegate) {
		this.delegates.remove(delegate);
		// TODO remove packet handlers
	}
	
	public Connection getUnderlyingConnection() {
		return this.underlyingConnection;
	}

	public Set<Node> getDestinations() {
		return this.destinations;
	}

	public boolean getIsEstablishingConnection() {
		return this.isEstablishingConnection;
	}

	public void setIsEstablishingConnection(boolean isEstablishingConnection) {
		this.isEstablishingConnection = isEstablishingConnection;
	}

    /**
    * Swaps this PacketConnection's underlying connection to a new one. The new connection can also be nil; in that case the packet connection will be disconnected.
    */
	public void swapUnderlyingConnection(de.tum.in.www1.jReto.module.api.Connection underlyingConnection) {
		if (this.underlyingConnection == underlyingConnection) return;

		if (this.underlyingConnection != null) {
			this.delegates.forEach(delegate -> delegate.onWillSwapUnderlyingConnection(this));
		}

		Connection previousConnection = this.underlyingConnection;
		this.underlyingConnection = underlyingConnection;
		if (this.underlyingConnection != null) this.underlyingConnection.setHandler(this.underlyingConnectionHandler);

		if (previousConnection != null && previousConnection.isConnected()) previousConnection.close();

		this.isSendingPacket = false;
		this.unsentPackets.clear();

		if (this.underlyingConnection != null && this.underlyingConnection.isConnected()) this.onConnect();
	}

	public UUID getConnectionIdentifier() {
		return this.connectionIdentifier;
	}

	public boolean getIsConnected() {
		return this.underlyingConnection != null && this.underlyingConnection.isConnected();
	}

    /**
    * Closes the underlying connection
    */
	public void disconnectUnderlyingConnection() {
		this.underlyingConnection.close();
	}
	/**
	 * Writes a packet. The packet will be buffered and sent later if the connection is currently disconnected.
	 */
	public void writePacket(Packet packet) {
		this.unsentPackets.add(packet);
		this.write();
	}
    /**
    * Attempts to write any buffered packets, or notifies it's delegates that all packes have been written.
    */
	public void write() {
		if (this.isSendingPacket)
			return;
		if (this.underlyingConnection == null
				|| !this.underlyingConnection.isConnected())
			return;

		Packet nextPacket = null;
		if (this.unsentPackets.size() != 0) {
			nextPacket = this.unsentPackets.poll();
		}

		if (nextPacket != null) {
			this.isSendingPacket = true;
			this.underlyingConnection.writeData(nextPacket.serialize());
		} else {
			this.delegates.forEach(delegate -> delegate.onNoPacketsLeft(this));
		}
	}
}
