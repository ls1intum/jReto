package de.tum.in.www1.jReto.routing.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable;

/**
* A LinkState packet represents a peer's link state, i.e. a list of all of it's neighbors and the cost associated with reaching them.
*/
public class LinkStatePacket implements Packet {
	public final static PacketType TYPE = PacketType.LINK_STATE;
	public final static int MINIMUM_LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE + Constants.INT_SIZE;
	
    /** The identifier of the peer that generated the packet. */
	public final UUID peerIdentifier;
    /** A list of identifier/cost pairs for each of the peer's neighbors. */
	public final List<LinkStateRoutingTable.NeighborInformation<UUID>> neighbors;

	public LinkStatePacket(UUID peerIdentifier, List<LinkStateRoutingTable.NeighborInformation<UUID>> neighbors) {
		this.peerIdentifier = peerIdentifier;
		this.neighbors = neighbors;
	}
	
	public static LinkStatePacket deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, MINIMUM_LENGTH)) return null;
		
		UUID identifier = reader.getUUID();
		int neighborCount = reader.getInt();
		
		if (!reader.checkRemaining(neighborCount * (Constants.UUID_SIZE + Constants.INT_SIZE))) return null;
		
		ArrayList<LinkStateRoutingTable.NeighborInformation<UUID>> neighbors = new ArrayList<>();
		
		for (int i=0; i<neighborCount; i++) {
			neighbors.add(new LinkStateRoutingTable.NeighborInformation<UUID>(reader.getUUID(), reader.getInt()));
		}
		
		return new LinkStatePacket(identifier, neighbors);
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(MINIMUM_LENGTH + this.neighbors.size() * (Constants.UUID_SIZE + Constants.INT_SIZE));
		data.add(TYPE);
		data.add(this.peerIdentifier);
		data.add(this.neighbors.size());
		
		for (LinkStateRoutingTable.NeighborInformation<UUID> neighbor : this.neighbors) {
			data.add(neighbor.node);
			data.add((int)neighbor.cost);
		}
		
		return data.getData();
	}
}
