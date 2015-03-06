package de.tum.in.www1.jReto.module.wlan;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.niotools.ChannelReader;
import de.tum.in.www1.jReto.niotools.ChannelWriter;
import de.tum.in.www1.jReto.niotools.Dispatcher;
import de.tum.in.www1.jReto.niotools.ChannelReader.CloseHandler;
import de.tum.in.www1.jReto.niotools.ChannelReader.ReadHandler;
import de.tum.in.www1.jReto.niotools.ChannelWriter.WriteHandler;

public class WlanConnection implements Connection, ReadHandler, CloseHandler, WriteHandler {
	public final int PACKET_LENGTH_FIELD_LENGTH = 4;
	
	private Handler handler;
	private Dispatcher dispatcher;
	private InetAddress address;
	private int port;
	private boolean isConnected;
	private SocketChannel socketChannel;
	private ChannelReader channelReader;
	private ChannelWriter channelWriter;
	
	private boolean readingPacketLength;
	private boolean writingPacketLength;
	
	public WlanConnection(Dispatcher dispatcher, InetAddress address, int port) {
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (address == null) throw new IllegalArgumentException("address may not be null");
		
		this.dispatcher = dispatcher;
		this.address = address;
		this.port = port;
		this.isConnected = false;
		
		this.readingPacketLength = true;
		this.writingPacketLength = true;
	}
	
	public WlanConnection(Dispatcher dispatcher, SocketChannel channel) {
		if (dispatcher == null) throw new IllegalArgumentException("dispatcher may not be null");
		if (channel == null) throw new IllegalArgumentException("channel may not be null");
		
		this.dispatcher = dispatcher;
		this.address = channel.socket().getInetAddress();
		this.port = channel.socket().getLocalPort();
		this.isConnected = true; 
		this.socketChannel = channel;
		
		this.readingPacketLength = true;
		this.writingPacketLength = true;
		
		try {
			channel.configureBlocking(false);
			
			this.channelReader = new ChannelReader(channel, this.dispatcher, this, this);
			this.channelWriter = new ChannelWriter(channel, this.dispatcher, this);
			
			this.channelReader.read(PACKET_LENGTH_FIELD_LENGTH);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	@Override
	public boolean isConnected() {
		return this.isConnected;
	}

	@Override
	public int getRecommendedPacketSize() {
		return 32*1024;
	}

	@Override
	public void connect() {
		try {
			socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress(this.address, this.port));
			socketChannel.configureBlocking(false);
			
			this.channelReader = new ChannelReader(socketChannel, this.dispatcher, this, this);
			this.channelWriter = new ChannelWriter(socketChannel, this.dispatcher, this);
			
			this.channelReader.read(PACKET_LENGTH_FIELD_LENGTH);
			
			this.isConnected = true;
			if (this.handler != null) this.handler.onConnect(this);
		} catch (IOException e) {}
	}

	@Override
	public void close() {
		try {
			this.isConnected = false;
			this.socketChannel.close();
			if (this.handler != null) this.handler.onClose(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void writeData(ByteBuffer data) {
		if (data.remaining() == 0) throw new IllegalArgumentException("data buffer needs to have more than 0 bytes remaining.");
		
		data.order(ByteOrder.LITTLE_ENDIAN);
		
		ByteBuffer lengthBuffer = ByteBuffer.allocate(PACKET_LENGTH_FIELD_LENGTH);
		lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lengthBuffer.putInt(data.remaining());
		lengthBuffer.clear();
		
		this.channelWriter.write(lengthBuffer);
		this.channelWriter.write(data);
	}

	@Override
	public void onClose() {
		this.isConnected = false;
		if (this.handler != null) this.handler.onClose(this);
	}

	@Override
	public void onRead(ByteBuffer byteBuffer) {
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		if (this.readingPacketLength) {
			this.readingPacketLength = false;
			
			int packetLength = byteBuffer.getInt();
			this.channelReader.read(packetLength);
		} else {
			this.readingPacketLength = true;
			
			this.handler.onDataReceived(this, byteBuffer);
			
			this.channelReader.read(PACKET_LENGTH_FIELD_LENGTH);
		}
	}

	@Override
	public void onCompletedWriteRequest() {
		if (this.writingPacketLength) {
			this.writingPacketLength = false;
		} else {
			this.handler.onDataSent(this);
			this.writingPacketLength = true;
		}
	}
}
