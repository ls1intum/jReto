package de.tum.in.www1.jReto.niotools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ChannelReader implements Dispatcher.ReadHandler<SocketChannel> {
	public static interface ReadHandler {
		public void onRead(ByteBuffer byteBuffer);
	}
	public static interface CloseHandler {
		public void onClose();
	}
	
	public final SocketChannel socketChannel;
	public final Dispatcher dispatcher;
	public final ReadHandler handler;
	public final CloseHandler closeHandler;
	
	private final Queue<Integer> readRequests;
	private ByteBuffer currentBuffer;

	public ChannelReader(SocketChannel socketChannel, Dispatcher dispatcher, ReadHandler readHandler, CloseHandler closeHandler) {
		if (socketChannel == null) throw new IllegalArgumentException("socketChannel may not be null");
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (readHandler == null) throw new IllegalArgumentException("readHandler may not be null");
		if (closeHandler == null) throw new IllegalArgumentException("closeHandler may not be null");
		
		this.readRequests = new LinkedList<Integer>();
		this.socketChannel = socketChannel;
		this.dispatcher = dispatcher;
		this.handler = readHandler;
		this.closeHandler = closeHandler;
	}
	
	public void read(int length) {
		if (length <= 0) throw new IllegalArgumentException("length may not be <= 0");
		
		if (this.readRequests.size() == 0 && currentBuffer == null) this.dispatcher.registerReadHandler(this, this.socketChannel);
			
		this.readRequests.add(length);
		
		processReadRequests();
	}
	
	private void processReadRequests() {
		if (this.currentBuffer != null || readRequests.size() == 0) return;
		
		int length = readRequests.poll();
		this.currentBuffer = ByteBuffer.allocate(length);
	}
	
	private void readFromSocket() {		
		if (this.currentBuffer == null) return;
		if (!this.socketChannel.isConnected()) {
			System.out.println("Socket not connected, cannot read.");
			return;
		}
		int bytesRead = -1;
		
		try {
			bytesRead = this.socketChannel.read(this.currentBuffer);
		} catch (IOException e1) {
			System.err.println("Exception occurred while reading from the socket. Closing the socket now.");
			e1.printStackTrace();
		}
		
		if (bytesRead==-1) {
			try {
				this.dispatcher.unregister(this.socketChannel);
				this.socketChannel.close();
			} catch (IOException e) {
				System.err.println("Exception occurred while closing connection. Continuing anyway.");
				e.printStackTrace();
			} finally {
				this.closeHandler.onClose();
			}
		} else if (!this.currentBuffer.hasRemaining()) {
			this.currentBuffer.clear();
			this.handler.onRead(this.currentBuffer);
			this.currentBuffer = null;
			
			if (this.readRequests.size() == 0) this.dispatcher.unregisterRead(this.socketChannel);
			
			processReadRequests();
		}
	}
	
	public void onReadable(SocketChannel socket) {
		this.readFromSocket();
	}
}
