package de.tum.in.www1.jReto.routing.packets;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/** This enum represents the possible purposes of a direct connection - either to transmit routing information, or to be part of a user-requested routed connection. */
public enum ConnectionPurpose {
	UNKNOWN(0),
    /** Used for connections that are used to transmit routing metadata. */
	ROUTING_DATA_EXCHANGE_CONNECTION(1),
    /** Used for user-requested connections that are routed. */
	ROUTED_CONNECTION(2);
	
	private static final Map<Integer, ConnectionPurpose> intToTypeMap = new HashMap<Integer, ConnectionPurpose>();
	static {
	    for (ConnectionPurpose type : ConnectionPurpose.values()) {
	        intToTypeMap.put(type.value, type);
	    }
	}
	
	private final int value;
	
	ConnectionPurpose(int value) {
		this.value = value;
	}
	public static ConnectionPurpose fromRaw(int value) {
		ConnectionPurpose result = intToTypeMap.get(value);
		if (result == null) result = ConnectionPurpose.UNKNOWN;
		return result;
	}
	public static ConnectionPurpose fromData(ByteBuffer data) {
		if (data.remaining() < 4) return ConnectionPurpose.UNKNOWN;
		
		int value = data.getInt();
		data.rewind();
		return ConnectionPurpose.fromRaw(value);
	}
	public int toRaw() {
		return this.value;
	}
}
