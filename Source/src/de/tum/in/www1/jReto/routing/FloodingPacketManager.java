package de.tum.in.www1.jReto.routing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.packets.FloodingPacket;

/**
* The FloodingPacketManager implements the Flooding algorithm used to distribute packets through the network.
* When a packet is received, and it or a newer one has been seen before, it is discarded. This is accomplished by storing the last seen sequence number 
* for each sender.
* If the packet is new, it is forwarded to all direct neighbors of the local peer.
*/
public class FloodingPacketManager {
	public static interface PacketHandler {
		Set<PacketType> getHandledPacketTypes();
		void handlePacket(ByteBuffer data, PacketType type);
	}
	
    /** The Router responsible for this flooding packet manager. */
	private final Router router;

    /** The packet handler registered with this flooding packet manager  */
	private final PacketHandler packetHandler;
    /** The next sequence number that will be used for packets sent from this peer. */
	private int currentSequenceNumber = 0;
    /** The highest sequence number seen for each remote peer. */
	private Map<UUID, Integer> sequenceNumbers = new HashMap<>();
	
    /** Constructs a new FloodingPacketManager */
	public FloodingPacketManager(PacketHandler packetHandler, Router router) {
		this.packetHandler = packetHandler;
		this.router = router;
	}
	
	public Set<PacketType> getHandledPacketTypes() {
		return new HashSet<>(Arrays.asList(PacketType.FLOODED_PACKET));
	}
	
    /** Handles a received packet from a given source. If the packet is new, it is forwarded and handled, otherwise it is dismissed. */
	public void handlePacket(ByteBuffer data, PacketType type, UUID sourceIdentifier) {
		FloodingPacket floodingPacket = FloodingPacket.deserialize(data);
		
		if (floodingPacket == null) {
			System.err.println("Received invalid flooded packet.");
			return;
		}
		
		if (this.sequenceNumbers.containsKey(floodingPacket.originIdentifier) && 
				floodingPacket.sequenceNumber <= this.sequenceNumbers.get(floodingPacket.originIdentifier)) return;
		
		this.sequenceNumbers.put(floodingPacket.originIdentifier, floodingPacket.sequenceNumber);
		
		for (Node neighbor : this.router.getNeighborNodes()) {
			if (neighbor.getIdentifier().equals(sourceIdentifier)) continue;
			
			neighbor.sendPacket(floodingPacket);
		}

		PacketType subtype = PacketType.fromData(floodingPacket.payload);
		if (subtype == PacketType.UNKNOWN) {
			System.err.println("Flooded packet contains payload packet of unknown type (payload length: "+floodingPacket.payload.remaining()+").");
			return;
		}

		if (this.packetHandler.getHandledPacketTypes().contains(subtype)) {
			this.packetHandler.handlePacket(floodingPacket.payload.slice().order(ByteOrder.LITTLE_ENDIAN), subtype);
		} else {
			System.err.println("No packet handler for flooded packet with type: "+subtype);
		}
	}
	
    /** Floods a new packet through the network. Increases the sequence number and sends the packet to all neighbors. */
	public void floodPacket(Packet packet) {
		FloodingPacket floodingPacket = new FloodingPacket(this.router.getLocalNodeIdentifier(), this.currentSequenceNumber, packet.serialize());
		this.sequenceNumbers.put(this.router.getLocalNodeIdentifier(), this.currentSequenceNumber);
		this.currentSequenceNumber++;
		for (Node neighbor : this.router.getNeighborNodes()) {			
			neighbor.sendPacket(floodingPacket);
		}
	}
}
