package de.tum.in.www1.jReto.niotools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ChannelWriter implements Dispatcher.WriteHandler<SocketChannel> {
	public static interface WriteHandler {
		void onCompletedWriteRequest();
	}
	
	public final SocketChannel socketChannel;
	public final Queue<ByteBuffer> writeRequests;
	public final Dispatcher dispatcher;
	public final WriteHandler writeHandler;
	
	private ByteBuffer currentBuffer;
	
	public ChannelWriter(SocketChannel socketChannel, Dispatcher dispatcher, WriteHandler writeHandler) {
		if (socketChannel == null) throw new IllegalArgumentException("socketChannel may not be null");
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		
		this.socketChannel = socketChannel;
		this.dispatcher = dispatcher;
		this.writeHandler = writeHandler;
		
		writeRequests = new LinkedList<ByteBuffer>();
	}
	
	public void write(ByteBuffer buffer) {		
		if (buffer == null) throw new IllegalArgumentException("buffer may not be null");
		if (!buffer.hasRemaining()) throw new IllegalArgumentException("Attempted to write a buffer with no remaining bytes. Did you forget to call clear()?");

		if (this.writeRequests.size() == 0 && this.currentBuffer == null) {
			this.dispatcher.registerWriteHandler(this, this.socketChannel);
		}

		writeRequests.add(buffer);
		processWriteRequests();
	}
	
	private void processWriteRequests() {
		if (currentBuffer != null || writeRequests.size() == 0) return;

		currentBuffer = writeRequests.poll();
	}
	
	private void processCurrentBuffer() {
		if (currentBuffer == null) return;

		try {
			socketChannel.write(currentBuffer);
		} catch (IOException e) {
			System.err.println("An error occured while trying to write to the socket.");
			e.printStackTrace();
		}
		
		if (!currentBuffer.hasRemaining()) {
			currentBuffer = null;
			
			this.writeHandler.onCompletedWriteRequest();
			
			if (this.writeRequests.size() == 0) this.dispatcher.unregisterWrite(this.socketChannel);
		}
		
		processWriteRequests();
	}
	
	public void onWriteable(SocketChannel socket) {	
		processCurrentBuffer();
	}
}
