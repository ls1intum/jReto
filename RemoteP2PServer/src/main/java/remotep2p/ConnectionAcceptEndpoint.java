/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package remotep2p;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author jasamer
 */
@ServerEndpoint("/connection/accept/{sourcePeer}/{targetPeer}")
public class ConnectionAcceptEndpoint {
    /// Logger to write to server logs.
    private static final Logger LOGGER = Logger.getLogger(DiscoveryEndpoint.class.getName());
    
    private Session acceptingSession;
    private Session requestingSession;
    
    public Session getAcceptingSession() {
        return this.acceptingSession;
    }
    public void setRequestingSession(Session session) {
        this.requestingSession = session;
    }
    
    /**
     * Called when a new RemotePeer opens a connection to the server
     * @param session WebSocket session of the RemotePeer
     * @param conf Contains all the information needed during the handshake process.
     * @param sourcePeer
     * @param targetPeer
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf, @PathParam("sourcePeer") String sourcePeer, @PathParam("targetPeer") String targetPeer) {
        LOGGER.log(Level.INFO, "onOpen called on data connection {0}", conf);
        this.acceptingSession = session;
        ConnectionManager
                .getInstance()
                .acceptConnectionRequest(
                        UUID.fromString(sourcePeer), 
                        UUID.fromString(targetPeer), 
                        this
                );
    }
    
    @OnMessage
    public void binaryMessage(byte[] data, Session session, @PathParam("sourcePeer") String sourcePeer) throws IOException {
        if (this.requestingSession == null) {
            LOGGER.log(Level.WARNING, "Could not send data, the requesting Session was not set yet.");
        }
        
        this.requestingSession
                .getBasicRemote()
                .sendBinary(
                        ByteBuffer
                                .wrap(data)
                                .order(ByteOrder.LITTLE_ENDIAN)
                );
    }
    
     /**
     * Called when a RemotePeer closes the connection to the server.
     * Informs other RemotePeers about the peer loss.
     * @param session WebSocket session of the RemotePeer
     * @param closeReason Reason why a web socket has been closed.
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOGGER.log(Level.INFO, "Data connection closed {0}", closeReason);
        
        try {
            this.requestingSession.close(closeReason);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionAcceptEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable t) {
        LOGGER.log(Level.WARNING, "Error occured {0}", t);

        try {
            this.requestingSession
                    .close(
                            new CloseReason(
                                    CloseReason.CloseCodes.CLOSED_ABNORMALLY, 
                                    "An exception occured: "+t.toString()
                            )
                    );
        } catch (IOException ex) {
            Logger.getLogger(ConnectionAcceptEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
