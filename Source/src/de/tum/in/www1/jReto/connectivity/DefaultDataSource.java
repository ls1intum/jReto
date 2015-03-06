package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;

public class DefaultDataSource {
	private final int length;
	private final ByteBuffer data;
	
	public DefaultDataSource(ByteBuffer data) {
		this.length = data.remaining();
		this.data = data;
	}
	
	public int getDataLength() {
		return this.length;
	}

	public ByteBuffer getData(int offset, int length) {		
		if (offset+length > this.length) {
			throw new IllegalArgumentException("Trying to use offset "+offset+" and length "+length+", total buffer length is "+this.length);
		}
		// Note: this implementation should be reasonably fast. No data is copied; this creates just a new "view" on the buffer
		ByteBuffer result = this.data.duplicate();
		
		result.position(offset);
		result = result.slice();
		result.limit(length);
	
		result.rewind();

		return result;
	}
}
