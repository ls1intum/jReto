package jReto.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.tum.in.www1.jReto.LocalPeer;
import de.tum.in.www1.jReto.RemotePeer;
import de.tum.in.www1.jReto.module.api.Module;
import de.tum.in.www1.jReto.routing.Router;
import jReto.module.dummy.DummyModule;
import jReto.module.dummy.DummyNetworkInterface;
import jReto.util.RunLoop;

/**
 * A PeerConfiguration provides various configurations of peers, used to test communication between two peers.
 * */
public class PeerConfiguration {
	public final RunLoop runloop;
	public final LocalPeer peer1;
	public final LocalPeer peer2;
	public final List<LocalPeer> participatingPeers;
	public final List<LocalPeer> reachablePeers;
	public final List<LocalPeer> destinations;

	public static List<LocalPeer> destinationPeerList(LocalPeer peer1, List<LocalPeer> participatingPeers) {
		List<LocalPeer> result = new ArrayList<>(participatingPeers);
		result.remove(peer1);
		return result;
	}
	
	public PeerConfiguration(RunLoop runloop, LocalPeer peer1, LocalPeer peer2, List<LocalPeer> participatingPeers) {
		this(runloop, peer1, peer2, participatingPeers, participatingPeers);
	}
	public PeerConfiguration(RunLoop runloop, LocalPeer peer1, LocalPeer peer2, List<LocalPeer> participatingPeers, List<LocalPeer> reachablePeers) {
		this(runloop, peer1, peer2, participatingPeers, reachablePeers, Arrays.asList(peer2));
	}
	public PeerConfiguration(RunLoop runloop, LocalPeer peer1, LocalPeer peer2, List<LocalPeer> participatingPeers, List<LocalPeer> reachablePeers, List<LocalPeer> destinations) {
		this.runloop = runloop;
		this.peer1 = peer1;
		this.peer2 = peer2;
		this.participatingPeers = participatingPeers;
		this.reachablePeers = reachablePeers;
		this.destinations = destinations;
	}
	
	public Set<UUID> getMulticastDestinationIdentifiers() {
		Set<UUID> results = new HashSet<>();
		for (LocalPeer peer : this.destinations) results.add(peer.getUniqueIdentifier());
		return results;
	}
	public Set<UUID> getReachablePeerIdentifiers() {
		Set<UUID> results = new HashSet<>();
		for (LocalPeer peer : this.reachablePeers) results.add(peer.getUniqueIdentifier());
		return results;
	}
	public Set<RemotePeer> getMulticastDestinations(LocalPeer peer) {
		HashSet<RemotePeer> destinations = new HashSet<>();
		Set<UUID> multicastIdentifiers = this.getMulticastDestinationIdentifiers();
		
		for (RemotePeer remotePeer : peer.getPeers()) {
			if (multicastIdentifiers.contains(remotePeer.getUniqueIdentifier())) destinations.add(remotePeer);
		}
		
		return destinations;
	}
	public void startAndExecuteAfterDiscovery(final Runnable onDiscoveryComplete) {
		final Map<LocalPeer, Set<UUID>> discoveredPeersByPeer = new HashMap<>();
		
		for (final LocalPeer reachablePeer : reachablePeers) {
			discoveredPeersByPeer.put(reachablePeer, new HashSet<UUID>());
			discoveredPeersByPeer.get(reachablePeer).add(reachablePeer.getUniqueIdentifier());
		}
		for (LocalPeer participatingPeer : participatingPeers) {
			if (reachablePeers.contains(participatingPeer)) {
				participatingPeer.start(peer -> {
					System.err.println("Discovered peer: "+peer.getUniqueIdentifier());
					
					discoveredPeersByPeer.get(participatingPeer).add(peer.getUniqueIdentifier());
					
					boolean allDiscovered = true;
					Set<UUID> allReachablePeers = getReachablePeerIdentifiers();
					for (Set<UUID> identifiers : discoveredPeersByPeer.values()) {
						if (!identifiers.equals(new HashSet<>(allReachablePeers))) {
							allDiscovered = false;
						}
					}
					
					if (allDiscovered) onDiscoveryComplete.run();
				}, peer -> {});
			} else {
				participatingPeer.start(peer -> {}, peer -> {});
			}
		}
		
		this.runloop.start();
	}
	
	public static LocalPeer createLocalPeer(RunLoop runloop, Module... modules) {
		return new LocalPeer(UUID.randomUUID(), Arrays.asList(modules), runloop, new Router.BroadcastDelaySettings(0.2, 0.1));
	}
	
	/**
	 * A simple peer configuration that allows direct communication.
	 * */
	public static PeerConfiguration directNeighborConfiguration() {
		final RunLoop runloop = new RunLoop(false);

		DummyNetworkInterface manager = new DummyNetworkInterface("test", runloop, 1024, 1);
		final LocalPeer localPeer1 = createLocalPeer(runloop, new DummyModule(manager, runloop));
		final LocalPeer localPeer2 = createLocalPeer(runloop, new DummyModule(manager, runloop));
		return new PeerConfiguration(runloop, localPeer1, localPeer2, Arrays.asList(localPeer1, localPeer2));
	}
	
	/**
	 * A peer configuration that allows communication via 2 hops.
	 * */
	public static PeerConfiguration twoHopRoutedConfiguration() {
		final RunLoop runloop = new RunLoop(false);

		DummyNetworkInterface manager1 = new DummyNetworkInterface("test1", runloop, 1024, 1);
		DummyNetworkInterface manager2 = new DummyNetworkInterface("test2", runloop, 1024, 1);
		
		final LocalPeer localPeer1 = createLocalPeer(runloop, new DummyModule(manager1, runloop));
		final LocalPeer localPeer2 = createLocalPeer(runloop, new DummyModule(manager2, runloop));
		final LocalPeer localPeer3 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager2, runloop));
		return new PeerConfiguration(runloop, localPeer1, localPeer2, Arrays.asList(localPeer1, localPeer2, localPeer3));
	}

	/**
	 * A peer configuration that allows communication via 2 hops and multicasts to both peers.
	 * */
	public static PeerConfiguration twoHopRoutedMulticastConfiguration() {
		PeerConfiguration config = PeerConfiguration.twoHopRoutedConfiguration();
		
		return new PeerConfiguration(config.runloop, config.peer1, config.peer2, config.participatingPeers, config.reachablePeers, destinationPeerList(config.peer1, config.participatingPeers));
	}
	
	/**
	 * A peer configuration that allows communication via 4 hops.
	 * */
	public static PeerConfiguration fourHopRoutedConfiguration() {
		final RunLoop runloop = new RunLoop(false);

		DummyNetworkInterface manager1 = new DummyNetworkInterface("test1", runloop, 1024, 1);
		DummyNetworkInterface manager2 = new DummyNetworkInterface("test2", runloop, 1024, 1);
		DummyNetworkInterface manager3 = new DummyNetworkInterface("test3", runloop, 1024, 1);
		DummyNetworkInterface manager4 = new DummyNetworkInterface("test4", runloop, 1024, 1);
		
		final LocalPeer localPeer1 = createLocalPeer(runloop, new DummyModule(manager1, runloop));
		final LocalPeer localPeer4 = createLocalPeer(runloop, new DummyModule(manager4, runloop));
		final LocalPeer localPeer12 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager2, runloop));
		final LocalPeer localPeer23 = createLocalPeer(runloop, new DummyModule(manager2, runloop), new DummyModule(manager3, runloop));
		final LocalPeer localPeer34 = createLocalPeer(runloop, new DummyModule(manager3, runloop), new DummyModule(manager4, runloop));
		return new PeerConfiguration(runloop, localPeer1, localPeer4, Arrays.asList(localPeer1, localPeer4, localPeer12, localPeer23, localPeer34));
	}
	
	/**
	 * A peer configuration that allows communication via 4 hops and multicasts to all peers.
	 * */
	public static PeerConfiguration fourHopRoutedMulticastConfiguration() {
		PeerConfiguration config = PeerConfiguration.fourHopRoutedConfiguration();
		
		return new PeerConfiguration(config.runloop, config.peer1, config.peer2, config.participatingPeers, config.reachablePeers, destinationPeerList(config.peer1, config.participatingPeers));
	}
	
	/**
	 * A peer configuration that contains a direct route, but a cheaper route via another peer (cost: 10 vs. 2). 
	 * */
	public static PeerConfiguration nontrivial2HopNetworkConfiguration() {
		final RunLoop runloop = new RunLoop(false);

		DummyNetworkInterface manager1 = new DummyNetworkInterface("test1", runloop, 1024, 1);
		DummyNetworkInterface manager2 = new DummyNetworkInterface("test2", runloop, 1024, 1);
		DummyNetworkInterface manager3 = new DummyNetworkInterface("test3", runloop, 1024, 10);
		DummyNetworkInterface manager4 = new DummyNetworkInterface("test4", runloop, 1024, 15);
		
		final LocalPeer localPeer1 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager3, runloop));
		final LocalPeer localPeer2 = createLocalPeer(runloop, new DummyModule(manager2, runloop), new DummyModule(manager3, runloop));
		final LocalPeer localPeer3 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager2, runloop), new DummyModule(manager4, runloop));
		final LocalPeer localPeer4 = createLocalPeer(runloop, new DummyModule(manager4, runloop));
		return new PeerConfiguration(runloop, localPeer1, localPeer2, Arrays.asList(localPeer1, localPeer2, localPeer3, localPeer4));
	}
	
	/**
	 * A peer configuration that contains a direct route, but a cheaper route via another peer (cost: 10 vs. 2). 
	 * */
	public static PeerConfiguration nontrivial2HopNetworkMulticastConfiguration() {
		PeerConfiguration config = PeerConfiguration.fourHopRoutedConfiguration();
		
		return new PeerConfiguration(config.runloop, config.peer1, config.peer2, config.participatingPeers, config.reachablePeers, destinationPeerList(config.peer1, config.participatingPeers));
	}
	
	/**
	 * A peer configuration that includes two peers that cannot connect to peer1 and peer2.
	 * */
	public static PeerConfiguration configurationWithDisconnectedPeers() {
		final RunLoop runloop = new RunLoop(false);

		DummyNetworkInterface manager1 = new DummyNetworkInterface("test1", runloop, 1024, 1);
		DummyNetworkInterface manager2 = new DummyNetworkInterface("test2", runloop, 1024, 1);
		DummyNetworkInterface manager3 = new DummyNetworkInterface("test3", runloop, 1024, 10);
		DummyNetworkInterface manager4 = new DummyNetworkInterface("test4", runloop, 1024, 15);
		
		final LocalPeer localPeer1 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager2, runloop));
		final LocalPeer localPeer2 = createLocalPeer(runloop, new DummyModule(manager1, runloop), new DummyModule(manager2, runloop));
		final LocalPeer localPeer3 = createLocalPeer(runloop, new DummyModule(manager3, runloop), new DummyModule(manager4, runloop));
		final LocalPeer localPeer4 = createLocalPeer(runloop, new DummyModule(manager3, runloop));
		return new PeerConfiguration(runloop, localPeer1, localPeer2, Arrays.asList(localPeer1, localPeer2, localPeer3, localPeer4),  Arrays.asList(localPeer1, localPeer2));
	}
	
	/**
	 * A peer configuration that contains a direct route, but a cheaper route via another peer (cost: 10 vs. 2). 
	 * */
	public static PeerConfiguration multicastConfigurationWithDisconnectedPeers() {
		PeerConfiguration config = PeerConfiguration.configurationWithDisconnectedPeers();
		
		return new PeerConfiguration(config.runloop, config.peer1, config.peer2, config.participatingPeers, config.reachablePeers, destinationPeerList(config.peer1, config.reachablePeers));
	}
	
}
