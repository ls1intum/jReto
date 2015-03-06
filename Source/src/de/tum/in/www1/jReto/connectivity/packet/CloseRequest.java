package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* A CloseRequest is sent to the Connection establisher if the destination of the Connection attempts to close it.
* The establisher is expected to respond with a CloseAnnounce packet.
*/
public class CloseRequest implements Packet {
	public final static PacketType TYPE = PacketType.CLOSE_REQUEST;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE;
	
	public static CloseRequest deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new CloseRequest();
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		return data.getData();
	}
}
