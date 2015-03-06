package de.tum.in.www1.jReto.module.remoteP2P;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 *
 * @author jasamer
 */
public class RemoteP2PPacket {
    public static final int START_ADVERTISEMENT = 1;
    public static final int STOP_ADVERTISEMENT = 2;
    public static final int START_BROWSING = 3;
    public static final int STOP_BROWSING = 4;
    public static final int PEER_ADDED = 5;
    public static final int PEER_REMOVED = 6;
    public static final int CONNECTION_REQUEST = 7;

    public final int type;
    public final UUID uuid;

    public RemoteP2PPacket(int type, UUID uuid) {
        this.type = type;
        this.uuid = uuid;
    }
    
    public static RemoteP2PPacket packetFromData(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        if (byteBuffer.remaining() < 20) { System.err.println("Not enough data remaining in packet."); return null; }
        int type = byteBuffer.getInt();
        if (type < 1 || type > 7) { System.err.println("Invalid packet type: "+ type); return null; }
        
        UUID identifier = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
        
        return new RemoteP2PPacket(type, identifier);
    }

    public ByteBuffer serialize() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4+16);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.putInt(this.type);
        byteBuffer.putLong(this.uuid.getMostSignificantBits());
        byteBuffer.putLong(this.uuid.getLeastSignificantBits());

        byteBuffer.rewind();

        return byteBuffer;
    }
}