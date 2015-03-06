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
* The LinkHandshake packet is the first packet exchanged over a direct connection; it is sent by the establishing peer. It contains that peer's identifier and 
* the purpose of the connection. It is used by the establishDirectConnection and handleDirectConnection methods in the Router class.
*/
public class LinkHandshake implements Packet {
	public final static PacketType TYPE = PacketType.LINK_HANDHAKE;
	public final static int LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE + Constants.INT_SIZE;
	
	public final UUID peerIdentifier;
	public final ConnectionPurpose connectionPurpose;
	
	public LinkHandshake(UUID peerIdentifier, ConnectionPurpose connectionPurpose) {
		this.peerIdentifier = peerIdentifier;
		this.connectionPurpose = connectionPurpose;
	}
	
	public static LinkHandshake deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, LENGTH)) return null;
		
		UUID identifier = reader.getUUID();
		ConnectionPurpose purpose = ConnectionPurpose.fromRaw(reader.getInt());
		
		if (purpose == ConnectionPurpose.UNKNOWN) return null;
		
		return new LinkHandshake(identifier, purpose);
	}
	
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(LENGTH);
		data.add(TYPE);
		data.add(this.peerIdentifier);
		data.add(this.connectionPurpose.toRaw());
		return data.getData();
	}
}
