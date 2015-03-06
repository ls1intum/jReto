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
* This packet is used when establishing multicast connectinos to ensure that all destinations are actually connected.
* It only contains the sender's identifier and is sent by all peers once the hop connection establishment phase is complete.
*/
public class RoutedConnectionEstablishedConfirmationPacket implements Packet {
	public final static PacketType TYPE = PacketType.ROUTED_CONNECTION_ESTABLISHED_CONFIRMATION;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE;

	public final UUID source;
	
	public RoutedConnectionEstablishedConfirmationPacket(UUID source) {
		this.source = source;
	}

	public static RoutedConnectionEstablishedConfirmationPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new RoutedConnectionEstablishedConfirmationPacket(reader.getUUID());
	}	
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		data.add(this.source);
		return data.getData();
	}
}
