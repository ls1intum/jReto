package jReto.unit;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import jReto.util.TestData;

import org.junit.Test;

import de.tum.in.www1.jReto.packet.DataChecker;
import de.tum.in.www1.jReto.packet.DataReader;
import de.tum.in.www1.jReto.packet.DataWriter;
import de.tum.in.www1.jReto.packet.Packet;
import de.tum.in.www1.jReto.packet.PacketType;

public class PacketTest {

	static class TestPacket implements Packet {
		public final PacketType type;
		public final int length;
		
		public TestPacket(PacketType type, int length) {
			this.type = type;
			this.length = length;
		}
		
		public static TestPacket deserialize(ByteBuffer data, PacketType expectedType, int expectedLength) {
			DataReader reader = new DataReader(data);
			if (!DataChecker.check(reader, expectedType, expectedLength)) return null;
			
			ByteBuffer testData = reader.getRemainingData();
			
			System.out.println(testData);
			
			TestData.verify(testData, testData.remaining());
			
			return new TestPacket(expectedType, expectedLength);
		}
		
		@Override
		public ByteBuffer serialize() {
			DataWriter data = new DataWriter(this.length);
			data.add(this.type);
			data.add(TestData.generate(length-4));
			return data.getData();
		}
	}
	
	@Test
	public void testPacketSubclass() {
		final PacketType type = PacketType.DATA_PACKET;
		final int length = 16;
		
		TestPacket packet = new TestPacket(type, length);
		TestPacket packet2 = TestPacket.deserialize(packet.serialize(), PacketType.DATA_PACKET, length);
		
		assertTrue(packet.type == packet2.type);
	}
	
	@Test
	public void testPacketInvalidLength1() {
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	    System.setErr(new PrintStream(outContent));
	    
		final PacketType type = PacketType.DATA_PACKET;
		
		TestPacket packet = TestPacket.deserialize(new TestPacket(type, 10).serialize(), type, 20);
		assertNull(packet);
	    assertEquals("Basic data check failed: Not enough data remaining. Needed: 20, available: 10\n", outContent.toString());
	}
	
	@Test
	public void testPacketInvalidType() {
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	    System.setErr(new PrintStream(outContent));
	    
		final int length = 16;

		TestPacket packet = TestPacket.deserialize(new TestPacket(PacketType.DATA_PACKET, length).serialize(), PacketType.CLOSE_REQUEST, length);
		assertNull(packet);
	    assertEquals("Basic data check failed: Unexpected type. Expected: CLOSE_REQUEST. Received: DATA_PACKET\n", outContent.toString());
	}
}
