package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable;
import de.tum.in.www1.jReto.routing.algorithm.Tree;
import de.tum.in.www1.jReto.routing.packets.ConnectionPurpose;
import de.tum.in.www1.jReto.routing.packets.LinkHandshake;
import de.tum.in.www1.jReto.routing.packets.LinkStatePacket;
import de.tum.in.www1.jReto.routing.packets.MulticastHandshake;
import de.tum.in.www1.jReto.routing.packets.RoutedConnectionEstablishedConfirmationPacket;
import de.tum.in.www1.jReto.util.RepeatedExecutor;

/**
* The Router class is responsible for discovering remote peers (represented via the Node class) in the network (both directly and indirectly reachable ones), 
* and establish connections to those peers.
*
* In this implementation, this involves the following tasks:
* 
* - Using of Reto Modules to discover direct neighbors and advertise the local peer (this is implemented in the DefaultRouter subclass).
* - Distribution of routing information using a flooding algorithm.
* - Building a link-state routing table based on that information.
* - Computing reachability information about other nodes when new routing information is received and informing the delegate about changes in reachabilty
* - Establishing connections to other peers, including routed connections
* - Acting as a router for other nodes, i.e. forwarding data from one peer to another, without handling the data on the local peer
* - Supporting multicast connections (computing routes, handling incoming underlying connections accordingly)
*/
public class Router {
	public static class BroadcastDelaySettings {
		public final double regularDelay;
		public final double shortDelay;
		
		public BroadcastDelaySettings(double regularDelay, double shortDelay) {
			this.regularDelay = regularDelay;
			this.shortDelay = shortDelay;
		}
	}
	
	public static interface RouterHandler {
		void onNodeFound(Router router, Node node);
		void onRouteImproved(Router router, Node node);
		void onNodeLost(Router router, Node node);
		void onConnection(Router router, Node node, Connection connection);
	}
	public static interface OnConnectionHandler {
		public void onConnect(Connection connection);
	}
	public static interface OnFailHandler {
		public void onFail();
	}
	
    /** The local peer's identifier */
	private final UUID identifier;
    /** The Executor used for networking related tasks. Delegate methods are also called on this Executor. */
	private final Executor executor;
    /** The Router's delegate. */
	private RouterHandler handler;

    /** A map from a node's UUID to the node for all Nodes known to the Router */
	private Map<UUID, Node> nodes = new HashMap<UUID, Node>();
    /** The set of Nodes that are neighbors of the local peer. */
	private Set<Node> neighbors = new HashSet<Node>();
    /** 
    * Forking connections act as a normal underling connection for the local peer, but forward received data to another peer in the background. 
    * This type of connection is used in multicast connections.
    * As the local peer may not hold a reference to the connection, it must be retained here.
    */
	private final Set<ForkingConnection> forwardingConnections = new HashSet<>();

    /** The linkStatePacketManager floods LinkStatePackets (i.e. routing information) through the network */
	private FloodingPacketManager linkStatePacketManager;
    /** Link state information packets are flooded periodically. The delayedLinkStateBroadcaster calls the appropriate methods in specified intervals. */
	private RepeatedExecutor delayedLinkStateBroadcaster;
    /** The routing table that builds a representation of the network using received link state information. */
	private final LinkStateRoutingTable<UUID> routingTable;
	
	public Executor getExecutor() {
		return this.executor;
	}
	
	public Collection<Node> getNeighborNodes() {
		return this.neighbors;
	}
	
	public UUID getLocalNodeIdentifier() {
		return this.identifier;
	}
	public void setHandler(RouterHandler handler) {
		this.handler = handler;
	}
	
    /** 
    * Constructs a Router.
    * @param identifier The local peer's identifier
    * @param linkStateBroadcastDelaySettings Settings related to the time intervals in which link state information is sent.
    * @param executor The Executor used for networking purposes. Delegate methods are also called on this executor.
    **/
	public Router(UUID identifier, Executor executor, BroadcastDelaySettings linkStateBroadcastDelaySettings) {		
		this.identifier = identifier;
		this.executor = executor;
		this.routingTable = new LinkStateRoutingTable<UUID>(identifier);

		this.linkStatePacketManager = new FloodingPacketManager(new FloodingPacketManager.PacketHandler() {
			@Override
			public Set<PacketType> getHandledPacketTypes() {
				return new HashSet<>(Arrays.asList(PacketType.LINK_STATE));
			}
			@Override
			public void handlePacket(ByteBuffer data, PacketType type) {
				LinkStatePacket packet = LinkStatePacket.deserialize(data);
				if (packet == null) {
					System.err.println("Received invalid LinkState packet.");
				} else {
					Router.this.handleLinkStatePacket(packet);
				}
			}		
		}, this);
		
		this.delayedLinkStateBroadcaster = new RepeatedExecutor(new Runnable() {
			@Override
			public void run() {
				Router.this.broadcastLinkStateInformation();
			}
		}, linkStateBroadcastDelaySettings.regularDelay, linkStateBroadcastDelaySettings.shortDelay, executor);
		this.delayedLinkStateBroadcaster.start();
	}
	
    /** Constructs a new node for a given identifier. */
	private Node provideNode(UUID nodeIdentifier) {
		Node node = this.nodes.get(nodeIdentifier);
		
		if (node == null) {
			node = new Node(this, nodeIdentifier, this.identifier);
			this.nodes.put(nodeIdentifier, node);
		}

		return node;
	}
    /** 
    * Adds an address for a given node.
    * The routing metadata connection for that node is established.
    * Finally, changes in reachability are computed, and the delegate is informed about any changes.
    */
	public void addAddress(UUID nodeIdentifier, Address address) {
		if (nodeIdentifier.equals(this.identifier)) return;
				
		Node node = this.provideNode(nodeIdentifier);
		Address oldBestAddress = node.getBestAddress();
		node.addAddress(address);
		
		if (oldBestAddress == null || (oldBestAddress.getCost() > address.getCost())) {
			node.establishRoutingConnection();
			this.updateNodes(this.routingTable.getRoutingTableChangeForNeighborUpdate(nodeIdentifier, address.getCost()));
		}
	}
    /** 
    * Removes an address for a node. 
    * Notifies the delegate about routing table changes.
    */
	public void removeAddress(UUID nodeIdentifier, Address address) {
		if (nodeIdentifier.equals(this.identifier)) return;
		if (!this.nodes.containsKey(nodeIdentifier)) throw new InvalidParameterException("Attempted to remove address from unknown node.");
		
		Node node = this.provideNode(nodeIdentifier);
		node.removeAddress(address);
		
		if (node.getAddresses().size() != 0) {
			this.updateNodes(this.routingTable.getRoutingTableChangeForNeighborUpdate(nodeIdentifier, node.getBestAddress().getCost()));
		} else {
			this.updateNodes(this.routingTable.getRoutingTableChangeForNeighborRemoval(nodeIdentifier));
		}
	}
	
    /*
    * Establishes a direct connection with a specific ConnectionPurpose to a given neighboring destination node
    *
    * @param destination The node to establish the connection with. Needs to be a direct neighbor of this node.
    * @param purpose The connection's purpose. Used to differentiate between routing metadata connections and standard routed connections.
    * @param onConnection A callback called when the connection is established.
    * @param onFail A closure called when an error occurs.
    */
	public void establishDirectConnection(Node destination, ConnectionPurpose purpose, final OnConnectionHandler onConnection, final OnFailHandler onFail) {
		Address bestAddress = destination.getBestAddress();
		
		if (bestAddress == null) {
			System.err.println("Failed to establish direct connection: no address is currently known for this peer. Most likely, it will be discovered soon.");
			onFail.onFail();
			return;
		}
		
		final Connection connection = bestAddress.createConnection();
		LinkHandshake linkHandshake = new LinkHandshake(this.identifier, purpose);
		
		SinglePacketHelper.write(connection, linkHandshake, new SinglePacketHelper.OnSuccessHandler() {
			@Override
			public void onSuccess() {
				onConnection.onConnect(connection);
			}
		}, new SinglePacketHelper.OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Failed to send LinkHandshake packet.");
				onFail.onFail();
			}
		});
	
		connection.connect();
	}
    /**
    * Handles direct connections. Expects to receive a LinkHandshake packet, which is sent by this method's counterpart, establishDirectConnection.
    * Depending on the ConnectionPurpose received, the connection is either used as a routing connection, or handled as a hop connection.
    */
	public void handleDirectConnection(final Connection connection) {
		SinglePacketHelper.read(connection, new SinglePacketHelper.OnPacketHandler() {
			@Override
			public void onPacket(ByteBuffer data) {
				LinkHandshake packet = LinkHandshake.deserialize(data);
				
				if (packet == null) {
					System.err.println("Received invalid LinkHandshake packet.");
					connection.close();
					return;
				}
								
				switch (packet.connectionPurpose) {
				case ROUTING_DATA_EXCHANGE_CONNECTION: 
					Router.this.provideNode(packet.peerIdentifier).handleRoutingConnection(connection);
					break;
				case ROUTED_CONNECTION: 
					Router.this.handleHopConnection(packet.peerIdentifier, connection);
					break;
				default: 
					System.err.println("Unknown connection purpose."); 
					connection.close();
					break;
				}
			}
		}, new SinglePacketHelper.OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Did not receive LinkHandshake, cannot handle this connection.");
			}
		});
	}

	private static class MutableBoolean {
		public boolean value = false;
	}
    /** 
    * Establishes all hop connections based on a nextHopTree. Hop connections are direct connections between two nodes and are part of a routed or multicast connection.
    * When establishing a routed connection, multiple peers may need to establish direct connections to each other. This method establishes the connections
    * that need to be established by the local peer.
    * 
    * This method establishes a direct connection for each child of the root (the local peer) of the next hop tree passed in. 
    * It then sends a MulticastHandshake over each connection. 
    *
    * If there are multiple children, a MulticastConnection is constructed that bundles the connections (i.e. it acts as a single connection that sends data to a set of subconnections).
    * If the local peer is a destination, a ForkingConnection used that is handled by the local peer as a normal underlying connection.
    *
    * @param destinationIdentifiers A set of UUIDs representing all destinations of the multicast connection for which the hop connections are used.
    * @param nextHopTree A Tree of UUIDs rooted at the local peer. This method establishes direct connections to each child, then sends the subtrees of the next hop tree to each child, such that they in turn can establish the next hop connections.
    * @param sourcePeerIdentifier The peer from which this connection originated.
    * @param onConnection A closure that is called when the next hop connections were established.
    * @param onFail A closure that is called when the connection establishement process failed.
    */
	public void establishHopConnections(final Set<UUID> destinationIdentifiers, final Tree<UUID> nextHopTree, final UUID sourcePeerIdentifier, final OnConnectionHandler onConnection, final OnFailHandler onFail) {
		final boolean useMulticastConnection = nextHopTree.children.size() > 1;
		final MulticastConnection multicastConnection = (useMulticastConnection) ? new MulticastConnection() : null;
		// A class wrapper for a boolean so the same instance can be accessed from all callbacks.
		final MutableBoolean failed = new MutableBoolean();
		
		for (final Tree<UUID> nextHopSubtree : nextHopTree.children) {
			this.establishDirectConnection(this.provideNode(nextHopSubtree.value), ConnectionPurpose.ROUTED_CONNECTION, new OnConnectionHandler() {
				@Override
				public void onConnect(final Connection connection) {
					MulticastHandshake handshake = new MulticastHandshake(sourcePeerIdentifier, destinationIdentifiers, nextHopSubtree);
					
					SinglePacketHelper.write(connection, handshake, new SinglePacketHelper.OnSuccessHandler() {			
						@Override
						public void onSuccess() {
							if (failed.value) {
								connection.close();
								return;
							}
							
							if (useMulticastConnection) { 
								multicastConnection.addSubconnection(connection);
								if (multicastConnection.getSubconnections().size() == nextHopTree.children.size()) {
									onConnection.onConnect(multicastConnection);
								}
							} else {
								onConnection.onConnect(connection);
							}
						}
					}, new SinglePacketHelper.OnFailHandler() {
						@Override
						public void onFail() {
							System.err.println("Could not send RoutingHandshake, connection establishment failed.");
							failed.value = true;
							
							if (multicastConnection != null) multicastConnection.close();
							
							onFail.onFail();
						}
					});
				}
			}, new OnFailHandler() {
				@Override
				public void onFail() {
					failed.value = true;
					
					if (multicastConnection != null) multicastConnection.close();
					
					onFail.onFail();
				}
			});
		}
	}
    
    /** 
    * Handles a incoming hop connection (i.e. a direct connection with a purpose of RoutedConnection).
    * Expects to receive a MulticastHandshake from the connection. 
    * 
    * If the next hop tree in the MulticastHandshake is a leaf, the connection can be handled as a multicast connection directly.
    * Otherwise, the nextHopTree is used to establish the next hop connections, to which data is forwarded. 
    * The connection is only handled on the local peer if it is a destination.
    *
    * @param connection The connection to handle
    */
	public void handleHopConnection(UUID hopSourceIdentifier, final Connection connection) {
		SinglePacketHelper.read(connection, new SinglePacketHelper.OnPacketHandler() {
			@Override
			public void onPacket(ByteBuffer data) {
				MulticastHandshake multicastHandshake = MulticastHandshake.deserialize(data);
				
				if (multicastHandshake == null) {
					System.err.println("Received invalid MulticastHandshake.");
					connection.close();
				}
				
				if (multicastHandshake.nextHopsTree.isLeaf()) {
					Router.this.handleMulticastConnection(multicastHandshake.sourcePeerIdentifier, connection);
				} else {
					Router.this.establishForwardingConnections(multicastHandshake.sourcePeerIdentifier, multicastHandshake.destinationIdentifiers, multicastHandshake.nextHopsTree, connection);
				}
			}
		}, new SinglePacketHelper.OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Failed to receive RoutingPacket. Cannot handle connection.");
			}
		});
	}
    /**
    * Establishes forwarding connections. Data received from an incoming connection is forwarded to an outgoing connection established via establishHopConnections,
    * and vice versa. If the local peer is a destination, a ForkingConnection is used to allow the local peer to handle the connection.
    * 
    * @param sourcePeerIdentifier The UUID of the peer which originally established the connection.
    * @param destinations A set of UUIDs that represent the destinations.
    * @param nextHopTree A tree rooted at the local peer representing the connections that still need to be established.
    * @param incomingConnection The connection from which data should be forwarded.
    */
	public void establishForwardingConnections(final UUID sourceNodeIdentifier, final Set<UUID> destinationNodeIdentifiers, Tree<UUID> nextHopsTree, final Connection incomingConnection) {
		this.establishHopConnections(destinationNodeIdentifiers, nextHopsTree, sourceNodeIdentifier, new OnConnectionHandler() {
			@Override
			public void onConnect(Connection outgoingConnection) {
				Connection connection = Router.this.createForwardingConnection(incomingConnection, outgoingConnection);
				if (destinationNodeIdentifiers.contains(Router.this.identifier)) Router.this.handleMulticastConnection(sourceNodeIdentifier, connection);
			}
		}, new OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println("Failed to establish connection to the next hops.");
				incomingConnection.close();
			}
		});
	}
    /** Creates a forking connection for an incoming and outgoing connection. */
	public Connection createForwardingConnection(Connection incomingConnection, Connection outgoingConnection) {
		ForkingConnection forwardingConnection = new ForkingConnection(incomingConnection, outgoingConnection, new ForkingConnection.CloseHandler() {
			@Override
			public void onClose(ForkingConnection connection) {
				Router.this.removeForwardingConnection(connection);
			}
		});
		
		this.forwardingConnections.add(forwardingConnection);
	
		return forwardingConnection;
	}
    /** Removes a forking connection. */
	public void removeForwardingConnection(ForkingConnection connection) { 
		this.forwardingConnections.remove(connection);
	}
	
   /** 
    * Establishes a multicast connection to a set of destinations. If the destinations set contains only one element, a unicast connection is established.
    *
    * Starts the hop connection establishement process. Expects a confirmation packet from all destinations to ensure that the connection was established successfully (these packets are sent by the handleMulticastConnection method). Finally sends a confirmation packet in turn, to signal that the connection is fully functional.
    *
    * @param destinations A set of destinations.
    * @param onConnection A closure that is called when the connection was fully established.
    * @param onFail A closure that is called when the connection establishment process fails.
    */
	public void establishMulticastConnection(final Set<Node> destinations, final OnConnectionHandler onConnection, final OnFailHandler onFail) {
		final Set<UUID> destinationIdentifiers = new HashSet<UUID>();
		for (Node destination : destinations) destinationIdentifiers.add(destination.getIdentifier());
		Tree<UUID> nextHopTree = this.routingTable.getNextHopTree(destinationIdentifiers);
		
		if (nextHopTree == null) {
			onFail.onFail();
			return;
		}
		
		final Set<UUID> receivedConfirmations = new HashSet<>();
		
		this.establishHopConnections(destinationIdentifiers, nextHopTree, this.identifier, new OnConnectionHandler() {
			@Override
			public void onConnect(final Connection connection) {
				SinglePacketHelper.read(connection, destinationIdentifiers.size(), new SinglePacketHelper.OnPacketHandler() {
					@Override
					public void onPacket(ByteBuffer data) {
						RoutedConnectionEstablishedConfirmationPacket packet = RoutedConnectionEstablishedConfirmationPacket.deserialize(data);
						if (packet == null) {
							System.err.println("Failed to receive confirmation packet.");
							connection.close();
							return;
						}
						receivedConfirmations.add(packet.source);
					}
				}, new SinglePacketHelper.OnSuccessHandler() {
					@Override
					public void onSuccess() {
						if (receivedConfirmations.equals(destinationIdentifiers)) {							
							SinglePacketHelper.write(connection, new RoutedConnectionEstablishedConfirmationPacket(identifier), new SinglePacketHelper.OnSuccessHandler() {	
								@Override
								public void onSuccess() {
									onConnection.onConnect(connection);
								}
							}, new SinglePacketHelper.OnFailHandler() {
								@Override
								public void onFail() {
									onFail.onFail();
								}
							});
						} else {
							System.err.println(identifier+": Did not receive all confirmation packets.");
							connection.close();
						}
					}
				}, new SinglePacketHelper.OnFailHandler() {
					@Override
					public void onFail() {
						System.err.println("Failed to receive confirmation packets.");
						onFail.onFail();
					}
				});
			}
		}, onFail);
	}
    /** 
    * Handles a multicast connection.
    * 
    * Sends a confirmation packet to the establisher of the connection. Then expects to receive a confirmation packet in turn.
    * Once the confirmation is received, the connection can be handled by the local peer.
    *
    * @param sourcepeerIdentifier The identifier of the peer that originally established the connection.
    * @param connection An underlying connection that should be handled.
    */
	public void handleMulticastConnection(final UUID sourceNodeIdentifier, final Connection connection) {
		SinglePacketHelper.write(connection, new RoutedConnectionEstablishedConfirmationPacket(this.identifier), new SinglePacketHelper.OnSuccessHandler() {
			@Override
			public void onSuccess() {
				SinglePacketHelper.read(connection, new SinglePacketHelper.OnPacketHandler() {
					@Override
					public void onPacket(ByteBuffer data) {
						RoutedConnectionEstablishedConfirmationPacket packet = RoutedConnectionEstablishedConfirmationPacket.deserialize(data);
						if (packet == null) {
							System.err.println("Received invalid data.");
							connection.close();
							return;
						}

						Router.this.handler.onConnection(Router.this, Router.this.provideNode(sourceNodeIdentifier), connection);
					}
				}, new SinglePacketHelper.OnFailHandler() {
					@Override
					public void onFail() {
						System.err.println(identifier+": Did not receive establishment confirmation.");
					}
				});
			}
		}, new SinglePacketHelper.OnFailHandler() {
			@Override
			public void onFail() {
				System.err.println(identifier+": Failed to send routed connection established confirmation.");
			}
		});
	}

    /**
    * Updates all nodes with routing table changes.
    * Informs the delegate about any changes in reachability.
    * @param change A RoutingTableChange object reflecting a set of changes in the routing table.
    */
	public void updateNodes(LinkStateRoutingTable.Change<UUID> change) {
		if (change.isEmpty()) return;
		
		System.out.println();
		System.out.println(" -- Updating with Routing Table Change (local identifier: "+this.identifier+") -- ");
		if (change.nowReachable.isEmpty()) { System.out.println(" - No new nodes reachable."); }
        else { System.out.println(" - Nodes now reachable: "); }
        
        for (LinkStateRoutingTable.Change.NowReachableInformation<UUID> nowReachable : change.nowReachable) {
            Node discoveredNode = this.provideNode(nowReachable.node);
            Node nextHopNode = this.provideNode(nowReachable.nextHop);
            
            discoveredNode.setNextHop(nextHopNode);
            discoveredNode.setCost((int)nowReachable.cost);
                        
            System.out.println("\t"+nowReachable.node+" (via "+nowReachable.nextHop+", cost: "+nowReachable.cost+")");
        }
        
        if (change.nowUnreachable.isEmpty()) { System.out.println(" - No nodes became unreachable."); }
        else { System.out.println(" - Nodes now unreachable: "); }
        
        for (UUID nowUnreachable : change.nowUnreachable) {
            Node unreachableNode = this.provideNode(nowUnreachable);
            
            unreachableNode.setNextHop(null);
            
            System.out.println("\t"+nowUnreachable+"");
        }
        
        if (change.routeChanged.isEmpty()) { System.out.println(" - No routes changed."); }
        else { System.out.println(" - Routes changed for nodes: "); }
        
        ArrayList<Node> nodesWithImprovedRoutes = new ArrayList<>();
        
        for (LinkStateRoutingTable.Change.RouteChangedInformation<UUID> routeChangeInformation : change.routeChanged) {
            Node changedNode = this.provideNode(routeChangeInformation.node);
            Node nextHopNode = this.provideNode(routeChangeInformation.nextHop);
            
            if (routeChangeInformation.oldCost > routeChangeInformation.cost) {
            	nodesWithImprovedRoutes.add(changedNode);
            }
            
            changedNode.setNextHop(nextHopNode);
            changedNode.setCost((int)routeChangeInformation.cost);
            
            System.out.println("\t"+routeChangeInformation.node+" (old cost: "+routeChangeInformation.oldCost+", new cost: "+routeChangeInformation.cost+", reachable via: "+routeChangeInformation.nextHop+")");
        }
        
        System.out.println();
        
        change.nowReachable.stream().map(nowReachableInfo -> this.provideNode(nowReachableInfo.node)).forEach(node -> this.handler.onNodeFound(this, node));
        change.nowUnreachable.stream().map(nowUnreachable -> this.provideNode(nowUnreachable)).forEach(node -> this.handler.onNodeLost(this, node));
        nodesWithImprovedRoutes.forEach(node -> this.handler.onRouteImproved(this, node));
	}
    /**
    * Handles a received link state packet and updates the routing table.
    */
	public void handleLinkStatePacket(LinkStatePacket packet) {		
		this.updateNodes(this.routingTable.getRoutingTableChangeForLinkStateInformationUpdate(packet.peerIdentifier, packet.neighbors));
	}
    /** Broadcasts link state information using the linkStatePacketManager. */
	public void broadcastLinkStateInformation() {		
		LinkStatePacket packet = new LinkStatePacket(this.identifier, this.routingTable.getLinkStateInformation());
		this.linkStatePacketManager.floodPacket(packet);
	}
	
    /** Called by a Node when it became directly reachable. */
	public void onNeighborReachable(final Node node) {
		this.neighbors.add(node);
		this.updateNodes(this.routingTable.getRoutingTableChangeForNeighborUpdate(node.getIdentifier(), node.getCost()));
		this.delayedLinkStateBroadcaster.runActionInShortDelay();
	}
	
    /** Called by a Node when it lost its neighbor status. */
	public void onNeighborLost(Node node) {
		this.neighbors.remove(node);
		this.updateNodes(this.routingTable.getRoutingTableChangeForNeighborRemoval(node.getIdentifier()));
		this.delayedLinkStateBroadcaster.runActionInShortDelay();
	}
	public void disconnectAll() {
		for (Node neighbor : this.neighbors) {
			neighbor.closeRoutingConnection();
		}
	}
	public FloodingPacketManager getLinkStatePacketManager() {
		return this.linkStatePacketManager;
	}
}
