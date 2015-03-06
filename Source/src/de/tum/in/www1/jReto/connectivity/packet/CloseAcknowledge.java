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
* Acknowledges that a Connection is about to close. Sent by all destinations of a Connection.
* Once the establisher has received all acknowledgements (if the Connection is not a multicast connection, it is only one acknowledgement),
* the underlying connection is closed.
*/
public class CloseAcknowledge implements Packet {
	public final static PacketType TYPE = PacketType.CLOSE_ACKNOWLEDGE;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE;
	
	public final UUID source;
	
	public CloseAcknowledge(UUID source) {
		this.source = source;
	}
	
	public static CloseAcknowledge deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		return new CloseAcknowledge(reader.getUUID());
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		data.add(this.source);
		return data.getData();
	}
}
