package de.tum.in.www1.jReto.niotools;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Method overview:
 * 
 * dispatch - runs a runnable in the run loop of the dispatcher (ie. in the dispatcher's thread). Can be called from any thread.
 * 
 * These methods need to be called from the dispatcher's thread.
 * 
 * start - starts the run loop, does not return until stop is called (either by one of the registered callbacks or by a dispatched runnable)
 * stop - stops the run loop
 * register* - register a callback for a certain event of a SocketChannel.
 * unregister* - unregisters a callback for a certain event of a SocketChannel.
 * */
public class Dispatcher {
	public static interface AcceptHandler<T> {
		public void onAcceptable(T socket);
	}
	public static interface ConnectHandler<T> {
		public void onConnectable(T socket);
	}
	public static interface WriteHandler<T> {
		public void onWriteable(T socket);
	}
	public static interface ReadHandler<T> {
		public void onReadable(T socket);
	}
	public static interface Handler<T> extends AcceptHandler<T>, ConnectHandler<T>, WriteHandler<T>, ReadHandler<T> {}
	
	private final static int[] ALL_OPERATIONS = {SelectionKey.OP_ACCEPT, SelectionKey.OP_CONNECT, SelectionKey.OP_READ, SelectionKey.OP_WRITE};

	private final Selector selector;
	private final ConcurrentHashMap<Integer, HashMap<SelectableChannel, HandlerDispatcher<?>>> handlersByChannelByOperation;
	private final Executor executor;
	private final LinkedBlockingQueue<Runnable> runnables;

	private boolean isCurrentlyRunning = false;
	
	public Dispatcher(Executor executor) throws IOException {		
		this.selector = Selector.open();
		this.executor = executor;
		this.runnables = new LinkedBlockingQueue<Runnable>();

		this.handlersByChannelByOperation = new ConcurrentHashMap<Integer, HashMap<SelectableChannel,HandlerDispatcher<?>>>();
		for (int operation : ALL_OPERATIONS) this.handlersByChannelByOperation.put(operation, new HashMap<SelectableChannel, HandlerDispatcher<?>>());
	}
	
	private void dispatch(Runnable runnable) {
		this.runnables.add(runnable);
		this.selector.wakeup();
	}
	
	/**
	 * Call these methods only from the Dispatcher's thread. 
	 * If you want to perform one of these from another thread, use dispatch to do it.
	 * */
	public <T extends SelectableChannel> void registerHandler(Handler<T> handler, T channel) {
		this.registerAcceptHandler(handler, channel);
		this.registerConnectHandler(handler, channel);
		this.registerReadHandler(handler, channel);
		this.registerWriteHandler(handler, channel);
	}
	public <T extends SelectableChannel> void registerAcceptHandler(AcceptHandler<T> handler, T channel) {		
		register(channel, SelectionKey.OP_ACCEPT, new HandlerAcceptDispatcher<T>(handler, channel));
	}
	public <T extends SelectableChannel> void registerConnectHandler(ConnectHandler<T> handler, T channel) {
		register(channel, SelectionKey.OP_CONNECT, new HandlerConnectDispatcher<T>(handler, channel));
	}
	public <T extends SelectableChannel> void registerReadHandler(ReadHandler<T> handler, T channel) {		
		register(channel, SelectionKey.OP_READ, new HandlerReadDispatcher<T>(handler, channel));
	}
	public <T extends SelectableChannel> void registerWriteHandler(WriteHandler<T> handler, T channel) {
		register(channel, SelectionKey.OP_WRITE, new HandlerWriteDispatcher<T>(handler, channel));
	}
	
	public void unregister(SelectableChannel channel) {
		unregisterAccept(channel);
		unregisterConnect(channel);
		unregisterRead(channel);
		unregisterWrite(channel);
	}	
	public void unregisterAccept(SelectableChannel channel) {
		unregister(channel, SelectionKey.OP_ACCEPT);
	}
	public void unregisterConnect(SelectableChannel channel) {
		unregister(channel, SelectionKey.OP_CONNECT);
	}
	public void unregisterRead(SelectableChannel channel) {
		unregister(channel, SelectionKey.OP_READ);
	}
	public void unregisterWrite(SelectableChannel channel) {
		unregister(channel, SelectionKey.OP_WRITE);
	}
	
	/**
	 * Registration has to happen in the thread that does select. So the dispatcher has it's own little action queue so we can 
	 * do the registration in that thread.
	 * */
	private <T extends SelectableChannel> void register(final T channel, final int operation, final HandlerDispatcher<?> handlerDispatcher) {
		if (channel == null) throw new IllegalArgumentException("channel may not be null");
		if (!channel.isOpen()) throw new IllegalArgumentException("channel may not be closed");
					
		this.dispatch(new Runnable() {
			@Override
			public void run() {
				try {
					Dispatcher.this.handlersByChannelByOperation.get(operation).put(channel, handlerDispatcher);	
					int interestOps = 0;
					SelectionKey key = channel.keyFor(selector);

					if (key != null) {
						interestOps = key.interestOps();
					}
					
					final int registeredInterestOps = interestOps;
					
					channel.register(selector, operation | registeredInterestOps);
				} catch (ClosedChannelException e) {
					System.err.println("Could not register operation with channel because the channel is closed.");
					Dispatcher.this.handlersByChannelByOperation.get(operation).remove(channel);	
					e.printStackTrace();
				}
			}
		});
	}
	
	private void unregister(final SelectableChannel channel, final int operation) {
		this.dispatch(new Runnable() {
			@Override
			public void run() {
				SelectionKey key = channel.keyFor(selector);
				
				if (key != null && key.isValid()) {
					int ops = key.interestOps();

					ops &= ~operation;
					
					key.interestOps(ops);
				}
				
				Dispatcher.this.handlersByChannelByOperation.get(operation).remove(channel);
			}
		});
	}
	
	public void start() {
		if (this.isCurrentlyRunning) {
			System.err.println("Attempted to start a dispatcher that is already running.");
			
			return;
		}
		
		this.isCurrentlyRunning = true;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				Dispatcher.this.loop();
			}
		}).start();
	}
	public void stop() {
		this.isCurrentlyRunning = false;
		this.selector.wakeup();
	}
	
	public void loop() {
		while (this.isCurrentlyRunning) {
			int readyChannels = 0;
			try {
				readyChannels = selector.select();
				
				while (!this.runnables.isEmpty()) {
					this.runnables.take().run();
				}
			} catch (IOException e) {
				this.isCurrentlyRunning = true;
				System.err.println("Dispatcher stopped due to IO Exception.");
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (readyChannels == 0) continue;
			Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
			
			while(keyIterator.hasNext()) {
				final SelectionKey key = keyIterator.next();
				
				for (final int operation : ALL_OPERATIONS) {
					if(key.isValid() && (key.readyOps() & operation) != 0) {
						this.executor.execute(new Runnable() {
							@Override
							public void run() {
								HandlerDispatcher<?> dispatcher = Dispatcher.this.handlersByChannelByOperation.get(operation).get(key.channel());
								if (dispatcher != null) {
									dispatcher.dispatch();
								}
							}
						});
					}
					if (!this.isCurrentlyRunning) break;
				}
				
				keyIterator.remove();
			}
		}
	}
	
	private static abstract class HandlerDispatcher<T extends InterruptibleChannel> {
		T socket;
		
		public HandlerDispatcher(T socket) {
			if (socket == null) throw new IllegalArgumentException("socket may not be null");
			
			this.socket = socket;
		}
		
		public abstract void dispatch();
	}
	private static class HandlerAcceptDispatcher<T extends InterruptibleChannel> extends HandlerDispatcher<T> {
		AcceptHandler<T> handler;
		
		public HandlerAcceptDispatcher(AcceptHandler<T> handler, T socket) {			
			super(socket);
			if (handler == null) throw new IllegalArgumentException("handler may not be null");

			this.handler = handler;
		}
		
		public void dispatch() {
			this.handler.onAcceptable(this.socket);
		}
	}
	private static class HandlerConnectDispatcher<T extends InterruptibleChannel> extends HandlerDispatcher<T> {
		ConnectHandler<T> handler;
		
		public HandlerConnectDispatcher(ConnectHandler<T> handler, T socket) {
			super(socket);
			if (handler == null) throw new IllegalArgumentException("handler may not be null");
			
			this.handler = handler;
		}
		
		public void dispatch() {
			this.handler.onConnectable(this.socket);
		}
	}
	private static class HandlerReadDispatcher<T extends InterruptibleChannel> extends HandlerDispatcher<T> {
		ReadHandler<T> handler;
		
		public HandlerReadDispatcher(ReadHandler<T> handler, T socket) {
			super(socket);
			if (handler == null) throw new IllegalArgumentException("handler may not be null");

			this.handler = handler;
		}
		
		public void dispatch() {
			this.handler.onReadable(this.socket);
		}
	}
	private static class HandlerWriteDispatcher<T extends InterruptibleChannel> extends HandlerDispatcher<T> {
		WriteHandler<T> handler;
		
		public HandlerWriteDispatcher(WriteHandler<T> handler, T socket) {
			super(socket);
			if (handler == null) throw new IllegalArgumentException("handler may not be null");

			this.handler = handler;
		}
		
		public void dispatch() {
			this.handler.onWriteable(this.socket);
		}
	}
}
