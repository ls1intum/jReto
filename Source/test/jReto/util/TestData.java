package jReto.util;

import java.nio.ByteBuffer;

public class TestData {
	public static ByteBuffer generate(int length) {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		for (int i=0; i<length; i++) {
			buffer.put((byte)(i%127));
		}
		buffer.rewind();
		return buffer;
	}
	
	public static void verify(ByteBuffer buffer, int length) {
		int count = buffer.remaining();
		if (count != length) throw new IllegalArgumentException("Test data needs to have the correct length");
		
		for (int i=0; i<count; i++) {
			byte value = buffer.get();
			
			if (value != i%127) throw new IllegalArgumentException("Buffer has incorrect value: "+value+", should be: "+i%127);
		}
	}
}
