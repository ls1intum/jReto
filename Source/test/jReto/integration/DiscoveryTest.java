package jReto.integration;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jReto.meta.PeerConfiguration;
import jReto.module.dummy.DummyModule;
import jReto.module.dummy.DummyNetworkInterface;
import jReto.util.RunLoop;

import org.junit.Test;

import de.tum.in.www1.jReto.LocalPeer;
import de.tum.in.www1.jReto.util.CountDown;

public class DiscoveryTest {
	/**
	 * Test discovery with various configurations.
	 * */
	@Test(timeout=1000000)
	public void testPeerDiscoveryDirect() {
		this.testPeerDiscovery(PeerConfiguration.directNeighborConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerDiscovery2Hop() {
		this.testPeerDiscovery(PeerConfiguration.twoHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerDiscovery4Hop() {
		this.testPeerDiscovery(PeerConfiguration.fourHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerDiscoveryNontrivial() {
		this.testPeerDiscovery(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerDiscoveryDisconnectedPeers() {
		this.testPeerDiscovery(PeerConfiguration.configurationWithDisconnectedPeers());
	}
	
	public void testPeerDiscovery(final PeerConfiguration configuration) {
		final Set<UUID> reachablePeerIdentifiers = new HashSet<>();
		for (LocalPeer reachablePeer : configuration.reachablePeers) {
			reachablePeerIdentifiers.add(reachablePeer.getUniqueIdentifier());
		}
		
		final Set<UUID> discoveredIdentifiers = new HashSet<>();
		discoveredIdentifiers.add(configuration.peer1.getUniqueIdentifier());
		
		final CountDown peerCountDown = new CountDown((reachablePeerIdentifiers.size()-1) * 2, new Runnable() {
			@Override
			public void run() {
				assertTrue("Not all peers were discovered. Discovered: "+discoveredIdentifiers+", Discoverable: "+reachablePeerIdentifiers, discoveredIdentifiers.equals(reachablePeerIdentifiers));
				
				configuration.runloop.stop();
			}
		});
		
		for (LocalPeer peer : configuration.participatingPeers) { 
			if (peer == configuration.peer1) {
				peer.start(discoveredPeer -> {
					assertTrue("Incorrect peer name discovered: "+peer.getUniqueIdentifier()+", reachable are: "+reachablePeerIdentifiers, reachablePeerIdentifiers.contains(peer.getUniqueIdentifier()));
					assertTrue("Incorrect peer discovered: Peers should not discover themselves.", !discoveredPeer.getUniqueIdentifier().equals(configuration.peer1.getUniqueIdentifier()));
					discoveredIdentifiers.add(discoveredPeer.getUniqueIdentifier());
					peerCountDown.countDown();
				}, removedPeer -> {});
			} else if (peer == configuration.peer2) {
				peer.start(discoveredPeer -> {
					assertTrue("Incorrect peer name discovered: "+peer.getUniqueIdentifier()+", reachable are: "+reachablePeerIdentifiers, reachablePeerIdentifiers.contains(peer.getUniqueIdentifier()));
					assertTrue("Incorrect peer discovered: Peers should not discover themselves.", !discoveredPeer.getUniqueIdentifier().equals(configuration.peer2.getUniqueIdentifier()));
					peerCountDown.countDown();
				}, removedPeer -> {});
			} else {
				peer.start(discoveredPeer -> {}, removedPeer -> {});
			}
		}
	
		configuration.runloop.start();
	}
	/**
	 * Test removal with various configurations.
	 * */
	@Test(timeout=1000)
	public void testPeerRemovalDirect() {
		this.testPeerRemoval(PeerConfiguration.directNeighborConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerRemoval2Hop() {
		this.testPeerRemoval(PeerConfiguration.twoHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerRemoval4Hop() {
		this.testPeerRemoval(PeerConfiguration.fourHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerRemovalNontrivial() {
		this.testPeerRemoval(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	@Test(timeout=1000)
	public void testPeerRemovalDisconnectedPeers() {
		this.testPeerRemoval(PeerConfiguration.configurationWithDisconnectedPeers());
	}
	
	public void testPeerRemoval(final PeerConfiguration configuration) {
		final Set<UUID> reachablePeerIdentifiers = new HashSet<>();
		for (LocalPeer reachablePeer : configuration.reachablePeers) {
			reachablePeerIdentifiers.add(reachablePeer.getUniqueIdentifier());
		}
		
		final Set<UUID> removedPeerIdentifiers = new HashSet<>();
		
		final CountDown peerAddedCountDown = new CountDown((configuration.reachablePeers.size()-1) * 2, () -> {
			configuration.peer2.stop();
		});
		
		final CountDown peerRemovedCountDown = new CountDown(configuration.reachablePeers.size()-1, () -> {
			reachablePeerIdentifiers.removeAll(removedPeerIdentifiers);
			
			assertTrue("All peers were removed", reachablePeerIdentifiers.size() == 1);
			assertTrue("Peer itself may not be removed", reachablePeerIdentifiers.contains(configuration.peer2.getUniqueIdentifier()));
			configuration.runloop.stop();
		});
	
		for (LocalPeer peer : configuration.participatingPeers) {
			if (peer == configuration.peer1) {
				peer.start(discoveredPeer -> peerAddedCountDown.countDown(), removedPeer -> {});
			} else if (peer == configuration.peer2) {
				peer.start(discoveredPeer -> peerAddedCountDown.countDown(), removedPeer -> {
					removedPeerIdentifiers.add(removedPeer.getUniqueIdentifier());
					peerRemovedCountDown.countDown();
				});
			} else {
				peer.start(p -> {}, p -> {});
			}
		}
		
		configuration.runloop.start();
	}
	
	/**
	 * Tests whether adding/removing modules affects peer discovery correctly
	 * */
	@Test(timeout=1000)
	public void testPeerDiscoveryWithModuleModifications() {
		new PeerDiscoveryWithModuleModificationsTest().start();
	}
	static class PeerDiscoveryWithModuleModificationsTest {
		LocalPeer localPeer1;
		LocalPeer localPeer2;
		
		public void start() {
			final RunLoop runloop = new RunLoop(false);
			final DummyNetworkInterface networkInterface1 = new DummyNetworkInterface("test1", runloop, 1024, 1);
			final DummyNetworkInterface networkInterface2 = new DummyNetworkInterface("test2", runloop, 1024, 1);
			final DummyModule peer1Module1 = new DummyModule(networkInterface1, runloop);
			final DummyModule peer1Module2 = new DummyModule(networkInterface2, runloop);
			final DummyModule peer2Module1 = new DummyModule(networkInterface1, runloop);
			final DummyModule peer2Module2 = new DummyModule(networkInterface2, runloop);
			
			final CountDown peerAddedCountDown = new CountDown(2, new Runnable() {
				@Override
				public void run() {
					// This should not trigger any additional onPeerDiscovereds. If it did, the count down called would throw an exception.
					localPeer2.addModule(peer2Module2);
					
					// Allow possible discovery methods to be called, then continue.
					runloop.execute(new Runnable() {
						@Override
						public void run() {
							// By removing these two modules, the peers should not be able to discover each other and should be removed, triggering the peerRemovedCountDown.
							localPeer1.removeModule(peer1Module2);
							localPeer2.removeModule(peer2Module1);
						}
					});
				}
			});
			
			final CountDown peerRemovedCountDown = new CountDown(2, new Runnable() {
				@Override
				public void run() {
					runloop.stop();
				}
			});
			
			this.localPeer1 = new LocalPeer(Arrays.asList(peer1Module1, peer1Module2), runloop);
			this.localPeer2 = new LocalPeer(Arrays.asList(peer2Module1), runloop);

			localPeer1.start(discoveredPeer -> peerAddedCountDown.countDown(), removedPeer -> peerRemovedCountDown.countDown());
			localPeer2.start(discoveredPeer -> peerAddedCountDown.countDown(), removedPeer -> peerRemovedCountDown.countDown());
		
			runloop.start();
		}
	}
}