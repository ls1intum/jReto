package de.tum.in.www1.jReto.routing.packets;

import java.nio.ByteBuffer;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* A FloodingPacket is a packet that floods any other packet through the network.
* The pair of sequenceNumber and originIdentifier are required to ensure that packets are not flooded indefinitely. See the FloodingPacketManager for more information.
*/
public class FloodingPacket implements Packet {
	public final static PacketType TYPE = PacketType.FLOODED_PACKET;
	public final static int MINIMUM_LENGTH = Constants.PACKET_TYPE_SIZE + Constants.INT_SIZE + Constants.UUID_SIZE;
	
	public final UUID originIdentifier;
	public final int sequenceNumber;
	public final ByteBuffer payload;
	
	public FloodingPacket(UUID originIdentifier, int sequenceNumber, ByteBuffer payload) {
		if (payload.remaining() == 0) {
			System.err.println("Warning: Created FloodingPacket with 0 length payload.");
		}
		
		this.originIdentifier = originIdentifier;
		this.sequenceNumber = sequenceNumber;
		this.payload = payload;
	}
	
	public static FloodingPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, MINIMUM_LENGTH)) return null;
		
		return new FloodingPacket(reader.getUUID(), reader.getInt(),  reader.getRemainingData());
	}
	public ByteBuffer serialize() {
		this.payload.rewind(); // Maybe someone from outside accessed the buffer and didn't rewind it
		DataWriter data = new DataWriter(MINIMUM_LENGTH + this.payload.remaining());
		data.add(TYPE);
		data.add(this.originIdentifier);
		data.add(this.sequenceNumber);
		data.add(this.payload);
		this.payload.rewind(); // Make sure someone from outside can use the buffer without rewinding first
		return data.getData();
	}
}
