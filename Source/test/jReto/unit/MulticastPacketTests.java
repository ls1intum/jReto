package jReto.unit;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;

import de.tum.in.www1.jReto.routing.algorithm.Tree;
import de.tum.in.www1.jReto.routing.packets.MulticastHandshake;

public class MulticastPacketTests {
	@Test
	public void testMulticastPacketSimple() {
		Tree<UUID> testTree = new Tree<>(UUID.randomUUID());
				
		MulticastHandshake handshake = new MulticastHandshake(UUID.randomUUID(), new HashSet<UUID>(Arrays.asList(UUID.randomUUID())), testTree);
		MulticastHandshake handshake2 = MulticastHandshake.deserialize(handshake.serialize());
		
		assertTrue(handshake.sourcePeerIdentifier.equals(handshake2.sourcePeerIdentifier));
		assertTrue(handshake.destinationIdentifiers.equals(handshake2.destinationIdentifiers));
		assertTrue(handshake.nextHopsTree.equals(handshake2.nextHopsTree));
	}
	
	@Test
	public void testMulticastPacket() {
		Tree<UUID> testTree =
				new Tree<>(UUID.randomUUID(), 
					new Tree<>(UUID.randomUUID(), 
						new Tree<>(UUID.randomUUID()), 
						new Tree<>(UUID.randomUUID())
					)
				);
				
		MulticastHandshake handshake = new MulticastHandshake(UUID.randomUUID(), new HashSet<UUID>(Arrays.asList(UUID.randomUUID(), UUID.randomUUID())), testTree);
		
		MulticastHandshake handshake2 = MulticastHandshake.deserialize(handshake.serialize());
		
		assertTrue(handshake.sourcePeerIdentifier.equals(handshake2.sourcePeerIdentifier));
		assertTrue(handshake.destinationIdentifiers.equals(handshake2.destinationIdentifiers));
		
		assertTrue(handshake.nextHopsTree.equals(handshake2.nextHopsTree));
	}
}
