package de.tum.in.www1.jReto.connectivity.packet;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

/** 
* This packet is sent when a transfer was interrupted and can be resumed to ensure that any data that went missing is resent.
*/
public class ProgressInformationPacket implements Packet {	
	public final static PacketType TYPE = PacketType.PROGRESS_INFORMATION;
	public final static int MINIMUM_LENGTH = Constants.PACKET_TYPE_SIZE + Constants.INT_SIZE;
	
	public static class TransferProgressInformation {
		public final UUID transferIdentifier;
		public final int progress;
		
		public TransferProgressInformation(UUID transferIdentifier, int progress) {
			this.transferIdentifier = transferIdentifier;
			this.progress = progress;
		}
	}
	
	public final Collection<TransferProgressInformation> progressInformation;
	
	public ProgressInformationPacket(Collection<TransferProgressInformation> progressInformation) {
		this.progressInformation = progressInformation;
	}
	

	public static ProgressInformationPacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, MINIMUM_LENGTH)) return null;
		
		int informationCount = reader.getInt();
		HashSet<TransferProgressInformation> allInfo = new HashSet<>();

		if (!reader.checkRemaining(informationCount * (Constants.UUID_SIZE + Constants.INT_SIZE))) return null;
		
		for (int i=0; i<informationCount; i++) {
			allInfo.add(new TransferProgressInformation(reader.getUUID(), reader.getInt()));
		}
		
		return new ProgressInformationPacket(allInfo);
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(MINIMUM_LENGTH + this.progressInformation.size() * (Constants.UUID_SIZE + Constants.INT_SIZE));
		data.add(TYPE);
		data.add(this.progressInformation.size());
		
		for (TransferProgressInformation information : this.progressInformation) {
			data.add(information.transferIdentifier);
			data.add(information.progress);
		}

		return data.getData();
	}
}
