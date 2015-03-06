package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;

public class DefaultDataConsumer {
	private final ByteBuffer data;
	private final int length;
	
	public DefaultDataConsumer(int length) {
		this.length = length;
		this.data = ByteBuffer.allocate(length);
	}
	
	public int getDataLength() {
		return this.length;
	}

	public void consume(ByteBuffer data) {
		if (this.data.remaining() < data.remaining()) throw new IllegalArgumentException("data contains "+data.remaining()+" additional bytes, can consume "+this.data.remaining()+" bytes maximum.");
		this.data.put(data);
	}
	
	public ByteBuffer getData() {
		this.data.rewind();
		return this.data;
	}
}
