package remotep2p;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.CloseReason;
import javax.websocket.Session;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jasamer
 */
public class ConnectionManager {
    private static ConnectionManager instance = null;
    protected ConnectionManager() {}
    public static ConnectionManager getInstance() {
        if(instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private static final  Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());

    private final Set<Session> discoveryUpdateSessions = new HashSet<>();
    private final Map<UUID, Session> discoveryConnectionByAdvertisedIdentifier = new HashMap<>();
    private final Map<UUID, Map<UUID, List<ConnectionRequestEndpoint>>> requestConnectionEndpointsBySourceByTarget = new HashMap<>();
 
    void removeDiscoveryConnection(Session session) throws IOException {
        this.stopSendingDiscoveryUpdates(session);
        
        UUID removedUUID = this.uuidForSession(session);
        if (removedUUID != null) {
            this.stopAdvertisingPeer(removedUUID, session);
        }
    }
    
    void startSendingDiscoveryUpdates(Session session) throws IOException {
        this.discoveryUpdateSessions.add(session);
        
        for (Map.Entry<UUID, Session> discoveryConnection : this.discoveryConnectionByAdvertisedIdentifier.entrySet()) {
            session
                    .getBasicRemote()
                    .sendBinary(
                            new RemoteP2PPacket(
                                    RemoteP2PPacket.PEER_ADDED, 
                                    discoveryConnection.getKey()
                            ).serialize()
                    );
        }
    }
    void stopSendingDiscoveryUpdates(Session session) {
        this.discoveryUpdateSessions.remove(session);
    }
    
    void startAdvertisingPeer(UUID peerIdentifier, Session session) throws IOException {
        this.discoveryConnectionByAdvertisedIdentifier.put(peerIdentifier, session);
        
        ByteBuffer discoveryData = new RemoteP2PPacket(RemoteP2PPacket.PEER_ADDED, peerIdentifier).serialize();
        
        for (Session discoveryConnection : this.discoveryUpdateSessions) {
            discoveryConnection.getBasicRemote().sendBinary(discoveryData);
        }
    }
    void stopAdvertisingPeer(UUID peerIdentifier, Session session) throws IOException {
        if (!this.discoveryConnectionByAdvertisedIdentifier.containsKey(peerIdentifier)) {
            LOGGER.log(Level.WARNING, "Attempted to remove unknown Session.");
            return;
        }
        
        this.discoveryConnectionByAdvertisedIdentifier.remove(peerIdentifier);
        
        ByteBuffer removalData = new RemoteP2PPacket(RemoteP2PPacket.PEER_REMOVED, peerIdentifier).serialize();

        for (Session discoveryConnection : this.discoveryUpdateSessions) {
            discoveryConnection.getBasicRemote().sendBinary(removalData);
        }
    }
    
    private UUID uuidForSession(Session session) {
        for (Map.Entry<UUID, Session> discoveryConnection : this.discoveryConnectionByAdvertisedIdentifier.entrySet()) {
           if (discoveryConnection.getValue() == session) return discoveryConnection.getKey();
        }
        
        return null;
    }
    
    private void warnAndCloseEndpoint(ConnectionAcceptEndpoint acceptEndpoint) {
        LOGGER.log(Level.WARNING, "Trying to accept connection that was not requested.");
        try {
            acceptEndpoint
                    .getAcceptingSession()
                    .close(
                            new CloseReason(
                                    CloseReason.CloseCodes.CANNOT_ACCEPT, 
                                    "Trying to accept connection that was not requested."
                            )
                    );
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void requestConnection(UUID requestingPeer, UUID acceptingPeer, ConnectionRequestEndpoint connectionRequestEndpoint) throws IOException {        
        Map<UUID, List<ConnectionRequestEndpoint>> requestConnectionEndpointsByTarget = this.requestConnectionEndpointsBySourceByTarget.get(requestingPeer);
        if (requestConnectionEndpointsByTarget == null) {
            requestConnectionEndpointsByTarget = new HashMap<>();
            this.requestConnectionEndpointsBySourceByTarget.put(requestingPeer, requestConnectionEndpointsByTarget);
        }
        
        List<ConnectionRequestEndpoint> requestConnectionEndpoints = requestConnectionEndpointsByTarget.get(acceptingPeer);
        if (requestConnectionEndpoints == null) {
            requestConnectionEndpoints = new ArrayList<>();
            requestConnectionEndpointsByTarget.put(acceptingPeer, requestConnectionEndpoints);
        }
        
        requestConnectionEndpoints.add(connectionRequestEndpoint);
        this.discoveryConnectionByAdvertisedIdentifier
                .get(acceptingPeer)
                .getBasicRemote()
                .sendBinary(
                        new RemoteP2PPacket(RemoteP2PPacket.CONNECTION_REQUEST, requestingPeer)
                                .serialize()
                );

        LOGGER.log(Level.INFO, "Received connection request from {0} to {1}", new Object[]{requestingPeer, acceptingPeer});
    }
    
    void acceptConnectionRequest(UUID acceptingPeer, UUID requestingPeer, ConnectionAcceptEndpoint acceptEndpoint) {
        Map<UUID, List<ConnectionRequestEndpoint>> requestConnectionEndpointsByTarget = this.requestConnectionEndpointsBySourceByTarget.get(requestingPeer);
        if (requestConnectionEndpointsByTarget == null) {
            this.warnAndCloseEndpoint(acceptEndpoint);
            return;
        }
        
        List<ConnectionRequestEndpoint> requestConnectionEndpoints = requestConnectionEndpointsByTarget.get(acceptingPeer);
        if (requestConnectionEndpoints == null || requestConnectionEndpoints.isEmpty()) {
            this.warnAndCloseEndpoint(acceptEndpoint);
            return;
        }
        
        ConnectionRequestEndpoint requestEndpoint = requestConnectionEndpoints.remove(0);
        
        acceptEndpoint.setRequestingSession(requestEndpoint.getRequestingSession());
        requestEndpoint.setAcceptingSession(acceptEndpoint.getAcceptingSession());
        requestEndpoint.confirmAccepted();
        
        LOGGER.log(Level.INFO, "Established data connection between {0} and {1}.", new Object[]{requestingPeer, acceptingPeer});
    }
}
