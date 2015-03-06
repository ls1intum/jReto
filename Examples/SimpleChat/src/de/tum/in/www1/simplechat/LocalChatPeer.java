package de.tum.in.www1.simplechat;

import de.tum.in.www1.jReto.LocalPeer;
import de.tum.in.www1.jReto.RemotePeer;
//import de.tum.in.www1.jReto.module.remoteP2P.RemoteP2PModule;
import de.tum.in.www1.jReto.module.wlan.WlanModule;

//import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/*
 * The LocalChatPeer advertises itself in the network, and creates RemoteChatPeers when other peers are found.
 * 
 * A note on lambdas in Java 8 (since this might be new to some but is very helpful here):
 * 
 * When you require an instance of an object implementing an interface with a single method, you can use a lambda to abbreviate this. For example, instead of:
 * 
 * someMethod(new SomeInterface() {
 *     @Override
 *     public ReturnType someMethod(Parameter parameter) {
 *         System.out.println(parameter);
 *         return someObjectOfReturnType;
 *     }
 * });
 * 
 * 
 * Write:
 * 
 * someMethod(parameter -> {
 *     System.out.println(parameter);
 *     return someObjectOfReturnType;
 * });
 * 
 * The even shorter version (when you have only one expression in the lambda):
 * 
 * someMethod(parameter -> someExpressionOfReturnType);
 * 
 * */

public class LocalChatPeer {
	/*
	 * 1.0. Field setup
	 * Replace Object with the appropriate class.
	 * */
	private LocalPeer localPeer;
	/*
	 * Stores a map from the reto class for remote peers to RemoteChatPeers. Replace Object with the corresponding class.
	 * */
	private Map<RemotePeer, ChatRoom> chatPeers = new HashMap<>();

	private String displayName;
	private SimpleChatUI chatUI;
		
	public LocalChatPeer(SimpleChatUI chatUI, Executor mainThreadExecutor) {
		this.chatUI = chatUI;
		
		try {
			this.initializeLocalPeer(mainThreadExecutor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initializeLocalPeer(Executor executor) throws Exception {
        /**
        * Create a local peer with a WlanModule. To use the RemoteP2PModule, the RemoteP2P server needs to be deployed locally.
        */
		//RemoteP2PModule remoteModule = new RemoteP2PModule(new URI("ws://localhost:8080/"));
		WlanModule wlanModule = new WlanModule("SimpleP2PChat");
		this.localPeer = new LocalPeer(Arrays.asList(wlanModule), executor);
	}
	
    /**
    * Starts the local peer. 
    * When a peer is discovered, a ChatRoom with that peer is created, when one is lost, the corresponding ChatRoom is removed.
    */
	public void start(String displayName) {
		this.displayName = displayName;
		
		this.localPeer.start(
			peer -> createChatPeer(peer), 
			peer -> removeChatPeer(peer)
		);
	}
	
	public void createChatPeer(RemotePeer peer) {
		if (this.chatPeers.get(peer) != null) {
			System.err.println("We already have a chat peer for this peer!");
			return;
		}
		
		ChatRoom chatPeer = new ChatRoom(peer, this.displayName, this.chatUI);
		this.chatPeers.put(peer, chatPeer);
	}
	public void removeChatPeer(RemotePeer peer) {
		this.chatUI.removeChatPeer(this.chatPeers.get(peer));
		this.chatPeers.remove(peer);
	}
}
