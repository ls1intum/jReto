package de.tum.in.www1.jReto.routing.algorithm;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable.Change.NowReachableInformation;
import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable.Change.RouteChangedInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * A LinkStateRoutingTable manages a graph of nodes in the network with type T.
 * 
 * Link state routing works by gathering information about the full network topology; i.e. for each node in the network, 
 * all of its neighbors are known eventually. Based on this information, the next hop to a node can be computed using a shortest path algorithm.
 * 
 * Advantages of link state routing (as opposed to distance vector routing) include that link state routing converges rather quickly and 
 * is not subject to the count-to-infinity problem; hence, no measures to combat this problem need to be taken. As the full network topology
 * is known to every node, rather advanced routing techniques can be implemented.
 * 
 * Disadvantages include that the link state information needs to be flooded through the network, causing higher overhead than link state protocols.
 * The memory and computational requirements are also higher. 
 * 
 * The LinkStateRoutingTable class is not responsible for distributing link state information across the network; 
 * however, it processes received link state information and can provide link state information for the local peer.
 * 
 * This routing table is designed to compute all next hops and path costs for all known nodes every time when new 
 * network topology information becomes available (e.g. neighbors added, updated or lost, and link state information received from 
 * any peer). 
 * 
 * These changes in the routing table are returned as a LinkStateRoutingTable.Change object. This object includes information about
 * nodes that became reachable or unreachable, or information about route changes to nodes that were reachable before.
 * */
public class LinkStateRoutingTable<T> {
	/**
	 * A Change object contains changes that occurred in the routing table caused by some operation.
	 * */
	public static class Change<T> {
		/**
		 * Contains information about nodes that became reachable.
		 * */
		public static class NowReachableInformation<T> {
			/** The node that became reachable */
			public final T node;
			/** The node that is the next hop for reaching this node. */
			public final T nextHop;
			/** The total cost for reaching this node. */
			public final double cost;
			
			/** Constructs a new NowReachableInformation object. */
			public NowReachableInformation(T node, T nextHop, double cost) {
				this.node = node;
				this.nextHop = nextHop;
				this.cost = cost;
			}
		}
		/**
		 * Contains informatiown about nodes that have changed routes.
		 * */
		public static class RouteChangedInformation<T> {
			/** The node that became reachable */
			public final T node;
			/** The node that is the next hop for reaching this node. */
			public final T nextHop;
			/** The previous total cost for reaching this node. */
			public final double oldCost;
			/** The total cost for reaching this node. */
			public final double cost;
			
			/** Constructs a new RouteChangedInformation object. */
			public RouteChangedInformation(T node, T nextHop, double oldCost, double cost) {
				this.node = node;
				this.nextHop = nextHop;
				this.oldCost = oldCost;
				this.cost = cost;
			}
		}
		
		/** Contains information about all nodes that are now reachable. */
		public final List<NowReachableInformation<T>> nowReachable;
		/** Contains all nodes that are now unreachable. */
		public final List<T> nowUnreachable;
		/** Contains information about all nodes that have changed routes. */
		public final List<RouteChangedInformation<T>> routeChanged;
		
		/** Constructs a new Change object. */
		public Change(List<NowReachableInformation<T>> nowReachable, List<T> nowUnreachable, List<RouteChangedInformation<T>> routeChanged) {
			this.nowReachable = nowReachable;
			this.nowUnreachable = nowUnreachable;
			this.routeChanged = routeChanged;
		}
		
		/** Returns whether this Change object is actually empty. */
		public boolean isEmpty() {
			return nowReachable.isEmpty() && nowUnreachable.isEmpty() && routeChanged.isEmpty();
		}
	}
	
	/**
	 * Stores neighbor information (the neighbor itself and the cost of reaching the neighbor).
	 * */
	public static class NeighborInformation<T> {
		/** The node object representing the neighbor. */
		public final T node;
		/** The cost of reaching this neighbor. */
		public final double cost;
		
		/** Constructs a new neighbor information object. */
		public NeighborInformation(T node, double cost) {
			this.node = node;
			this.cost = cost;
		}
	}

	/** A directed, weighted multigraph used to represent the network of nodes and their link states. */
	private final DirectedWeightedPseudograph<T, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
	/** The local node. In all neighbor related operations, the neighbor is considered a neighbor of this node. */
	private final T localNode;
	
	/** Constructs a new LinkStateRoutingTable. */
	public LinkStateRoutingTable(T localNode) {
		this.localNode = localNode;
		this.graph.addVertex(localNode);
	}
	
	/**
	 * Computes the changes to the routing table when updating or adding a new neighbor.
	 * If the neighbor is not yet known to the routing table, it is added.
	 * 
	 * @param neighbor The neighbor to update or add.
	 * @param cost The cost to reach that neighbor.
	 * @return A LinkStateRoutingTable.Change object representing the changes that occurred in the routing table.
	 * */
	public Change<T> getRoutingTableChangeForNeighborUpdate(final T neighbor, final double cost) {
		return this.trackGraphChanges(new Runnable() {
			@Override
			public void run() {
				LinkStateRoutingTable.this.updateNeighbor(neighbor, cost);
			}
		});
	}
	/**
	 * Computes the changes to the routing table when removing a neighbor.
	 * 
	 * @param neighbor The neighbor to remove
	 * @return A LinkStateRoutingTable.Change object representing the changes that occurred in the routing table.
	 * */
	public Change<T> getRoutingTableChangeForNeighborRemoval(final T neighbor) {
		return this.trackGraphChanges(new Runnable() {
			
			@Override
			public void run() {
				LinkStateRoutingTable.this.removeNeighbor(neighbor);
			}
		});
	}
	/**
	 * Computes the changes to the routing table when link state information is received for a given node.
	 * 
	 * @param node The node for which a list of neighbors (ie. link state information) was received.
	 * @param neighbors The node's neighbors.
	 * @return A LinkStateRoutingTable.Change object representing the changes that occurred in the routing table.
	 * */
	public Change<T> getRoutingTableChangeForLinkStateInformationUpdate(final T node, final List<NeighborInformation<T>> neighbors) {
		return this.trackGraphChanges(new Runnable() {
			
			@Override
			public void run() {
				LinkStateRoutingTable.this.updateLinkStateInformation(node, neighbors);
			}
		});
	}
	
	/** Returns a list of neighbors for the local node (ie. link state information). */
	public List<NeighborInformation<T>> getLinkStateInformation() {
		List<NeighborInformation<T>> linkStateInformation = new ArrayList<>();
		
		for (DefaultWeightedEdge edge : this.graph.outgoingEdgesOf(this.localNode)) {
			linkStateInformation.add(new NeighborInformation<T>(this.graph.getEdgeTarget(edge), this.graph.getEdgeWeight(edge)));
		}
		
		return linkStateInformation;
	}
	
	public Tree<T> getNextHopTree(Set<T> destinations) {
		for (T destination : destinations) {
			if (!this.graph.containsVertex(destination)) {
				System.err.println("You have attempted to connect to a destination that has not yet been added to the routing table. Most likely, it will be discovered soon.");
				return null;
			}
		}
		
		Set<T> participatingNodes = new HashSet<>(destinations);
		participatingNodes.add(this.localNode);
		
		return MinimumSteinerTreeApproximation.approximateSteinerTree(this.graph, localNode, participatingNodes);
	}

	/** Updates or adds a neighbor. */
	private void updateNeighbor(T neighbor, double cost) {
		if (this.graph.edgeSet().contains(neighbor)) this.graph.removeAllEdges(this.localNode, neighbor);
		this.graph.addVertex(neighbor);
		DefaultWeightedEdge edge = this.graph.addEdge(this.localNode, neighbor);
		this.graph.setEdgeWeight(edge, cost);
	}
	/** Removes a neighbor. */
	private void removeNeighbor(T neighbor) {
		this.graph.removeAllEdges(this.localNode, neighbor);
	}
	/** Updates link state information for a given node. */
	private void updateLinkStateInformation(T node, List<NeighborInformation<T>> neighbors) {
		// Remove all edges
		if (this.graph.vertexSet().contains(node)) {
			Set<DefaultWeightedEdge> outgoingEdges = new HashSet<>(this.graph.outgoingEdgesOf(node));
			this.graph.removeAllEdges(outgoingEdges);
		} else {
			this.graph.addVertex(node);
		}

		// Add new edges
		for (NeighborInformation<T> neighbor : neighbors) {
			this.graph.addVertex(neighbor.node);
			DefaultWeightedEdge edge = this.graph.addEdge(node, neighbor.node);
			this.graph.setEdgeWeight(edge, neighbor.cost);
		}
	}
	
	/**
	 * Computes a Change object for arbitrary modifications of the graph.
	 * 
	 * This method first computes the shortest paths to all reachable nodes in the graph, then runs the graph action, and then calculates all shortest paths again.
	 * 
	 * From changes in which nodes are reachable, and changes in the paths, a LinkStateRoutingTable.Change object is created.
	 * 
	 * @param graphAction A Runnable that is expected to perform some changes on the graph.
	 * @return A LinkStateRoutingTable.Change object representing the changes caused by the changes performed by the graphAction.
	 * */
	private Change<T> trackGraphChanges(Runnable graphAction) {
		// Compute shortest paths before and after execting the graph action
		Map<T, DijkstraShortestPath<T, DefaultWeightedEdge>> previousShortestPaths = new HashMap<>();
		
		for (T node : graph.vertexSet()) {
			DijkstraShortestPath<T, DefaultWeightedEdge> shortestPath = new DijkstraShortestPath<T, DefaultWeightedEdge>(this.graph, this.localNode, node);
			if (shortestPath.getPath() != null) previousShortestPaths.put(node, shortestPath);
		}

		graphAction.run();
		
		Map<T, DijkstraShortestPath<T, DefaultWeightedEdge>> updatedShortestPaths = new HashMap<>();
		
		for (T node : graph.vertexSet()) {
			DijkstraShortestPath<T, DefaultWeightedEdge> shortestPath = new DijkstraShortestPath<T, DefaultWeightedEdge>(this.graph, this.localNode, node);
			if (shortestPath.getPath() != null) updatedShortestPaths.put(node, shortestPath);
		}
		
		// Compute routing table changes from shortest paths.
		
		// 1. Nodes that are now reachable but weren't before.
		List<NowReachableInformation<T>> nowReachable = new ArrayList<>();
		Set<T> nowReachableNodes = new HashSet<T>(updatedShortestPaths.keySet());
		nowReachableNodes.removeAll(previousShortestPaths.keySet());
		for (T nowReachableNode : nowReachableNodes) {
			DijkstraShortestPath<T, DefaultWeightedEdge> shortestPath = updatedShortestPaths.get(nowReachableNode);
			DefaultWeightedEdge firstEdge = shortestPath.getPathEdgeList().get(0);
			nowReachable.add(new NowReachableInformation<T>(
				nowReachableNode, 
				this.graph.getEdgeTarget(firstEdge), 
				shortestPath.getPathLength())
			);
		}
		
		// 2. Nodes that were reachable before, but are now unreachable.
		Set<T> nowUnreachableNodes = new HashSet<T>(previousShortestPaths.keySet());
		nowUnreachableNodes.removeAll(updatedShortestPaths.keySet());
		List<T> nowUnreachable = new ArrayList<T>(nowUnreachableNodes);
		
		// 3. Nodes that were and are still reachable, but have a changed route.
		Set<T> stillReachable = new HashSet<T>(previousShortestPaths.keySet());
		stillReachable.retainAll(updatedShortestPaths.keySet());
		
		List<RouteChangedInformation<T>> routeChanged = new ArrayList<>();
		
		for (T node : stillReachable) {
			if (node == this.localNode) continue;
			
			DijkstraShortestPath<T, DefaultWeightedEdge> previousShortestPath = previousShortestPaths.get(node);
			DijkstraShortestPath<T, DefaultWeightedEdge> updatedShortestPath = updatedShortestPaths.get(node);
			
			boolean pathLengthChanged = previousShortestPath.getPathLength() != updatedShortestPath.getPathLength();
			boolean nextHopChanged = this.graph.getEdgeTarget(previousShortestPath.getPathEdgeList().get(0)) != this.graph.getEdgeTarget(updatedShortestPath.getPathEdgeList().get(0));
			
			if (pathLengthChanged || nextHopChanged) {
				routeChanged.add(new RouteChangedInformation<T>(
						node, 
						this.graph.getEdgeTarget(updatedShortestPath.getPathEdgeList().get(0)), 
						previousShortestPath.getPathLength(), 
						updatedShortestPath.getPathLength())
				);
			}
		}
		
		return new Change<>(nowReachable, nowUnreachable, routeChanged);
	}
}
