package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import de.tum.in.www1.jReto.connectivity.PacketConnection;
import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.Router.OnConnectionHandler;
import de.tum.in.www1.jReto.routing.Router.OnFailHandler;
import de.tum.in.www1.jReto.routing.packets.ConnectionPurpose;

/**
* The Node class represents the routing component of a remote peer. It stores all routing related information about that peer.
* Nodes are created and managed by a Router.
* 
* Nodes also forward FloodPackets to the FloodingPacketManager which handles those packets. These packets are used to transmit routing information.
*/
public class Node implements PacketConnection.Handler {
    /** The Router that created this Node object*/
	private final Router router;
    /** The Node's identifer*/
	private final UUID identifier;
    /** The local peer's identifier. */
	private final UUID localIdentifier;
    /** Addresses that allow to connect to this node directly. */
	private final Set<Address> directAddresses = new HashSet<Address>();
    /** Stores the PacketConnection used to transmit routing metadata. */
	private PacketConnection routingConnection;
    /** The next hop to use when establishing a connection to this node (if the optimal route should be used). */
	private Node nextHop;
	private int cost;

    /** Initializes a Node object */
	public Node(Router router, UUID identifier, UUID localIdentifier) {
		this.router = router;
		this.identifier = identifier;
		this.localIdentifier = localIdentifier;
	}

    /** Whether this node is a neighbor of the local peer. */
	public boolean isNeighbor() {
		return this.nextHop == this;
	}
	
    /** Whether a route to this node exists or not. */
	public boolean isReachable() {
		return this.nextHop != null;
	}
	public Node getNextHop() {
		return this.nextHop;
	}
	public void setNextHop(Node node) {
		this.nextHop = node;
	}
	public int getCost() {
		return this.cost;
	}
	public void setCost(int cost) {
		this.cost = cost;
	}
	
	public UUID getIdentifier() {
		return this.identifier;
	}
	
	public PacketConnection getRoutingConnection() {
		return this.routingConnection;
	}
	public Set<Address> getAddresses() {
		return this.directAddresses;
	}

	public boolean isResponsibleForEstablishingRoutingConnection() {
		if (this.identifier.getMostSignificantBits() == this.localIdentifier.getMostSignificantBits()) {
			return this.identifier.getLeastSignificantBits() == this.localIdentifier.getLeastSignificantBits();
		} else {
			return this.identifier.getMostSignificantBits() < this.localIdentifier.getMostSignificantBits();
		}
	}

	public Address getBestAddress() {
		if (this.directAddresses.size() == 0) return null;

		return Collections.min(new ArrayList<Address>(this.directAddresses),
				new Comparator<Address>() {
					public int compare(Address o1, Address o2) {
						return o1.getCost() - o2.getCost();
					}
				});
	}

	public void connect(OnConnectionHandler onConnection, OnFailHandler onFail) {
		this.router.establishMulticastConnection(new HashSet<Node>(Arrays.asList(this)), onConnection, onFail);
		//this.router.establishRoutedConnection(this, onConnection, onFail);
	}

	public void establishRoutingConnection() {
		if (!this.isResponsibleForEstablishingRoutingConnection()) return;
		if (this.routingConnection != null && this.routingConnection.getIsConnected()) return;
				
		this.getBestAddress();
		
		this.router.establishDirectConnection(this, ConnectionPurpose.ROUTING_DATA_EXCHANGE_CONNECTION, new OnConnectionHandler() {
			@Override
			public void onConnect(Connection connection) {
				PacketConnection packetConnection = new PacketConnection(connection, null, new HashSet<>(Arrays.asList(Node.this)));
				Node.this.setupRoutingConnection(packetConnection);
			}
		}, new OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Failed to establish routing connection.");
			}
		});
	}
	public void handleRoutingConnection(Connection connection) {
		PacketConnection packetConnection = new PacketConnection(connection, null, new HashSet<>(Arrays.asList(this)));
		this.setupRoutingConnection(packetConnection);
	}
	private void setupRoutingConnection(PacketConnection connection) {
		this.routingConnection = connection;
		connection.addDelegate(this);
		
		this.router.onNeighborReachable(this);
		
		if (connection.getIsConnected()) { this.onUnderlyingConnectionConnected(connection); }
	}
	public void closeRoutingConnection() {
		if (this.routingConnection == null) {
			return;
		}
		
		this.routingConnection.getUnderlyingConnection().close();
	}
	public void setIncomingRoutingConnection(PacketConnection connection) {
		this.setupRoutingConnection(connection);
	}
	public void sendPacket(Packet packet) {
		this.routingConnection.writePacket(packet);
	}
	public UUID getLocalPeerIdentifier() {
		return this.localIdentifier;
	}

	public void addAddress(Address address) {
		this.directAddresses.add(address);
	}

	public void removeAddress(Address address) {
		this.directAddresses.remove(address);
	}

	@Override
	public void onUnderlyingConnectionClose(PacketConnection connection) {
		System.err.println("Lost routing connection: " + this);
		//ensure that the stale underlying connection is closed. Otherwise, this will result in one-way only traffic
		//between two peers. We will not be able to connect to a peer who had restarted via localpeer.stop() --> localPeer.Start()
		this.router.onNeighborLost(this);
	}

	@Override
	public void onWillSwapUnderlyingConnection(PacketConnection connection) {}
	@Override
	public void onUnderlyingConnectionConnected(PacketConnection connection) {}
	@Override
	public void onNoPacketsLeft(PacketConnection connection) {}

	@Override
	public Set<PacketType> getHandledPacketTypes() {
		return this.router.getLinkStatePacketManager().getHandledPacketTypes();
	}

	@Override
	public void handlePacket(ByteBuffer data, PacketType type) {
		this.router.getLinkStatePacketManager().handlePacket(data, type, this.identifier);
	}
}
