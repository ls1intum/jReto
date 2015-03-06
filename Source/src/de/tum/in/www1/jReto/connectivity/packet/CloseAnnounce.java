package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* Announces that a connection will close. Sent by the Connection establisher.
*/
public class CloseAnnounce implements Packet {
	public final static PacketType TYPE = PacketType.CLOSE_ANNOUNCE;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE;
	
	public static CloseAnnounce deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new CloseAnnounce();
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		return data.getData();
	}
}