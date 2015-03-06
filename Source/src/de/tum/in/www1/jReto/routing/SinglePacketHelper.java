package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;

import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.packet.Packet;

public class SinglePacketHelper {
	public static interface OnPacketHandler {
		void onPacket(ByteBuffer data);
	}
	public static interface OnSuccessHandler {
		void onSuccess();
	}
	public static interface OnFailHandler {
		void onFail();
	}
	
	public static void read(Connection connection, final OnPacketHandler packetHandler, final OnFailHandler onFail) {
		read(connection, 1, packetHandler, new OnSuccessHandler() {
			@Override
			public void onSuccess() {}
		}, onFail);
	}
	public static void read(Connection connection, final int packetCount, final OnPacketHandler packetHandler, final OnSuccessHandler onSuccess, final OnFailHandler onFail) {
		connection.setHandler(new Connection.Handler() {
			private int packetsReceived = 0;
			
			@Override
			public void onDataSent(Connection connection) {}
			
			@Override
			public void onDataReceived(Connection connection, ByteBuffer data) {
				this.packetsReceived++;
				
				if (packetsReceived == packetCount) {
					connection.setHandler(null);
				}
				
				packetHandler.onPacket(data);
				
				if (packetsReceived == packetCount) {
					onSuccess.onSuccess();
				}
			}
			
			@Override
			public void onConnect(Connection connection) {}
			
			@Override
			public void onClose(Connection connection) {
				connection.setHandler(null);
				onFail.onFail();
			}
		});
	}
	public static void write(Connection connection, final Packet packet, final OnSuccessHandler onSuccess, final OnFailHandler onFail) {
		connection.setHandler(new Connection.Handler() {
			@Override
			public void onConnect(Connection connection) {
				connection.writeData(packet.serialize());
			}

			@Override
			public void onClose(Connection connection) {
				connection.setHandler(null);
				onFail.onFail();
			}

			@Override
			public void onDataReceived(Connection connection, ByteBuffer data) {}

			@Override
			public void onDataSent(Connection connection) {
				connection.setHandler(null);
				onSuccess.onSuccess();
			}
		});
		
		if (connection.isConnected()) connection.writeData(packet.serialize());
	}
}
