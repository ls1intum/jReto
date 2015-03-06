package jReto.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jReto.util.Condition;
import jReto.util.OrderVerifier;
import jReto.meta.PeerConfiguration;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.LocalPeer;

public class BasicConnectionTest {
	/**
	 * Starts two networks (for easier understanding, imagine they run on two different devices); verifies that, when establishing a connection, the delegate methods are called correctly.
	 * Note: onConnect is not called on the side that handles the incoming connection, because the connection is already connected when onConnection is called.
	 * */
	@Test(timeout=1000000)
	public void testConnectionEstablishmentAndCloseWithDirectNeighbor() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.directNeighborConfiguration());
	}

	/**
	 * Tests connection establishment with 2-hop routing.
	 * */
	@Test(timeout=1000000) 
	public void testConnectionEstablishmentAndCloseWith2Hops() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.twoHopRoutedConfiguration());
	}
	
	/**
	 * Tests connection establishment with 4-hop routing.
	 * */
	@Test(timeout=1000) 
	public void testConnectionEstablishmentAndCloseWith4Hops() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.fourHopRoutedConfiguration());
	}
	
	/**
	 * Tests connection establishment with 4-hop routing.
	 * */
	@Test(timeout=1000) 
	public void testConnectionEstablishmentAndCloseWithNontrivialConfiguration() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	
	/**
	 * Tests connection establishment with 2-hop routing.
	 * */
	@Test(timeout=1000000) 
	public void testMulticastConnectionEstablishmentAndCloseWith2Hops() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.twoHopRoutedMulticastConfiguration());
	}
	
	/**
	 * Tests connection establishment with 4-hop routing.
	 * */
	@Test(timeout=1000000) 
	public void testMulticastConnectionEstablishmentAndCloseWith4Hops() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.fourHopRoutedMulticastConfiguration());
	}
	
	/**
	 * Tests connection establishment with 4-hop routing.
	 * */
	@Test(timeout=10000000) 
	public void testMulticastConnectionEstablishmentAndCloseWithNontrivialConfiguration() {
		this.testConnectionEstablishmentAndClose(PeerConfiguration.nontrivial2HopNetworkMulticastConfiguration());
	}
	
	public void testConnectionEstablishmentAndClose(final PeerConfiguration peerConfiguration) {
		final Condition onConnectWasCalled1 = new Condition("onConnect was called in peer1", true, true);
		final Condition onCloseWasCalled1 = new Condition("onClose was called in peer1", true, true);
		final Condition allConnectionsAccepted = new Condition("onConnect was called in all remote peers", true, true);
		final Condition allConnectionsClosed = new Condition("onConnect was called in all remote peers", true, true);
		
		final Map<UUID, Condition> onCloseCalledConditions = new HashMap<>();
		final OrderVerifier orderVerifier = new OrderVerifier();
		final Set<Connection> allConnections = new HashSet<Connection>();
		
		peerConfiguration.startAndExecuteAfterDiscovery(new Runnable() {
			@Override
			public void run() {
				for (final LocalPeer destination : peerConfiguration.destinations) destination.setOnConnection((peer, connection) -> {
					connection.setOnClose(c -> {
						/* 5 */ orderVerifier.check(5);
						allConnections.remove(c);
						if (allConnections.size() == 0) allConnectionsClosed.confirm();
					});

					allConnections.add(connection);
					if (allConnections.size() == peerConfiguration.destinations.size()) allConnectionsAccepted.confirm();
				});

				Connection establishedConnection  = peerConfiguration.peer1.connect(peerConfiguration.getMulticastDestinations(peerConfiguration.peer1));
				establishedConnection.setOnConnect(c -> {
					/* 3 */ orderVerifier.check(3);
					onConnectWasCalled1.confirm();
					System.err.println("Connected successfully.");
					peerConfiguration.runloop.execute(() -> {
						/* 4 */ orderVerifier.check(4);
						System.out.println("Closing.");
						c.close();
					});
				});
				establishedConnection.setOnClose(c -> {
					/* 5 */ orderVerifier.check(5);

					onCloseWasCalled1.confirm();
					System.err.println("Close on connecting s");
					peerConfiguration.runloop.execute(() -> {
						/* 6 */ orderVerifier.check(6);

						peerConfiguration.runloop.stop();
							
						Condition.verifyAll(onConnectWasCalled1, onCloseWasCalled1);
						Condition.verifyAll(onCloseCalledConditions.values());
					});
				});
			}
		});
	}
}
