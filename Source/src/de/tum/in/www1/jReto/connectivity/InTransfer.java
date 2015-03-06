package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;
import java.util.UUID;
/**
* An InTransfer represents a data transfer from a remote peer to the local peer. The connection class generates InTransfer instances when a remote peer sends data.
* 
* This class exposes several events that can be set with the corresponding methods:
* 
* - onCompleteData: Called when the transfer completes with the full data received. Buffers the data in memory until the transfer is complete. Alternative to onPartialData. If both are set, onPartialData is used.
* - onPartialData: Called whenever data is received. This method may be called multiple times, i.e. the data is not the full transfer. Exclusive alternative to onCompleteData.
*/
public class InTransfer extends Transfer {
	public static interface CompleteDataHandler {
		void onData(InTransfer transfer, ByteBuffer data);
	}
	public static interface PartialDataHandler {
		void onPartialData(InTransfer transfer, ByteBuffer data);
	}
	
	private CompleteDataHandler completeDataHandler;
	private PartialDataHandler partialDataHandler;
	private DefaultDataConsumer defaultDataConsumer;
	
	public InTransfer(TransferManager transferManager, int lenght, UUID identifier) {
		super(transferManager, lenght, identifier);
	}
	
	void updateWithReceivedData(final ByteBuffer data) {
		int dataLength = data.remaining();

		if (this.partialDataHandler != null) {
			this.partialDataHandler.onPartialData(this, data);
		} else if (this.completeDataHandler != null) {
			if (this.defaultDataConsumer == null) {
				this.defaultDataConsumer = new DefaultDataConsumer(this.getLength());
			}
			this.defaultDataConsumer.consume(data);
		} else {
			System.err.println("You need to set either onCompleteData or onPartialData on incoming transfers (affected instance: "+this);
		}
		
		this.updateProgress(dataLength);
	}

	public void cancel() {
		InTransfer.this.getTransferManager().cancelTransfer(InTransfer.this);
	}
	
	void confirmEnd() {
		this.defaultDataConsumer = null;
		
		super.confirmEnd();
	}
	void confirmCompletion() {
		if (this.completeDataHandler != null) this.completeDataHandler.onData(this, this.defaultDataConsumer.getData());

		super.confirmCompletion();
	}
	
	public CompleteDataHandler getOnCompleteData() {
		return this.completeDataHandler;
	}
	/** Sets the onCompleteData event handler. */
	public void setOnCompleteData(CompleteDataHandler completeDataHandler) {
		this.completeDataHandler = completeDataHandler;
	}
	public PartialDataHandler getOnPartialData() {
		return this.partialDataHandler;
	}
	/** Sets the onPartialData event handler. */
	public void setOnPartialData(PartialDataHandler partialDataHandler) {
		this.partialDataHandler = partialDataHandler;
	}
}
