package de.tum.in.www1.jReto.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public enum PacketType {		
	UNKNOWN(0),
	
	// Routing Layer
	LINK_HANDHAKE(1),
	ROUTING_HANDSHAKE(2),
	LINK_STATE(3),
	FLOODED_PACKET(4),
	ROUTED_CONNECTION_ESTABLISHED_CONFIRMATION(5),
	
	// Connectivity
	MANAGED_CONNECTION_HANDSHAKE(10),
	CLOSE_REQUEST(11),
	CLOSE_ANNOUNCE(12),
	CLOSE_ACKNOWLEDGE(13),
	
	// Data transmission
	TRANSFER_STARTED(20),
	DATA_PACKET(21),
	CANCELLED_TRANSFER(22),
	PROGRESS_INFORMATION(23);
	
	private static final Map<Integer, PacketType> intToTypeMap = new HashMap<Integer, PacketType>();
	static {
	    for (PacketType type : PacketType.values()) {
	        intToTypeMap.put(type.value, type);
	    }
	}
	
	private final int value;
	
	PacketType(int value) {
		this.value = value;
	}
	public static PacketType fromRaw(int value) {
		PacketType result = intToTypeMap.get(value);
		if (result == null) result = PacketType.UNKNOWN;
		return result;
	}
	public static PacketType fromData(ByteBuffer data) {
		if (data.remaining() < 4) {
			System.err.println("Insufficient data to get packet type.");
			return PacketType.UNKNOWN;
		}
		
		int value = data.getInt();
		data.rewind();
		PacketType type = PacketType.fromRaw(value);
		
		if (type == PacketType.UNKNOWN) {
			System.err.println("Read unknown type, value was: "+value);
		}
		
		return type;
	}
	public int toRaw() {
		return this.value;
	}
}