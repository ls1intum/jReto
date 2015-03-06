package de.tum.in.www1.jReto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import de.tum.in.www1.jReto.LocalPeer.IncomingConnectionHandler;
import de.tum.in.www1.jReto.routing.Node;

/**
* A RemotePeer represents another peer in the network.
*
* You do not construct RemotePeer instances yourself; they are provided to you by the LocalPeer.
*
* This class can be used to establish and accept connections to/from those peers.
* */
public class RemotePeer {
	public static interface ConnectionCreatedCallback {
		void onConnectionCreated(Connection connection);
	}
	
    /** The LocalPeer that created this peer */
	private final LocalPeer localPeer;
    /** The node representing this peer on the routing level */
	private Node node;
	private IncomingConnectionHandler incomingConnectionHandler;
   
	/**
    * Internal initializer. See the class documentation about how to obtain RemotePeer instances.
    * @param node The node representing the the peer on the routing level.
    * @param localPeer The local peer that created this peer
    */
	RemotePeer(LocalPeer localPeer, Node node) {
		this.localPeer = localPeer;
		this.node = node;
	}
	
    /** Returns this peer's unique identifier. */
	public UUID getUniqueIdentifier() {
		return RemotePeer.this.node.getIdentifier();
	}
	
    /**
     * Establishes a connection to this peer. The connection can only be used to send data, not to receive data.
     * @return A Connection object. It can be used to send data immediately (the transfers will be started once the connection was successfully established).
     * */
	public Connection connect() {
		return this.localPeer.connect(new HashSet<>(Arrays.asList(this)));
	}
	
	/**
	 * Sets the incomingConnectionHandler. When you set it, it will be called when any incoming connections from this peer are received.
	 * The LocalPeer will not report incoming connections from this peer once this property is set.
	 * */
	public void setIncomingConnectionHandler(IncomingConnectionHandler incomingConnectionHandler) {
		this.incomingConnectionHandler = incomingConnectionHandler;
	}
	public IncomingConnectionHandler getIncomingConnectionHandler() {
		return this.incomingConnectionHandler;
	}
	Node getNode() {
		return this.node;
	}
}
