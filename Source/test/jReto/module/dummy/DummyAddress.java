package jReto.module.dummy;

import java.nio.ByteBuffer;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Connection;
import jReto.util.RunLoop;

public class DummyAddress implements Address {
	public static interface DummySocket {
		void write(ByteBuffer data);
		void close();
		void sabotage();
	}
	
	public class ForwardingConnection {
		private DummyConnection inConnection;
		private DummyConnection outConnection;
		private DummyAdvertiser advertiser;
		
		public ForwardingConnection(final DummyNetworkInterface networkInterface, DummyAdvertiser advertiser, RunLoop runloop) {
			this.inConnection = new DummyConnection(networkInterface, advertiser, this, runloop);
			this.outConnection = new DummyConnection(networkInterface, advertiser, this, runloop);
			this.advertiser = advertiser;
			
			this.inConnection.setSocket(new DummySocket() {
				public void write(ByteBuffer buffer) {
					outConnection.receive(buffer);
				}
				
				public void close() {
					outConnection.internalClose();

					if (networkInterface.connectionClosedHook != null) {
						networkInterface.connectionClosedHook.closedConnections(DummyAddress.this, outConnection, inConnection);
					}
				}
				
				public void sabotage() {
					inConnection.onSabotage();
					outConnection.onSabotage();
				}
			});
			
			this.outConnection.setSocket(new DummySocket() {
				public void write(ByteBuffer buffer) {
					inConnection.receive(buffer);
				}
				
				public void close() {
					inConnection.internalClose();
					
					if (networkInterface.connectionClosedHook != null) {
						networkInterface.connectionClosedHook.closedConnections(DummyAddress.this, outConnection, inConnection);
					}
				}
				
				public void sabotage() {
					inConnection.onSabotage();
					outConnection.onSabotage();
				}
			});
		}
		
		public void advertiseConnection() {
			getInConnection().setConnected(true);
			this.advertiser.onConnection(getInConnection());
		}
		
		public DummyConnection getOutConnection() {
			return this.outConnection;
		}
		
		public DummyConnection getInConnection() {
			return this.inConnection;
		}
	}
	
	private DummyNetworkInterface networkInterface;
	private DummyAdvertiser advertiser;
	private RunLoop runloop;
	
	public DummyAddress(DummyNetworkInterface networkInterface, DummyAdvertiser advertiser, RunLoop runloop) {
		this.networkInterface = networkInterface;
		this.advertiser = advertiser;
		this.runloop = runloop;
	}

	@Override
	public Connection createConnection() {
		final ForwardingConnection connection = new ForwardingConnection(this.networkInterface, this.advertiser, this.runloop);

		if (this.networkInterface.connectionCreatedHook != null) {
			this.networkInterface.connectionCreatedHook.createdConnection(this, connection.getOutConnection());
		}
		return connection.getOutConnection();
	}

	@Override
	public int getCost() {
		return this.networkInterface.getPriority();
	}
}
