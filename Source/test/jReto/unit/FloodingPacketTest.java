package jReto.unit;

import static org.junit.Assert.*;

import java.util.UUID;

import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.connectivity.packet.DataPacket;
import de.tum.in.www1.jReto.routing.packets.FloodingPacket;

public class FloodingPacketTest {

	@Test
	public void test() {
		UUID identifier = UUID.randomUUID();
		DataPacket packet = new DataPacket(TestData.generate(16));
		FloodingPacket flood = new FloodingPacket(identifier, 1, packet.serialize());
		FloodingPacket flood2 = FloodingPacket.deserialize(flood.serialize());
		
		assertNotNull(flood2);
		assert(flood2.payload.remaining() == 20);
		
		DataPacket packet2 = DataPacket.deserialize(flood2.payload);
	
		assertNotNull(packet2);
		TestData.verify(packet2.data, 16);
	}
}
