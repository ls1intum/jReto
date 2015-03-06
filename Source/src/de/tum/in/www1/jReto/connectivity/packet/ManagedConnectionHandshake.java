package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.*;

/**
* A ManagedConnectionHandshake is sent once a connection was established with another peer. 
* It contains the connections unique identifier, which is used to decide whether the new underlying connection should be used 
* with an existing connection (e.g. in the case of a reconnect), or if a new Connection should be created.
*/
public class ManagedConnectionHandshake implements Packet {
	public final static PacketType TYPE = PacketType.MANAGED_CONNECTION_HANDSHAKE;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE;
	
	public final UUID connectionIdentifier;
	
	public ManagedConnectionHandshake(UUID connectionIdentifier) {
		this.connectionIdentifier = connectionIdentifier;
	}
	
	public static ManagedConnectionHandshake deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new ManagedConnectionHandshake(reader.getUUID());
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(ManagedConnectionHandshake.LENGTH);
		data.add(TYPE);
		data.add(this.connectionIdentifier);
		return data.getData();
	}
}
