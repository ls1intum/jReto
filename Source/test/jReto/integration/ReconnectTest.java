package jReto.integration;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jReto.module.dummy.DummyAddress;
import jReto.module.dummy.DummyConnection;
import jReto.module.dummy.DummyModule;
import jReto.module.dummy.DummyNetworkInterface;
import jReto.util.RunLoop;
import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.Connection;
import de.tum.in.www1.jReto.LocalPeer;
import de.tum.in.www1.jReto.connectivity.Transfer;

/**
 * When a connection fails, a reconnect should be attempted and the transfer should complete successfully.
 * */
public class ReconnectTest {
	RunLoop runloop = new RunLoop(false);
	DummyNetworkInterface manager = new DummyNetworkInterface("test", runloop, 1024, 1);
	int dataLength = 100000;
	Set<DummyConnection> connections = new HashSet<>();
	boolean sabotaged = false;
	boolean recordConnections = false;
	int count = 0;
	
	@Test(timeout=10000000)
	public void testReconnect() {
		manager.connectionCreatedHook = new DummyNetworkInterface.DummyConnectionCreatedHook() {
			@Override
			public void createdConnection(DummyAddress address, DummyConnection connection) {
				if (recordConnections) connections.add(connection);
			}
		};
		
		LocalPeer localPeer1 = new LocalPeer(Arrays.asList(new DummyModule(manager, runloop)), runloop);
		LocalPeer localPeer2 = new LocalPeer(Arrays.asList(new DummyModule(manager, runloop)), runloop);
		localPeer1.start(discoveredPeer -> {
			recordConnections = true;
			Connection connection = discoveredPeer.connect();
			recordConnections = false;
			Transfer transfer = connection.send(TestData.generate(dataLength));
			transfer.setOnProgress(t -> {
				System.out.println("Finnished: "+t.getProgress() +" of "+t.getLength());
				if (!sabotaged) {
					sabotaged = true;
					assertTrue(connections.size() > 0);
					System.out.println("Sent some data, sabotaging: "+connections.size());
					for (DummyConnection dummyConnection : connections) dummyConnection.sabotage();
				}
			});
		}, p -> {});
		
		localPeer2.start(p -> {}, p -> {}, (peer, connection) -> {
			connection.setOnTransfer((c, transfer) -> {
				transfer.setOnCompleteData((t, data) -> {
					TestData.verify(data, dataLength);
					System.out.println("Successfully finished!");
					runloop.stop();
				});
			});
		});
	
		runloop.start();
	}
}
