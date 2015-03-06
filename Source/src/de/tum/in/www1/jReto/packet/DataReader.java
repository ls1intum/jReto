package de.tum.in.www1.jReto.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
* Read primitive types from a ByteBuffer.
*/
public class DataReader {
	private final ByteBuffer data;
	
    /** Constructs a DataReader from an ByteBuffer object. */
	public DataReader(ByteBuffer data) {
		data.order(ByteOrder.LITTLE_ENDIAN);
		this.data = data;
	}
    /**
    * Checks whether more than length bytes can still be read.
    * @param length The number of bytes to check
    * @return true if more than or equal to length bytes can still be read.
    */
	public boolean checkRemaining(int minimumRemainingBytes) {
		return this.data.remaining() >= minimumRemainingBytes;
	}
    /**
    * The number of remaining bytes to be read.
    */
	public int getRemainingBytes() {
		return this.data.remaining();
	}
    /**
    * Resets the position to zero.
    */
	public void rewind() {
		this.data.rewind();
	}
	/**
	 * Reads a PacketType.
	 * */
	public PacketType getPacketType() {
		return PacketType.fromRaw(this.getInt());
	}
    /** 
    * Returns the next 4 byte integer.
    */
	public int getInt() {
		return this.data.getInt();
	}
    /**
    * Reads an UUID.
    */
	public UUID getUUID() {
		return new UUID(this.data.getLong(), this.data.getLong());
	}
    /**
    * Returns all remaining data.
    */
	public ByteBuffer getRemainingData() {
		ByteBuffer data = this.data.slice();
		data.order(ByteOrder.LITTLE_ENDIAN);
		return data;
	}
}
