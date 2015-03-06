package jReto.integration;

import java.util.Arrays;

import jReto.module.dummy.DummyAddress;
import jReto.module.dummy.DummyConnection;
import jReto.module.dummy.DummyModule;
import jReto.module.dummy.DummyNetworkInterface;
import jReto.util.Condition;
import jReto.util.RunLoop;
import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.LocalPeer;

public class AutoConnectionUpgradeTest {
	Condition createdFastConnection = new Condition("created fast connection");
	Condition closedSlowConnection = new Condition("closed slow connection");
	RunLoop runloop = new RunLoop(false);
	DummyNetworkInterface slowInterface = new DummyNetworkInterface("testSlow", runloop, 1024, 2);
	DummyNetworkInterface fastInterface = new DummyNetworkInterface("testFast", runloop, 1024, 1);
	
	LocalPeer localPeer1;
	LocalPeer localPeer2;

	int dataLength = 1000000;
	boolean addedFastInterface = false;
	
	
	@Test(timeout=1000000)
	public void testAutoConnectionUpgrade() {
		fastInterface.connectionCreatedHook = new DummyNetworkInterface.DummyConnectionCreatedHook() {
			@Override
			public void createdConnection(DummyAddress address, DummyConnection connection) {
				createdFastConnection.confirm();
			}
		};
		
		slowInterface.connectionClosedHook = new DummyNetworkInterface.DummyConnectionClosedHook() {
			@Override
			public void closedConnections(DummyAddress address, DummyConnection outConnection, DummyConnection inConnection) {
				closedSlowConnection.confirm();
			}
		};
		
		localPeer1 = new LocalPeer(Arrays.asList(new DummyModule(slowInterface, runloop)), runloop);
		localPeer2 = new LocalPeer(Arrays.asList(new DummyModule(slowInterface, runloop)), runloop);
		
		localPeer1.start(discoveredPeer -> {
			Connection connection = discoveredPeer.connect();
			connection.send(TestData.generate(dataLength));
		}, p -> {});
		localPeer2.start(p -> {}, p -> {}, (p, connection) -> {
			connection.setOnTransfer((c, transfer) -> {
				transfer.setOnCompleteData((t, d) -> {
					TestData.verify(d, dataLength);
					
					Condition.verifyAll(createdFastConnection, closedSlowConnection);
					
					runloop.stop();
				});
				transfer.setOnProgress(t -> {
					if (!addedFastInterface) {
						addedFastInterface = true;
						
						localPeer1.addModule(new DummyModule(fastInterface, runloop));
						localPeer2.addModule(new DummyModule(fastInterface, runloop));
					}
				});
			});
		});
	}
}
