package de.tum.in.www1.jReto;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.connectivity.DefaultDataSource;
import de.tum.in.www1.jReto.connectivity.InTransfer;
import de.tum.in.www1.jReto.connectivity.OutTransfer;
import de.tum.in.www1.jReto.connectivity.PacketConnection;
import de.tum.in.www1.jReto.connectivity.ReliablitiyManager;
import de.tum.in.www1.jReto.connectivity.TransferProcessor;
import de.tum.in.www1.jReto.connectivity.ReliablitiyManager.PacketConnectionManager;
import de.tum.in.www1.jReto.connectivity.ReliablitiyManager.ReliabilityManagerHandler;
import de.tum.in.www1.jReto.connectivity.TransferProcessor.TransferProcessorHandler;
import de.tum.in.www1.jReto.routing.Node;

/**
* Connection objects can be used to send and receive data. They can be established by calling connect on a RemotePeer or the LocalPeer.
* If necessary, routing will be used automatically if a remote peer is not reachable directly. The Connection object will also automatically attempt to reconnect to a peer, should a route become unavailable. 
* Furthermore, the Connection object will attempt to automatically upgrade the connection to the remote peer if a better route becomes available.
*
* Events
* The Connection class gives access to five events: 
* - onConnect: Called when the connection connects successfully. If the connection is already connected when this property is set, it is called immediately. Passes the connection that connected as a parameter.
* - onTransfer: Called when an incoming transfer starts. It's possible to specify how the data is received, cancel the transfer, receive progress updates. Passes the connection that received the transfer and the transfer object as parameters.
* - onData: Convenience alternative to onTransfer. If set, transfers are received automatically, and passed to this closure on completion. Is used only if onTransfer is not set. 
* - onClose: Called when the connection closes. Passes the connection that closed as the parameter.
* - onError: Called when an error occurs that caused the connection to close. If not set, onClose will be called when an error occurs. Passes the connection that closed as the parameter.
* 
* You can react to the events by simply setting a lambda, e.g.
* connection.setOnData((receivingConnection, data) -> System.out.println("received data"))
* None of these events have to be handled, however, if you wish to receive data, you need to react to either the onTransfer or onData event.
*
* Sending Data
* Data can be sent using the send methods. 
*
* Receiving Data
* The Connection class offers two means of receiving data that are mutually exclusive:
* 1. Setting onData: the closure will be called whenever data is received. For each send method call on the remote peer, 
*    onData will be called once when the data is transmitted. This is a convenience method and does not give you access to any additional information about transfers.
* 2. Setting onTransfer: This closure is called when a data transfer starts, i.e. before the data is transmitted.
*    It gives access to a InTransfer object that allows you to respond to events (such as progress updates), and allows you to specify how the transfer should be received. It can also be used to cancel transfers.
*/
public class Connection {
	public static interface ConnectHandler {
		void onConnect(Connection connection);
	}
	public static interface IncomingTransferHandler {
		void onTransferAvailable(Connection connection, InTransfer transfer);
	}
	public static interface ConnectionDataHandler {
		void onData(Connection connection, ByteBuffer data);
	}
	public static interface CloseHandler {
		void onClose(Connection connection);
	}
	public static interface ErrorHandler {
		void onError(Connection connection, String error);
	}
	
	private ConnectHandler connectHandler;
	private IncomingTransferHandler incomingTransferStartedHandler;
	private ConnectionDataHandler dataHandler;
	private CloseHandler closeHandler;
	private ErrorHandler errorHandler;
	
    /** The trasfer manager, which is responsible for data transmissions. */
	private TransferProcessor transferProcessor;
    /** The reliability manager, which is responsible for cleanly closing connections and providing automatic reconnect functionality. */
	private ReliablitiyManager reliabilityManager;
    /** Whether this connection is currently connected. */
	private boolean isConnected = false;
	
	/** Implements the TransferProcessor's Handler protocol and calls methods appropriately */
	private TransferProcessorHandler transferProcessorHandler = new TransferProcessorHandler() {
		@Override
		public void notifyTransferStarted(InTransfer transfer) {
			Connection.this.notifyTransferStarted(transfer);
		}
	}; 
	/** Implements the ReliabilityManager's Handler protocol and calls methods appropriately */
	private ReliabilityManagerHandler reliablityHandler = new ReliabilityManagerHandler() {	
		@Override
		public void onUnexpectedFinalConnectionClose() {
			notifyError();
		}
		
		@Override
		public void onExpectedConnectionClose() {
			notifyClose();
		}

		@Override
		public void onConnectionConnected() {
			notifyConnect();
		}
	};
	
    /**
    * Constructs a Connection. Users should use RemotePeer and LocalPeer's connect() methods to establish connections.
    *
    * @param packetConnection The packet connection used by this connection.
    * @param localIdentifier: The local peer's identifier
    * @param executor The Executor delegate methods and events are dispatched on
    * @param isConnectionEstablisher: A boolean indicating whether the connection was established on this peer. If the connection closes unexpectedly, the 
    *           connection establisher is responsible for any reconnect attempts.
    * @param connectionManager The connection's manager. If a reconnect is required, it is responsible to establish a new underlying connection.
    */
	Connection(PacketConnection packetConnection, UUID localIdentifier, Executor executor, boolean isConnectionEstablisher, PacketConnectionManager connectionManager) {	
		this.transferProcessor = new TransferProcessor(packetConnection);
	
		Set<UUID> destinationIdentifiers = new HashSet<>();
		for (Node destination : packetConnection.getDestinations()) destinationIdentifiers.add(destination.getIdentifier());
		this.reliabilityManager = new ReliablitiyManager(packetConnection, connectionManager, localIdentifier, destinationIdentifiers, isConnectionEstablisher, executor);
		
		this.transferProcessor.setHandler(this.transferProcessorHandler);
		this.reliabilityManager.setHandler(this.reliablityHandler);
	}

	void setTransferProcessor(TransferProcessor transferProcessor) {
		this.transferProcessor = transferProcessor;
	}
	void setReliablitiyManager(ReliablitiyManager reliabilityManager) {
		this.reliabilityManager = reliabilityManager;
	}
    /** Whether this connection is currently connected. */
	public boolean isConnected() {
		return this.isConnected;
	}
	
	/**
	 * Closes this connection.
	 * */
	public void close() {
		this.isConnected = false;
		Connection.this.reliabilityManager.closeConnection();
	}
	/**
	 * Attempts to reconnect this connection. You can call this method when the connection was closed explicitly (i.e. by calling close()) or when it was closed by a network fault.
	 * */
	public void attemptReconnect() {
		Connection.this.reliabilityManager.attemptConnect();
	}

	/**
	 * Sends data.
	 * 
	 * @param data The data to be sent.
	 */
	public OutTransfer send(ByteBuffer data) {
		DefaultDataSource dataSource = new DefaultDataSource(data);
		
		return this.send(dataSource.getDataLength(), (offset, length) -> dataSource.getData(offset, length));
	}
	public static interface DataProvider {
		ByteBuffer getData(int offset, int length);
	}
	/**
	 * Sends data. Use this if you don't want to store the full data in memory at once. A dataProvider is used that lets you load data piece by piece.
	 * 
	 * @param dataLength The total length of this transfer.
	 * @param dataProvider Called when another chunk of data is needed.
	 */
	public OutTransfer send(int dataLength, DataProvider dataProvider) {
		return this.transferProcessor.startTransfer(dataLength, dataProvider);
	}
	
	private void notifyTransferStarted(InTransfer transfer) {
		if (this.incomingTransferStartedHandler != null) {
			this.incomingTransferStartedHandler.onTransferAvailable(this, transfer);
		} else if (this.dataHandler != null) {
			transfer.setOnCompleteData((t, data) -> this.dataHandler.onData(this, data));
		} else {
			System.err.println("You need to set either onTransfer or onData on connection "+this);
		}
	}
	private void notifyConnect() {
		this.isConnected = true;
		if (this.connectHandler != null) this.connectHandler.onConnect(this);
	}
	private void notifyClose() {
		this.isConnected = false;
		if (this.closeHandler != null) this.closeHandler.onClose(this);
	}
	private void notifyError() {
		this.isConnected = false;
		if (this.errorHandler != null) this.errorHandler.onError(this, "The connection was closed unexpectedly and could not be reestablished.");
	}
	public ConnectHandler getOnConnect() {
		return connectHandler;
	}
	public void setOnConnect(ConnectHandler connectHandler) {
		if (this.isConnected) { this.connectHandler.onConnect(this); }
		this.connectHandler = connectHandler;
	}
	public IncomingTransferHandler getOnTransfer() {
		return incomingTransferStartedHandler;
	}
	public void setOnTransfer(IncomingTransferHandler incomingTransferStartedHandler) {
		this.incomingTransferStartedHandler = incomingTransferStartedHandler;
	}
	public ConnectionDataHandler getOnData() {
		return this.dataHandler;
	}
	public void setOnData(ConnectionDataHandler dataHandler) {
		this.dataHandler = dataHandler;
	}
	public CloseHandler getOnClose() {
		return closeHandler;
	}
	public void setOnClose(CloseHandler closeHandler) {
		this.closeHandler = closeHandler;
	}
	public ErrorHandler getOnError() {
		return errorHandler;
	}
	public void setOnError(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}
}
