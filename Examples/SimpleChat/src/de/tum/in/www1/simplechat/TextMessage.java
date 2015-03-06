package de.tum.in.www1.simplechat;

import java.nio.ByteBuffer;

public class TextMessage {
	public final String text;
	
	public TextMessage(String text) {
		this.text = text;
	}
	public TextMessage(ByteBuffer data) {
		this.text = new String(data.array());
	}
	public ByteBuffer serialize() {
		return ByteBuffer.wrap(this.text.getBytes());
	}
}