package de.tum.in.www1.jReto.connectivity;

import java.util.UUID;

/**
* A Transfer object represents a data transfer between two or more peers. 
* It has two subclasses, InTransfer and OutTransfer, which represent an incoming transfer (i.e. a data transfer that is being received) and an outgoing transfer (i.e. a data transfer that is being sent to some other peer).
* OutTransfers are created by calling one of the send methods on the Connection class.
* InTransfers are created by the Connection class when the connected peer starts a data transfer. At this point, the connection's onTransfer event is invoked. Thus, InTransfers can be obtained by using the onTransfer event exposed by the Connection class.
* 
* This class exposes the following events for which handlers can be set using the appropriate methods:
* 
* - onStart: Called when the transfer starts. If this property is set when the transfer is already started, the closure is called immediately.
* - onProgress: Called whenever the transfer makes progress.
* - onComplete: Called when the transfer completes successfully. To receive the data from an incoming transfer, use onCompleteData or onPartialData of InTransfer.
* - onCancel: Called when the transfer is cancelled.
* - onEnd: Called when the transfer ends, either by cancellation or completion.
* 
* All of these events are optional. Note: Incoming transfers have separate events that give access to the received data. See the InTransfer class documentation.
*/
public abstract class Transfer {
	public static interface StartHandler {
		void onStart(Transfer transfer);
	}
	public static interface ProgressHandler {
		void onProgress(Transfer transfer);
	}
	public static interface CompletionHandler {
		void onComplete(Transfer transfer);
	}
	public static interface CancellationHandler {
		void onCancel(Transfer transfer);
	}
	public static interface EndHandler {
		void onEnd(Transfer transfer);
	}
	
    // Called when the transfer starts. If this property is set when the transfer is already started, the closure is called immediately.
	private StartHandler startHandler;
    // Called whenever the transfer makes progress.
	private ProgressHandler progressHandler;
    // Called when the transfer completes successfully. To receive the data from an incoming transfer, use onCompleteData or onPartialData of InTransfer.
	private CompletionHandler completionHandler;
    // Called when the transfer is cancelled.
	private CancellationHandler cancellationHandler;
    // Called when the transfer ends, either by cancellation or completion.
	private EndHandler endHandler;
	
	public StartHandler getOnStart() {
		return startHandler;
	}
	/** Sets the onStart event handler. */
	public void setOnStart(StartHandler startHandler) {
		this.startHandler = startHandler;
		
		if (this.isStarted) startHandler.onStart(this);
	}
	public ProgressHandler getOnProgress() {
		return progressHandler;
	}
	/** Sets the onProgress event handler. */
	public void setOnProgress(ProgressHandler progressHandler) {
		this.progressHandler = progressHandler;
	}
	public CompletionHandler getOnComplete() {
		return completionHandler;
	}
	/** Sets the onComplete event handler. */
	public void setOnComplete(CompletionHandler completionHandler) {
		this.completionHandler = completionHandler;
	}
	public CancellationHandler getOnCancel() {
		return cancellationHandler;
	}
	/** Sets the onCancel event handler. */
	public void setOnCancel(CancellationHandler cancellationHandler) {
		this.cancellationHandler = cancellationHandler;
	}
	public EndHandler getOnEnd() {
		return endHandler;
	}
	/** Sets the onEnd event handler. */
	public void setOnEnd(EndHandler endHandler) {
		this.endHandler = endHandler;
	}
	
    /** Whether the transfer was been started */
	private boolean isStarted;
    /** Whether the transfer was completed successfully */
	private boolean isCompleted;
    /** Whether the transfer was cancelled */
	private boolean isCancelled;
    /** The transfer's length in bytes*/
	private final int length;
    /** The transfer's current progress in bytes */
	private int progress;
    /** Indicates if the transfer is currently interrupted. This occurs, for example, when a connection closes unexpectedly. The transfer is resumed automatically on reconnect. */
	private boolean isInterrupted;
    /** The transfer's identifier */
	private final UUID identifier;
    /** The transfer's manager. */
	private final TransferManager transferManager;

    /** 
    * Constructs a Transfer.
    * @param manager The TransferManager responsible for this transfer.
    * @param length The total length of the transfer in bytes.
    * @param identifier The transfer's identifier.
    */
	public Transfer(TransferManager transferManager, int length, UUID identifier) {
		this.length = length;
		this.identifier = identifier;
		this.transferManager = transferManager;
	}
	
    /** Whether all data was sent. */
	public boolean getIsAllDataTransmitted() {
		return this.progress == this.length;
	}
	
    /** The transfer's current progress in bytes */
	public int getProgress() {
		return this.progress;
	}
	
    /** The transfer's identifier */
	public UUID getIdentifier() {
		return this.identifier;
	}
	
    /** The transfer's length in bytes*/
	public int getLength() {
		return this.length;
	}
	
	/** Cancels the transfer. */
	public abstract void cancel();
	
    /** Updates the transfer's progress. */
	void updateProgress(int numberOfBytes) {
		if (this.length < this.progress+numberOfBytes) throw new IllegalArgumentException("You may not update the progress beyond the Transfer's length.");
		
		this.progress += numberOfBytes;
		
		this.confirmProgress();
		if (this.getIsAllDataTransmitted()) this.confirmCompletion();
	}
	
	void setInterrupted(boolean interrupted) {
		this.isInterrupted = interrupted;
	}
	
	public boolean getIsInterrupted() {
		return this.isInterrupted;
	}
    /** Whether the transfer was been started */
	public boolean getIsStarted() {
		return this.isStarted;
	}
    /** Whether the transfer was completed successfully */
	public boolean getIsCompleted() {
		return this.isCompleted;
	}
    /** Whether the transfer was cancelled */
	public boolean getIsCancelled() {
		return this.isCancelled;
	}
	
    /** Call to change the transfer's state to started and dispatch the associated events. */
	void confirmStart() {
		this.isStarted = true;
		if (this.startHandler != null) this.startHandler.onStart(this);
	}
    /** Call to confirm updated progress and dispatch the associated events. */
	void confirmProgress() {
		if (this.progressHandler != null) this.progressHandler.onProgress(this);
	}
    /** Call to change thet transfer's state to cancelled and dispatch the associated events. */
	void confirmCancel() {
		this.isCancelled = true;
		if (this.cancellationHandler != null) this.cancellationHandler.onCancel(this);
		this.confirmEnd();
	}
    /** Call to change thet transfer's state to completed and dispatch the associated events. */
	void confirmCompletion() {
		this.isCompleted = true;
		if (this.completionHandler != null) this.completionHandler.onComplete(this);
		this.confirmEnd();
	}
    /** Call to change thet transfer's state to ended, dispatch the associated events, and clean up events. */
	void confirmEnd() {
		if (this.endHandler != null) this.endHandler.onEnd(this);
	}

	/** Sets the transfer's progress. */
	void setProgress(int progress) {
		this.progress = progress;
	}
	TransferManager getTransferManager() {
		return this.transferManager;
	}
}
