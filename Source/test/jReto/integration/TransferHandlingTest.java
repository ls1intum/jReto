package jReto.integration;

import jReto.meta.PeerConfiguration;
import jReto.util.Condition;
import jReto.util.OrderVerifier;
import jReto.util.RunLoop;
import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.RemotePeer;
import de.tum.in.www1.jReto.connectivity.Transfer;
import de.tum.in.www1.jReto.util.CountDown;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;


/**
 * Tests whether OutTransfer.Handler and InTransfer.Handler methods are called, and whether they are called in the correct order.
 * */
public class TransferHandlingTest {
	@Test(timeout=1000)
	public void testTransferHandlingDirect() {
		new TransferHandlingTest().testTransferHandling(PeerConfiguration.directNeighborConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferHandling2Hop() {
		new TransferHandlingTest().testTransferHandling(PeerConfiguration.twoHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferHandling4Hop() {
		new TransferHandlingTest().testTransferHandling(PeerConfiguration.fourHopRoutedConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferHandlingNontrivial() {
		new TransferHandlingTest().testTransferHandling(PeerConfiguration.nontrivial2HopNetworkConfiguration());
	}
	@Test(timeout=1000)
	public void testTransferHandlingDisconnectedPeers() {
		new TransferHandlingTest().testTransferHandling(PeerConfiguration.configurationWithDisconnectedPeers());
	}
	
	Condition inStartWasCalled = new Condition("onStart was called in inTransfer", true, true);
	Condition outStartWasCalled = new Condition("onStart was called in outTransfer", true, true);
	Condition inCompleteWasCalled = new Condition("onComplete was called in inTransfer", true, true);
	Condition outCompleteWasCalled = new Condition("onComplete was called in outTransfer", true, true);
	Condition inProgressUpdateGiven = new Condition("onBytesFinneshed called in inTransfer", false, false);
	Condition outProgressUpdateGiven = new Condition("onBytesFinneshed called in outTransfer", false, false);
	OrderVerifier orderVerifier = new OrderVerifier();
	RunLoop runloop;
	
	CountDown bothComplete = new CountDown(2, new Runnable() {
		@Override
		public void run() {
			/* 5 */ orderVerifier.check(5);

			runloop.stop();
			
			Condition.verifyAll(inStartWasCalled, outStartWasCalled, inCompleteWasCalled, outCompleteWasCalled, inProgressUpdateGiven, outProgressUpdateGiven);
		}
	});
	
	public void testTransferHandling(final PeerConfiguration configuration) {
		this.runloop = configuration.runloop;
		
		/* 1 */ orderVerifier.check(1);
		
		configuration.startAndExecuteAfterDiscovery(() -> {
			configuration.peer2.setIncomingConnectionHandler((peer, connection) -> {
				/* 3 */ orderVerifier.check(3);
				
				connection.setOnTransfer((c, transfer) -> {
					transfer.setOnStart(t -> {
						/* 3 */ orderVerifier.check(3);
						inStartWasCalled.confirm();
					});
					transfer.setOnCompleteData((t, data) -> {
						/* 4 */ orderVerifier.check(4);
						inCompleteWasCalled.confirm();
						bothComplete.countDown();
					});
					transfer.setOnCancel(t -> {
						fail("No cancel should happen here.");
					});
					transfer.setOnProgress(t -> {
						inProgressUpdateGiven.confirm();
						
						assertFalse("onBytesFinnished calles may not occurr after a transfer was finnished (error with in transfer).", inCompleteWasCalled.isConfirmed());
					});
				});
			});
			
			RemotePeer destination = configuration.peer1.getPeers().stream().filter(p -> p.getUniqueIdentifier().equals(configuration.peer2.getUniqueIdentifier())).findFirst().get();
			Connection connection = destination.connect();
			Transfer transfer = connection.send(TestData.generate(10000));
			
			transfer.setOnStart(t -> {
				/* 3 */ orderVerifier.check(3);
				outStartWasCalled.confirm();
			});
			transfer.setOnComplete(t -> {
				/* 4 */ orderVerifier.check(3);
				outCompleteWasCalled.confirm();
				bothComplete.countDown();
			});
			transfer.setOnCancel(t -> {
				fail("No cancel should happen here.");
			});
			transfer.setOnProgress(t -> {
				outProgressUpdateGiven.confirm();
				
				assertFalse("onBytesFinnished calles may not occurr after a transfer was finnished (error with out transfer).", outCompleteWasCalled.isConfirmed());
			});
		});
	}
}
