package jReto.module.dummy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.packet.PacketType;
import jReto.module.dummy.DummyAddress.DummySocket;
import jReto.module.dummy.DummyAddress.ForwardingConnection;
import jReto.util.RunLoop;

public class DummyConnection implements Connection {
	private DummyNetworkInterface networkInterface;
	private DummySocket writer;
	//private DummyAdvertiser advertiser;
	private ForwardingConnection forwardingConnection;
	private RunLoop runloop;
	private Handler handler;
	private boolean isConnected;
	
	public DummyConnection(DummyNetworkInterface networkInterface, DummyAdvertiser advertiser, ForwardingConnection forwardingConnection, RunLoop runloop) {
		this.networkInterface = networkInterface;
		//this.advertiser = advertiser;
		this.forwardingConnection = forwardingConnection;
		this.runloop = runloop;
		this.isConnected = false;
	}
	
	public void setSocket(DummySocket writer) {
		this.writer = writer;
	}
	
	public void receive(final ByteBuffer data) {
		runloop.execute(new Runnable() {

			@Override
			public void run() {
				if (DummyConnection.this.handler == null) {
					System.err.println("Connection has no handler: "+DummyConnection.this);
					System.err.println("Packet type was: "+data.getInt());
				}
				else { DummyConnection.this.handler.onDataReceived(DummyConnection.this, data); }
			}
		});
	}
	
	public void internalClose() {
		this.isConnected = false;
		this.handler.onClose(this);
	}
	
	@Override
	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	@Override
	public boolean isConnected() {
		return this.isConnected;
	}

	@Override
	public int getRecommendedPacketSize() {
		return this.networkInterface.getRecommendedPacketSize();
	}
	
	public void setConnected(boolean connected) {
		this.isConnected = connected;
	}

	@Override
	public void connect() {
		if (this.isConnected) throw new IllegalStateException("Tried to connect twice.");
		
		this.isConnected = true;
		this.forwardingConnection.advertiseConnection();
		
		if (this.handler != null) this.handler.onConnect(this);
	}

	@Override
	public void close() {		
		if (!this.isConnected) {
			System.out.println("Attempted to close closed connection.");
			return;
		}
		
		this.isConnected = false;
		this.writer.close();
		if (this.handler != null) this.handler.onClose(this);
	}

	public void sabotage() {
		runloop.execute(new Runnable() {

			@Override
			public void run() {
				DummyConnection.this.writer.sabotage();
			}
		});
	}
	
	public void onSabotage() {
		this.isConnected = false;
		this.writer = null;
		this.handler.onClose(this);
	}
	
	public String toString() {
		return "DummyConnection@"+ Integer.toHexString(this.hashCode())+"/"+this.networkInterface.getInterfaceName();
	}
	
	@Override
	public void writeData(final ByteBuffer data) {
		
		if (!this.isConnected) throw new Error("no sending data before connecting!");
		if (data.remaining() == 0) {
			System.err.println("Attempting to send 0 length data.");
		}
		if (data.position() != 0) {
			System.out.println("Attempting to write data that is not reset.");
		}
		
		if (data.order() == ByteOrder.BIG_ENDIAN) {
			System.err.println("Attempting to write big endian.");
		}
		data.rewind();
		int type = data.getInt();
		if (PacketType.fromRaw(type) == PacketType.UNKNOWN) {
			System.err.println("Attempting to write packet with unknown type: "+type);
		}
		data.rewind();
		this.writer.write(data);
		
		runloop.execute(new Runnable() {

			@Override
			public void run() {
				if (!DummyConnection.this.isConnected) return;
				DummyConnection.this.handler.onDataSent(DummyConnection.this);				
			}
		});
	}
}
