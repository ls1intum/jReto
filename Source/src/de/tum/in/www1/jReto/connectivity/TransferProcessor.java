package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import de.tum.in.www1.jReto.Connection.DataProvider;
import de.tum.in.www1.jReto.connectivity.packet.CancelledTransferPacket;
import de.tum.in.www1.jReto.connectivity.packet.DataPacket;
import de.tum.in.www1.jReto.connectivity.packet.ProgressInformationPacket;
import de.tum.in.www1.jReto.connectivity.packet.StartedTransferPacket;
import de.tum.in.www1.jReto.connectivity.packet.ProgressInformationPacket.TransferProgressInformation;
import de.tum.in.www1.jReto.packet.PacketType;

/**
* The TransferManager class is responsible to perform data transfers. It supports resuming transfers after a connection failed during a transfer, 
* and the cancellation of transfers.
*/
public class TransferProcessor implements PacketConnection.Handler, TransferManager {
	public static interface TransferProcessorHandler {
	    /** Called when an incoming transfer starts. */
		void notifyTransferStarted(InTransfer transfer);
	}
    /** The TransferManager's delegate.*/
	private TransferProcessorHandler handler;
    /** The packetConnection used to send and receive packets. */
	private final PacketConnection packetConnection;

    /** Whether all transfers are currently interrupted. This is the case when a packet connection's underlying connection fails. */
	private boolean isInterrupted;
    /** The transfer that is currently being received. */
	private InTransfer currentInTransfer;
    /** The transfer that is currently being sent. */
	private OutTransfer currentOutTransfer;
    /** A queue of transfers that will be sent next. */
	private Queue<OutTransfer> outTransferQueue;
	
    /** 
    * Constructs a new TransferManager.
    * 
    * @param packetConnection The PacketConnection used to send and receive data transfers.
    */
	public TransferProcessor(PacketConnection packetConnection) {
		this.outTransferQueue = new LinkedList<OutTransfer>();
		this.isInterrupted = false;
		this.packetConnection = packetConnection;
		
		this.packetConnection.addDelegate(this);
	}
	
    /** 
    * Starts a transfer.
    *
    * @param dataLength The length of the transfer in bytes.
    * @param dataProvider A function that returns data for a given range.
    * @return An OutTransfer object.
    */
	public OutTransfer startTransfer(int transferLength, DataProvider dataProvider) {
		OutTransfer transfer = new OutTransfer(this, transferLength, dataProvider, UUID.randomUUID());
		
		this.outTransferQueue.add(transfer);
		
		this.packetConnection.write();		
		return transfer;
	}
	
    /** Cancels an incoming transfer. */
	public void cancelTransfer(InTransfer transfer) {
		if (transfer == null) throw new IllegalArgumentException("transfer may not be null.");
		
		if (transfer == this.currentInTransfer) {
			this.packetConnection.writePacket(new CancelledTransferPacket(this.currentInTransfer.getIdentifier()));
		} else {
			throw new IllegalArgumentException("Transfer is not the current in transfger");
		}

		this.packetConnection.write();		
	}
    /** Cancels an outgoing transfer. */
	public void cancelTransfer(OutTransfer transfer) {
		if (transfer == null) throw new IllegalArgumentException("transfer may not be null.");
		
		if (this.outTransferQueue.contains(transfer)) {
			this.outTransferQueue.remove(transfer);
			transfer.confirmCancel();
			return;
		} else if (transfer == this.currentOutTransfer) {
			this.packetConnection.writePacket(new CancelledTransferPacket(this.currentOutTransfer.getIdentifier()));
			this.currentOutTransfer.confirmCancel();
			this.currentOutTransfer = null;
		}

		this.packetConnection.write();		
	}
    /** Called when progress information about a transfer is received. The affected transfer's progress is set according to the information received. */
	private void handleProgressInformation(ProgressInformationPacket progressInformation) {
		if (progressInformation == null) {
			System.err.println("Received invalid packet.");
			return;
		}
		
		// TODO: current out transfer might be null
		for (ProgressInformationPacket.TransferProgressInformation information : progressInformation.progressInformation) {
			if (information.transferIdentifier.equals(this.currentOutTransfer.getIdentifier())) {
				this.currentOutTransfer.setProgress(information.progress);
				this.currentOutTransfer.setInterrupted(false);
			} else {
				throw new Error("Received progress information for unrecognized transfer.");
			}
		}
		
		if (this.currentOutTransfer != null && this.currentOutTransfer.getIsInterrupted()) {
			this.currentOutTransfer.setProgress(0);
			this.currentOutTransfer.setInterrupted(false);
			this.packetConnection.writePacket(new StartedTransferPacket(this.currentOutTransfer.getIdentifier(), this.currentOutTransfer.getLength()));
		}
		
		this.isInterrupted = false;
		this.packetConnection.write();
	}
    /** Called when a transfer is started. */
	private void handleStartedTransfer(StartedTransferPacket startedTransfer) {
		if (startedTransfer == null) {
			System.err.println("Received invalid packet.");
			return;
		}
		
		if (this.currentInTransfer != null) throw new Error("Attempted to start in transfer, but there is still an in transfer active.");
		
		this.currentInTransfer = new InTransfer(this, startedTransfer.transferLength, startedTransfer.transferIdentifier);
		
		this.handler.notifyTransferStarted(this.currentInTransfer);
		this.currentInTransfer.confirmStart();
	}
    /** Handles a cancelled transfer packet. */
	private void handleCancelledTransfer(CancelledTransferPacket cancelledTransferPacket) {
		if (cancelledTransferPacket == null) {
			System.err.println("Received invalid packet.");
			return;
		}
		
		if (this.currentOutTransfer != null && cancelledTransferPacket.transferIdentifier.equals(this.currentOutTransfer.getIdentifier())) {
			this.cancelTransfer(this.currentOutTransfer);
		} else if (this.currentInTransfer != null && cancelledTransferPacket.transferIdentifier.equals(this.currentInTransfer.getIdentifier())) {
			this.currentInTransfer.confirmCancel();
			this.currentInTransfer = null;
		} else {
			System.out.println("Received cancel request for an unknown transfer. The transfer was probably finnished before the cancel request was received.");
		}
	}
    /** Handles a data packet. */
	private void handleData(DataPacket dataPacket) {
		if (this.currentInTransfer == null) throw new Error("Received data, but there is no active in transfer.");
		
		this.currentInTransfer.updateWithReceivedData(dataPacket.data);
		
		if (this.currentInTransfer.getIsCompleted()) this.currentInTransfer = null;
	}
	
	@Override
	public Set<PacketType> getHandledPacketTypes() {
		Set<PacketType> types = new HashSet<>();
		types.add(PacketType.PROGRESS_INFORMATION);
		types.add(PacketType.TRANSFER_STARTED);
		types.add(PacketType.CANCELLED_TRANSFER);
		types.add(PacketType.DATA_PACKET);
		return types;
	}
	@Override
	public void handlePacket(ByteBuffer packet, PacketType type) {
		switch (type) {
			case PROGRESS_INFORMATION: handleProgressInformation(ProgressInformationPacket.deserialize(packet)); break;
			case TRANSFER_STARTED: handleStartedTransfer(StartedTransferPacket.deserialize(packet)); break;
			case CANCELLED_TRANSFER: handleCancelledTransfer(CancelledTransferPacket.deserialize(packet)); break;
			case DATA_PACKET: handleData(DataPacket.deserialize(packet)); break;
			default: throw new IllegalArgumentException("Invalid type: "+type);
		}
	}
	@Override
	public void onUnderlyingConnectionClose(PacketConnection connection) {}
	@Override
	public void onWillSwapUnderlyingConnection(PacketConnection connection) {
		if (this.isInterrupted) return;
		
		this.isInterrupted = true;
		if (this.currentInTransfer != null) this.currentInTransfer.setInterrupted(true);
		if (this.currentOutTransfer != null) this.currentOutTransfer.setInterrupted(true);
	}

	@Override
	public void onUnderlyingConnectionConnected(PacketConnection connection) {
		if (!this.isInterrupted) return;
		
		List<TransferProgressInformation> progressInformation = new ArrayList<TransferProgressInformation>();
		
		if (this.currentInTransfer != null) {
			progressInformation.add(new TransferProgressInformation(this.currentInTransfer.getIdentifier(), this.currentInTransfer.getProgress()));
			this.currentInTransfer.setInterrupted(false);
		}
		
		this.packetConnection.writePacket(new ProgressInformationPacket(progressInformation));
		this.packetConnection.write();
	}

	@Override
	public void onNoPacketsLeft(PacketConnection connection) {
		if (this.isInterrupted) return;
		int packetLength = 1024;
		if (this.packetConnection.getUnderlyingConnection() != null) {
			packetLength = this.packetConnection.getUnderlyingConnection().getRecommendedPacketSize();
		}
		
		if (currentOutTransfer != null) {
			this.packetConnection.writePacket(currentOutTransfer.nextPacket(packetLength));
			
			if (this.currentOutTransfer.getIsCompleted()) {
				this.currentOutTransfer = null;
			}
		} else {
			this.currentOutTransfer = this.outTransferQueue.poll();
			
			if (this.currentOutTransfer != null) {
				this.currentOutTransfer.confirmStart();
				this.packetConnection.writePacket(new StartedTransferPacket(this.currentOutTransfer.getIdentifier(), this.currentOutTransfer.getLength()));
			}
		}
	}
	
	public TransferProcessorHandler getHandler() {
		return this.handler;
	}
	public void setHandler(TransferProcessorHandler handler) {
		this.handler = handler;
	}
}
