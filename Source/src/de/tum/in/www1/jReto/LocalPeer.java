package de.tum.in.www1.jReto;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.connectivity.PacketConnection;
import de.tum.in.www1.jReto.connectivity.ReliablitiyManager;
import de.tum.in.www1.jReto.connectivity.packet.ManagedConnectionHandshake;
import de.tum.in.www1.jReto.module.api.Module;
import de.tum.in.www1.jReto.routing.DefaultRouter;
import de.tum.in.www1.jReto.routing.Node;
import de.tum.in.www1.jReto.routing.Router;
import de.tum.in.www1.jReto.routing.SinglePacketHelper;

/**
 * @author jasamer
 *
 * A LocalPeer advertises the local peer in the network and browses for other peers.
 * 
 * It requires one or more Modules to accomplish this. Two Modules that come with Reto are the WlanModule and the RemoteP2P module.
 * 
 * The LocalPeer can also be used to establish multicast connections to multiple other peers.
 */
public class LocalPeer {
	/** Used to notify about discovered peers. */
	public static interface PeerDiscoveryHandler {
		void onPeerDiscovered(RemotePeer peer);
	}
	/** Used to notify about removed peers. */
	public static interface PeerRemovalHandler {
		void onPeerRemoved(RemotePeer peer);
	}
	/** Used to notify about incoming connections. */
	public static interface IncomingConnectionHandler {
		void onConnection(RemotePeer peer, Connection connection);
	}
	
	private PeerDiscoveryHandler peerDiscoveryHandler;
	private PeerRemovalHandler peerRemovalHandler;
	private IncomingConnectionHandler incomingConnectionHandler;
	
    /** The Executor used to execute all networking operations and callbacks */
	private final Executor executor;
	/** All know peers by their Node counterpart (provided by the Router). */
	private final Map<Node, de.tum.in.www1.jReto.RemotePeer> knownPeers;
	/** The Router instance used by the LocalPeer */
	private DefaultRouter router;
    /** This peer's unique identifier. If not specified in the constructor, it has a random value. */
	private UUID localPeerIdentifier;
	/** All connections that were established by this local peer. */
	private Map<UUID, PacketConnection> establishedConnections = new HashMap<>();
	/** All connections that were established to this local peer. */
	private Map<UUID, PacketConnection> incomingConnections = new HashMap<>();
	/** Whether browsing and advertisement was started. */
	private boolean isStarted = false;
	
	private ReliablitiyManager.PacketConnectionManager packetConnectionManager = new ReliablitiyManager.PacketConnectionManager() {
		@Override
		public void notifyConnectionClose(PacketConnection connection) {
			LocalPeer.this.removeConnection(connection);
		}
		
		@Override
		public void establishUnderlyingConnection(PacketConnection connection) {
			LocalPeer.this.reconnect(connection);
		}
	};
	
	/**
	 * Constructs a new LocalPeer object. A random identifier will be used for the LocalPeer. 
	 * Note that a LocalPeer is not functional without modules. You can add modules later with the addModule method.
	 * 
	 * @param executor The executor used to run all networking code with. The executor can be used to specifiy the thread that should be used.
	 */
	public LocalPeer(Executor executor) {
		this(UUID.randomUUID(), new ArrayList<Module>(), executor); 
	}
	/**
	 * Constructs a new LocalPeer object. A random identifier will be used for the LocalPeer.
	 * 
	 * @param modules A collection of modules used for the underlying networking functionality. For example: @see WlanModule, @see RemoteP2PModule.
	 * @param executor The executor used to run all networking code with. The executor can be used to specifiy the thread that should be used.
	 */
	public LocalPeer(Collection<Module> modules, Executor executor) {
		this(UUID.randomUUID(), modules, executor); 
	}
	
	/**
	 * Constructs a new LocalPeer object.
	 * 
	 * @param localPeerIdentifier The identifier used for the peer
	 * @param modules A collection of modules used for the underlying networking functionality. For example: @see WlanModule, @see RemoteP2PModule.
	 * @param executor The executor used to run all networking code with. The executor can be used to specifiy the thread that should be used.
	 */
	public LocalPeer(UUID localPeerIdentifier, Collection<Module> modules, Executor executor) {
		this(localPeerIdentifier, modules, executor, new Router.BroadcastDelaySettings(5, 0.5));
	}

	/**
	 * Constructs a new LocalPeer object.
	 * 
	 * @param localPeerIdentifier The identifier used for the peer
	 * @param modules A collection of modules used for the underlying networking functionality. For example: @see WlanModule, @see RemoteP2PModule.
	 * @param executor The executor used to run all networking code with. The executor can be used to specifiy the thread that should be used.
	 * @param broadcastDelaySettings Settings for the timing of routing information broadcast.
	 */
	public LocalPeer(UUID localPeerIdentifier, Collection<Module> modules, Executor executor, Router.BroadcastDelaySettings broadcastDelaySettings) {		
		this.localPeerIdentifier = localPeerIdentifier;
		this.executor = executor;
		this.knownPeers = new HashMap<Node, de.tum.in.www1.jReto.RemotePeer>();
		this.router = new DefaultRouter(localPeerIdentifier, executor, modules, broadcastDelaySettings);
		
		this.router.setHandler(new Router.RouterHandler() {
			@Override
			public void onRouteImproved(Router router, Node node) {
				LocalPeer.this.reconnectConnections(node);
			}
			
			@Override
			public void onNodeLost(Router router, Node node) {
				LocalPeer.this.removeNode(node);
			}
			
			@Override
			public void onNodeFound(Router router, Node node) {
				LocalPeer.this.addNode(node);
			}
		
			@Override
			public void onConnection(Router router, Node node, de.tum.in.www1.jReto.module.api.Connection connection) {
				LocalPeer.this.handleConnection(node, connection);
			}
		});
	}
	
    /** This peer's unique identifier. If not specified in the constructor, it has a random value. */
	public UUID getUniqueIdentifier() {
		return LocalPeer.this.localPeerIdentifier;
	}
    /** The set of peers currently reachable */
	public Collection<RemotePeer> getPeers() {
		return this.knownPeers.values();
	}
	
	public PeerDiscoveryHandler getPeerDiscoveryHandler() {
		return this.peerDiscoveryHandler;
	}
	public void setPeerDiscoveryHandler(PeerDiscoveryHandler peerDiscoveryHandler) {
		this.peerDiscoveryHandler = peerDiscoveryHandler;
	}
	public PeerRemovalHandler getPeerRemovalHandler() {
		return this.peerRemovalHandler;
	}
	public void setPeerRemovalHandler(PeerRemovalHandler peerRemovalHandler) {
		this.peerRemovalHandler = peerRemovalHandler;
	}
	public IncomingConnectionHandler getIncomingConnectionHandler() {
		return this.incomingConnectionHandler;
	}
	public void setIncomingConnectionHandler(IncomingConnectionHandler incomingConnectionHandler) {
		this.incomingConnectionHandler = incomingConnectionHandler;
	}
	
	/**
	 * Starts the local peer (i.e. it will advertise itself and browse for other peers).
	 * You need to set the incomingConnectionHandler property of the discovered RemotePeers, otherwise you will not be able to handle incoming connections.
	 * 
	 * @param peerDiscoveryHandler Called when a peer is discovered. You can use a lambda expression of the form "peer -> doSomething(peer)" as the parameter.
	 * @param peerRemovalHandler Called when a peer is lost/removed. You can use a lambda expression of the form "peer -> doSomething(peer)" as the parameter.
	 */
	public void start(PeerDiscoveryHandler peerDiscoveryHandler, PeerRemovalHandler peerRemovalHandler) {
		this.start(peerDiscoveryHandler, peerRemovalHandler, null);
	}
	
	/**
	 * Starts the local peer (i.e. it will advertise itself and browse for other peers).
	 * Incoming connections will be reported via the incomingConnectionHandler, unless the incomingConnectionHandler property of the RemotePeer that established the connection is set.
	 * 
	 * @param peerDiscoveryHandler Called when a peer is discovered. You can use a lambda expression of the form "peer -> doSomething(peer)" as the parameter.
	 * @param peerRemovalHandler Called when a peer is lost/removed. You can use a lambda expression of the form "peer -> doSomething(peer)" as the parameter.
	 * @param incomingConnectionHandler Called when a connection is established by a RemotePeer to the LocalPeer, and the RemotePeer's incomingConnectionHandler property is not set. You can, again, use a lambda expression as above. 
	 * 		  No connection is passed to the handler; you can obtain the Connection by calling one of the acceptConnection methods on the RemotePeer.
	 */
	public void start(PeerDiscoveryHandler peerDiscoveryHandler, PeerRemovalHandler peerRemovalHandler, IncomingConnectionHandler incomingConnectionHandler) {
		if (this.isStarted) {
			System.err.println("You attempted to start a LocalPeer that is already started. Nothing will happen.");
			return;
		}
		
		this.isStarted = true;
		this.setPeerDiscoveryHandler(peerDiscoveryHandler);
		this.setPeerRemovalHandler(peerRemovalHandler);
		this.setIncomingConnectionHandler(incomingConnectionHandler);
		this.router.start();
	}
	
	/**
	 * Stops the LocalPeer. It will no longer be advertised and will not browse for other peers.
	 * */
	public void stop() {
		if (!this.isStarted) {
			System.err.println("You attempted to stop a LocalPeer that is not started. Nothing will happen.");
			return;
		}
		
		this.isStarted = false;
		this.router.stop();
	}
	
   /**
    * Establishes a multicast connection to a set of peers.
    * @param destinations The RemotePeers to establish the connection with.
    * @return A Connection object. It can be used to send data immediately (the transfers will be started once the connection was successfully established).
    * */	
	public Connection connect(Set<RemotePeer> destinations) {
		Set<Node> destinationNodes = new HashSet<>();
		for (RemotePeer peer : destinations) destinationNodes.add(peer.getNode());
		
		UUID connectionIdentifier = UUID.randomUUID();
		PacketConnection packetConnection = new PacketConnection(null, connectionIdentifier, destinationNodes);
		this.establishedConnections.put(connectionIdentifier, packetConnection);
			
		final Connection transferConnection = new Connection(packetConnection, this.localPeerIdentifier, this.executor, true, this.packetConnectionManager);
		transferConnection.attemptReconnect();

		return transferConnection;
	}
	
	/**
	 * Adds a module to the LocalPeer.
	 * 
	 * @param module The module that should be added.
	 * */
	public void addModule(Module module) {
		this.router.addModule(module);
	}
	/**
	 * Removes a module from the LocalPeer.
	 * 
	 * @param module The module that should be removed.
	 * */
	public void removeModule(Module module) {
		this.router.removeModule(module);
	}
	/** Provides an existing RemotePeer for a given Node, or creates a new one if it is not known. */
	private de.tum.in.www1.jReto.RemotePeer providePeerForNode(Node node) {
		de.tum.in.www1.jReto.RemotePeer peer = this.knownPeers.get(node);
			
		if (peer == null) {
			peer = new RemotePeer(this, node);
			knownPeers.put(node, peer);
		}
			
		return peer;
	}
	/** Adds a Node */
	private void addNode(Node node) {
		if (this.knownPeers.containsKey(node)) return;
		final RemotePeer peer = this.providePeerForNode(node);
		
		this.peerDiscoveryHandler.onPeerDiscovered(peer);
	}
	/** Removes a Node */
	private void removeNode(final Node node) {
		RemotePeer removedPeer = this.knownPeers.get(node);
		this.knownPeers.remove(node);
		
		this.peerRemovalHandler.onPeerRemoved(removedPeer);
	}

	/**
	 * Handles an incoming connection.
	 * 
	 * @param node The node which established the connection
	 * @param connection The connection that was established
	 * */
	private void handleConnection(final Node node, final de.tum.in.www1.jReto.module.api.Connection connection) {
		SinglePacketHelper.read(connection, new SinglePacketHelper.OnPacketHandler() {
			@Override
			public void onPacket(ByteBuffer data) {
				ManagedConnectionHandshake handshake = ManagedConnectionHandshake.deserialize(data);
				if (handshake == null) {
					System.err.println("Received invalid packet, closing connection.");
					connection.close();
					return;
				}
				LocalPeer.this.handleConnection(node, connection, handshake.connectionIdentifier);
			}
		}, new SinglePacketHelper.OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Failed to receive ManagedConnectionHandshake.");
			}
		});
	}
	/** Handles an incoming connection with a know connection identifier.
	 * Called when ManagedConnectionHandshake was received, i.e. when all necessary information is available to deal with this connection.
	 * If the corresponding PacketConnection already exists, its underlying connection is swapped. Otherwise, a new Connection is created.
	 * 
	 * @param node The node which established the connection
	 * @param connection The connection that was established
	 * @param connectionIdentifier The identifier of the connection
	 * */
	private void handleConnection(Node node, de.tum.in.www1.jReto.module.api.Connection connection, UUID connectionIdentifier) {
		boolean needsToReportPeer = this.knownPeers.get(node) == null;
		RemotePeer peer = this.providePeerForNode(node);
		
		if (needsToReportPeer) this.peerDiscoveryHandler.onPeerDiscovered(peer);
		
		PacketConnection packetConnection = this.incomingConnections.get(connectionIdentifier);
		
		if (packetConnection != null) {
			packetConnection.swapUnderlyingConnection(connection);
		} else {
			packetConnection = new PacketConnection(connection, connectionIdentifier, new HashSet<>(Arrays.asList(peer.getNode())));
			this.incomingConnections.put(connectionIdentifier, packetConnection);
			Connection transferConnection = new Connection(packetConnection, this.localPeerIdentifier, this.executor, false, this.packetConnectionManager);
			
			if (peer.getIncomingConnectionHandler() != null) {
				peer.getIncomingConnectionHandler().onConnection(peer, transferConnection);
			} else if (this.getIncomingConnectionHandler() != null) {
				this.getIncomingConnectionHandler().onConnection(peer, transferConnection);
			} else {
				System.err.println("An incoming connection was received, but onConnection is not set. Set it either in your LocalPeer instance ("+this+"), or in the RemotePeer which established the connection ("+peer+").");
			}
		}
	}
	/**
	 * Establishes a new underlying connection for a given packet connection using the Router.
	 * */
	private void establishUnderlyingConnection(final PacketConnection packetConnection) {
		if (packetConnection.getIsEstablishingConnection()) {
			return;
		}
		
		packetConnection.setIsEstablishingConnection(true);
		
		this.router.establishMulticastConnection( packetConnection.getDestinations(), new Router.OnConnectionHandler() {
			@Override
			public void onConnect(final de.tum.in.www1.jReto.module.api.Connection connection) {
				SinglePacketHelper.write(connection, new ManagedConnectionHandshake(packetConnection.getConnectionIdentifier()), new SinglePacketHelper.OnSuccessHandler() {
					@Override
					public void onSuccess() {						
						packetConnection.swapUnderlyingConnection(connection);
						packetConnection.setIsEstablishingConnection(false);
					}
				}, new SinglePacketHelper.OnFailHandler() {
					@Override
					public void onFail() {
						packetConnection.setIsEstablishingConnection(false);
						System.err.println("Failed to send ManagedConnectionHandshake");
					}
				});
			}
		}, new Router.OnFailHandler() {
			@Override
			public void onFail() {
				packetConnection.setIsEstablishingConnection(false);
				System.err.println("Could not establish a connection to: "+packetConnection.getDestinations()+". Will retry soon.");
			}
		});
	}

	/** Reconnects all connections that were established to a certain node. */
	private void reconnectConnections(Node node) {
		for (PacketConnection packetConnection : this.establishedConnections.values()) this.reconnect(packetConnection);
	}

	private void removeConnection(PacketConnection connection) {
		this.establishedConnections.remove(connection.getConnectionIdentifier());
		this.incomingConnections.remove(connection.getConnectionIdentifier());
	}
	private void reconnect(PacketConnection connection) {		
		this.establishUnderlyingConnection(connection);
	}
	
	public void setOnConnection(IncomingConnectionHandler handler) {
		this.incomingConnectionHandler = handler;
	}
	public IncomingConnectionHandler getOnConnection() {
		return this.incomingConnectionHandler;
	}
}
