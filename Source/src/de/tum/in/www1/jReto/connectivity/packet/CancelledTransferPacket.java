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
* Sent when a transfer was cancelled by the sender of a data transfer, or sent when the cancellation of a transfer is requested by the receiver of the data transfer.
*/
public class CancelledTransferPacket implements Packet {
	public final static PacketType TYPE = PacketType.CANCELLED_TRANSFER;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE;
	
	public final UUID transferIdentifier;
	
	public CancelledTransferPacket(UUID transferIdentifier) {
		this.transferIdentifier = transferIdentifier;
	}
	
	public static CancelledTransferPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new CancelledTransferPacket(reader.getUUID());
	}
	
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		data.add(this.transferIdentifier);
		return data.getData();
	}
}
