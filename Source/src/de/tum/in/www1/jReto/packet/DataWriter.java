package de.tum.in.www1.jReto.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/** Write primitive types to a ByteBuffer */
public class DataWriter {
	private final ByteBuffer data;
	
    /** Constructs a data writer with a given length. */
	public DataWriter(int length) {
		this.data = ByteBuffer.allocate(length);
		this.data.order(ByteOrder.LITTLE_ENDIAN);
	}
	/** Appends a PacketType */
	public void add(PacketType type) {
		this.add(type.toRaw());
	}
    /** Appends a 4 byte integer */
	public void add(int integer) {
		this.data.putInt(integer);
	}
    /** Appends an UUID */
	public void add(UUID uuid) {
		this.data.putLong(uuid.getMostSignificantBits());
		this.data.putLong(uuid.getLeastSignificantBits());
	}
    /** Appends a ByteBuffer object. */
	public void add(ByteBuffer data) {
		this.data.put(data);
	}
	
    /** Returns all data that was written. */
	public ByteBuffer getData() {
		this.data.rewind();
		return this.data;
	}
}