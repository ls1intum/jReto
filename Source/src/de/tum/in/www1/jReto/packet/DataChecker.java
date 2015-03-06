package de.tum.in.www1.jReto.packet;

public class DataChecker {
    /**
    * Verifies that the data (wrapped in a DataReader) has the expected packet type, and has the minimum required lenght (i.e. number of bytes).
    * @param data The data to check
    * @param expectedType The type the packet is expected to have
    * @param minimumLength The minimum length required for the packet to be valid
    * @return Whether the conditions are met
    */
	public static boolean check(DataReader data, PacketType expectedType, int minimumLength) {
		if (!data.checkRemaining(minimumLength)) {
			System.err.println("Basic data check failed: Not enough data remaining. Needed: "+minimumLength+", available: "+data.getRemainingBytes());
			return false;
		}
		PacketType type = data.getPacketType();
		if (type != expectedType) {
			System.err.println("Basic data check failed: Unexpected type. Expected: "+expectedType+". Received: "+type);
			return false;
		}
		
		return true;
	}
}
