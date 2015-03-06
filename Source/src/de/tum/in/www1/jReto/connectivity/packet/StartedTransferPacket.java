package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* Sent when a new transfer is started.
*/
public class StartedTransferPacket implements Packet {	
	public final static PacketType TYPE = PacketType.TRANSFER_STARTED;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE + Constants.INT_SIZE;
		
	public final UUID transferIdentifier;
	public final int transferLength;
	
	public StartedTransferPacket(UUID transferIdentifier, int transferLength) {
		this.transferIdentifier = transferIdentifier;
		this.transferLength = transferLength;
	}
	
	public static StartedTransferPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new StartedTransferPacket(reader.getUUID(), reader.getInt());
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		data.add(this.transferIdentifier);
		data.add(this.transferLength);
		return data.getData();
	}
}
