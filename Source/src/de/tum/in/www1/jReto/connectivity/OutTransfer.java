package de.tum.in.www1.jReto.connectivity;

import java.nio.ByteBuffer;
import java.util.UUID;

import de.tum.in.www1.jReto.Connection.DataProvider;
import de.tum.in.www1.jReto.connectivity.packet.DataPacket;

/**
* An OutTransfer represents a data transfer from the local peer to a remote peer. You can obtain one by calling the connection's send method.
*/
public class OutTransfer extends Transfer {
	private DataProvider dataSource;
	
	public OutTransfer(TransferManager transferManager, int dataLenght, DataProvider dataSource, UUID identifier) {
		super(transferManager, dataLenght, identifier);
		this.dataSource = dataSource;
	}
	
	public DataProvider getDataSource() {
		return this.dataSource;
	}
	
	DataPacket nextPacket(int length) {
		int dataLength = length - 4;
		
		dataLength = Math.min(this.getLength() - this.getProgress(), dataLength);
		ByteBuffer data = this.getDataSource().getData(this.getProgress(), dataLength);
		DataPacket packet = new DataPacket(data);
		
		this.updateProgress(dataLength);
		
		return packet;
	}

	public void cancel() {
		this.getTransferManager().cancelTransfer(OutTransfer.this);
	}
}
