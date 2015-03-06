package jReto.integration;


import jReto.meta.PeerConfiguration;
import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.LocalPeer;
import de.tum.in.www1.jReto.util.CountDown;

/**
 * Tests whether data is actually transferred correctly.
 * */
public class TransferDataIntegrityTest {
	@Test(timeout=1000)
	public void testTransferDataIntegrityDirect() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.directNeighborConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity2Hop() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.twoHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity4Hop() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.fourHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityNontrivial() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityDisconnectedPeers() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.configurationWithDisconnectedPeers());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity2HopMulticast() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.twoHopRoutedMulticastConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity4HopMulticast() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.fourHopRoutedMulticastConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityNontrivialMulticast() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.nontrivial2HopNetworkMulticastConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityDisconnectedPeersMulticast() {
		new TransferDataIntegrityTest().testTransferDataIntegrity(PeerConfiguration.multicastConfigurationWithDisconnectedPeers());
	}
	
	public void testTransferDataIntegrity(final PeerConfiguration peerConfiguration) {
		final int dataLength = 10000;

		final CountDown stopCountdown = new CountDown(peerConfiguration.destinations.size(), new Runnable() {
			@Override
			public void run() {
				peerConfiguration.runloop.stop();
			}
		});
		
		peerConfiguration.startAndExecuteAfterDiscovery(() -> {
			for (LocalPeer peer: peerConfiguration.destinations) {
				peer.setIncomingConnectionHandler((connectingPeer, connection) -> {
					connection.setOnTransfer((transferConnection, transfer) -> {
						transfer.setOnCompleteData((completeTransfer, data) -> {
							TestData.verify(data, dataLength);
							
							stopCountdown.countDown();
						});
					});
				});
			}
			
			Connection connection = peerConfiguration.peer1.connect(peerConfiguration.getMulticastDestinations(peerConfiguration.peer1));
			connection.send(TestData.generate(dataLength));
		});
	}
}
