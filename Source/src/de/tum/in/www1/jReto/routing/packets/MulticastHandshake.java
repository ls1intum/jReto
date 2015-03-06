package de.tum.in.www1.jReto.routing.packets;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import de.tum.in.www1.jReto.packet.Constants;
import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;
import de.tum.in.www1.jReto.routing.algorithm.Tree;

/**
* The MulticastHandshake contains information relevant to establish a routed multi- or unicast connection.
* It is sent to each peer that is part of the route.
* It contains the identifier of the peer that originally established the peer, the set of destinations of the connection, and the direct connections that
* still need to be established structured as a tree (the nextHopTree). When a peer receives a MulticastHandshake, the nextHopTree is always rooted at that tree. 
* That node is expected to establish connections to all nodes that are its children in the nextHopTree.
*/
public class MulticastHandshake implements Packet {
	public final static PacketType TYPE = PacketType.ROUTING_HANDSHAKE;
	public final static int MINIMUM_LENGTH = Constants.PACKET_TYPE_SIZE + Constants.UUID_SIZE;
	
	public final UUID sourcePeerIdentifier;
	public final Set<UUID> destinationIdentifiers;
	public final Tree<UUID> nextHopsTree;
	
	public MulticastHandshake(UUID sourcePeerIdentifier, Set<UUID> destinationIdentifiers, Tree<UUID> nextHopsTree) {
		if (destinationIdentifiers.size() == 0) throw new IllegalArgumentException("At least one destination is required.");
		
		this.sourcePeerIdentifier = sourcePeerIdentifier;
		this.destinationIdentifiers = destinationIdentifiers;
		this.nextHopsTree = nextHopsTree;
	}
	
	public static MulticastHandshake deserialize(ByteBuffer data) {
		DataReader reader = new DataReader(data);
		if (!DataChecker.check(reader, TYPE, MINIMUM_LENGTH)) return null;
		
		UUID sourcePeerIdentifier = reader.getUUID();
		int destinationsCount = reader.getInt();
		
		if (destinationsCount == 0) {
			System.err.println("Invalid MulticastHandshake: No destinations specified.");
			return null;
		}
		if (!reader.checkRemaining(destinationsCount * Constants.UUID_SIZE)) {
			System.err.println("Invalid MulticastHandshake: Not enough data remaining to read destinations.");
			return null;
		}
		
		Set<UUID> destinations = new HashSet<>();
		for (int i=0; i<destinationsCount; i++) {
			destinations.add(reader.getUUID());
		}
		
		Tree<UUID> nextHopsTree = deserializeNextHopTree(reader);
		
		return new MulticastHandshake(sourcePeerIdentifier, destinations, nextHopsTree);
	}
	public ByteBuffer serialize() {
		DataWriter data = new DataWriter(MINIMUM_LENGTH + Constants.INT_SIZE + destinationIdentifiers.size() * Constants.UUID_SIZE + nextHopsTree.size() * (Constants.INT_SIZE + Constants.UUID_SIZE));
		data.add(TYPE);
		data.add(this.sourcePeerIdentifier);
		data.add(destinationIdentifiers.size());
		for (UUID destinationIdentifier : destinationIdentifiers) data.add(destinationIdentifier);
		
		serializeNextHopTree(data, nextHopsTree);
		
		return data.getData();
	}
	
	private static Tree<UUID> deserializeNextHopTree(DataReader reader) {
		if (!reader.checkRemaining(Constants.UUID_SIZE + Constants.INT_SIZE)) {
			System.err.println("Invalid MulticastHandshake: Not enough data remaining to read hop tree.");
			return null;
		}
		
		UUID value = reader.getUUID();
		int childrenCount = reader.getInt();
		Set<Tree<UUID>> children = new HashSet<>();
		
		for (int i=0; i<childrenCount; i++) {
			Tree<UUID> child = deserializeNextHopTree(reader);
			if (child == null) return null;
			
			children.add(child);
		}
		
		return new Tree<>(value, children);
	}
	private void serializeNextHopTree(DataWriter data, Tree<UUID> tree) {
		data.add(tree.value);
		data.add(tree.children.size());
		
		for (Tree<UUID> child : tree.children) {
			serializeNextHopTree(data, child);
		}
	}
}
