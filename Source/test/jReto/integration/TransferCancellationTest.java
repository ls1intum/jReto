package jReto.integration;

import static org.junit.Assert.*;
import jReto.meta.PeerConfiguration;
import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.RemotePeer;
import de.tum.in.www1.jReto.connectivity.Transfer;
import de.tum.in.www1.jReto.util.CountDown;

public class TransferCancellationTest {
	@Test(timeout=1000)
	public void testTransferDataIntegrityDirect() {
		new TransferCancellationTest().testTransferCancellation(PeerConfiguration.directNeighborConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity2Hop() {
		new TransferCancellationTest().testTransferCancellation(PeerConfiguration.twoHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrity4Hop() {
		new TransferCancellationTest().testTransferCancellation(PeerConfiguration.fourHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityNontrivial() {
		new TransferCancellationTest().testTransferCancellation(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferDataIntegrityDisconnectedPeers() {
		new TransferCancellationTest().testTransferCancellation(PeerConfiguration.configurationWithDisconnectedPeers());
	}

	int dataLength = 1000000;
	boolean didCancel = false;
	
	public void testTransferCancellation(final PeerConfiguration peerConfiguration) {
		CountDown cancellationCountDown = new CountDown(2, () -> peerConfiguration.runloop.stop());

		peerConfiguration.startAndExecuteAfterDiscovery(() -> {
			peerConfiguration.peer2.setIncomingConnectionHandler((connectingPeer, connection) -> {
				connection.setOnTransfer((c, transfer) -> {
					transfer.setOnCompleteData((t, data) -> {}); // We don't care about the data, but need to set a data handler to keep the transfer from complaining
					transfer.setOnComplete(t -> fail("Transfer should not complete."));
					transfer.setOnCancel(t -> {
						assertTrue("transfer should have cancelled state", t.getIsCancelled());
						assertFalse("transfer should not be completed", t.getIsCompleted());
						cancellationCountDown.countDown();
					});
					transfer.setOnProgress(t -> {
						// When we get the first progress update, we cancel the transfer.
						if (!didCancel) {
							didCancel = true;
							peerConfiguration.runloop.execute(() -> {
								transfer.cancel();	
							});
						}
					});
				});
			});
			
			RemotePeer destination = peerConfiguration.peer1.getPeers().stream().filter(p -> p.getUniqueIdentifier().equals(peerConfiguration.peer2.getUniqueIdentifier())).findFirst().get();
			Connection connection = destination.connect();
			Transfer outTransfer = connection.send(TestData.generate(dataLength));
			outTransfer.setOnComplete(t -> fail("Transfer should not complete."));
			outTransfer.setOnCancel(t -> {
				assertTrue("transfer should have cancelled state", t.getIsCancelled());
				assertFalse("transfer should not be completed", t.getIsCompleted());
				cancellationCountDown.countDown();
			});
		});
	}
}
