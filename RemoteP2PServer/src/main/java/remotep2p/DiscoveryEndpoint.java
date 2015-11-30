/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package remotep2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author jasamer
 */
@ServerEndpoint("/discovery")
public class DiscoveryEndpoint {
    /// Logger to write to server logs.
    private static final Logger LOGGER = Logger.getLogger(DiscoveryEndpoint.class.getName());
    
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf) throws IOException {
        LOGGER.log(Level.INFO, "Discovery connection opened {0}", conf);
    }
    
    @OnMessage
    public void binaryMessage(byte[] data, Session session) throws IOException {
        RemoteP2PPacket packet = RemoteP2PPacket.packetFromData(data);
        if (packet == null) {
            LOGGER.log(Level.WARNING, "Discovery connection received invalid data.");
            return;
        }
        
        switch (packet.type) {
            case RemoteP2PPacket.START_ADVERTISEMENT: 
                ConnectionManager.getInstance().startAdvertisingPeer(packet.uuid, session);
                break;
            case RemoteP2PPacket.STOP_ADVERTISEMENT:
                ConnectionManager.getInstance().stopAdvertisingPeer(packet.uuid, session);
                break;
            case RemoteP2PPacket.START_BROWSING: 
                ConnectionManager.getInstance().startSendingDiscoveryUpdates(session);
                break;
            case RemoteP2PPacket.STOP_BROWSING: 
                ConnectionManager.getInstance().stopSendingDiscoveryUpdates(session);
                break;
            default: 
                LOGGER.log(Level.WARNING, "Discovery connection received invalid packet. You may only send START_ADVERTISEMENT and STOP_ADVERTISEMENT packets to the server.");
        }
    }
    
    /**
     * Called when a RemotePeer closes the connection to the server.
     * Informs other RemotePeers about the peer loss.
     * @param session WebSocket session of the RemotePeer
     * @param closeReason Reason why a web socket has been closed.
     * @throws java.io.IOException
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws IOException {
        LOGGER.log(Level.INFO, "Discovery connection closed {0}", closeReason);
        
        ConnectionManager.getInstance().removeDiscoveryConnection(session);
    }

    @OnError
    public void onError(Session session, Throwable t) throws IOException {
        LOGGER.log(Level.INFO, "Error occured {0}", t);

        ConnectionManager.getInstance().removeDiscoveryConnection(session);
    }
}
