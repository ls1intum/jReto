package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* A DataPacket sends the payload data of a transfer.
*/
public class DataPacket implements Packet {
	public final static PacketType TYPE = PacketType.DATA_PACKET;
	public final static int MINIMUM_LENGTH = Constants.PACKET_TYPE_SIZE;
	
	public final ByteBuffer data;

	public DataPacket(ByteBuffer data) {
		this.data = data;
	}
	
	public static DataPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, MINIMUM_LENGTH)) return null;
		
		return new DataPacket(reader.getRemainingData());
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(MINIMUM_LENGTH + this.data.remaining());
		data.add(TYPE);
		data.add(this.data);
		return data.getData();
	}
}
