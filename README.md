jReto 1.0
========
Reto: P2P Framework for realtime collaboration in Java.

Notice: There is also a Swift version for the use of Reto in iOS or OS X applications: [sReto](https://github.com/ls1intum/sReto)

About
-----

Reto is an extensible P2P networking framework implemented in Swift and Java 8. It offers the same APIs in both languages.

The most important features of the framework are:

  - Peer discovery
  - Establishing connections between peers
  - Performing cancellable data transfers
  - Offers support for routed connections (i.e., peers that can not directly communicate can still be discovered and use other peers to forward data)

Reto is designed to be easily extensible to use other networking technologies by implementing Reto "Module"s; by default, it comes with two modules:

  - WlanModule: Enables peer discovery and connectivity in local area networks by using Bonjour for discovery and standard TCP/IP connections for data transfers
  - RemoteP2PModule: Uses an online server to facilitate communication between peers. This module enables peers to discover each other and communicate over the Internet; however, a RemoteP2P server is required. 



Installation
------------
Reto is provided as an Maven/Eclipse project.

Note that Reto uses Java 8 features, so make sure that you have the correct JDK installed, and use a version of Eclipse that includes Maven and supports Java 8.

There are two ways to include Reto. The simples way is to build a jar and include it in your project:

 1. Build Reto using "mvn package" in the command line (e.g. in Eclipse, go to your project's properties -> Java Build Path -> Libraries)
 2. Add the jReto-jar-with-dependencies.jar to your project.
 
 Alternatively, you can use Maven to declare the depenency on Reto.
 
  1. Since Reto is not available from one of the public maven repositories, it needs to be installed locally using "mvn install".
  2. Add the dependency to your .pom file (groupId: de.tum.in.www1.jReto, artifactId: jReto, version: 1.3)
 
Usage
-----

** Starting Discovery/Advertisement **

Advertisement and discovery is managed by the `LocalPeer` class. A `LocalPeer` requires one or more Reto Modules to function. In this example, we will use the `WlanModule`.

    // 1. Create the WlanModule
    WlanModule wlanModule = new WlanModule("ExampleType");
    // 2. Create the LocalPeer
    LocalPeer localPeer = new LocalPeer(Arrays.asList(WlanModule), Executors.newSingleThreadExecutor());
    // 3. Starting the LocalPeer
    localPeer.start(
        discoveredPeer -> System.out.println("Discovered peer: "+discoveredPeer), 
        removedPeer -> System.out.println("Removed peer: "+removedPeer),
        (peer, incomingConnection) -> System.out.println("Received incoming connection: "+incomingConnection+" from peer: "+peer)
    );

Any two applications that use the same type parameter for the WlanModule will discover each other in a local area network. Therefore, you should choose a unique type parameter.

In Swift and Java, a dispatch queue is passed to the LocalPeer; any networking operations will be executed using this queue. Furthermore, all callbacks occur on this queue, too.

The same principle is used in Java; here, an Executor is used instead of a dispatch queue. All callbacks are dispatched using this Executor's thread.

In many cases, it is ok to use the main dispatch queue / an Executor that runs on the GUI thread, since Reto will not perform any blocking operations on the queue. If a lot of data is being sent and processed, it may be a good idea to move these operations to a background thread, though.

When starting the `LocalPeer`, three closures are passed as parameters.

  - The onPeerDiscovered closure is called whenever a new peer was discovered.
  - The onPeerRemoved closure is called whenever a peer was lost
  - The onIncomingConnection closure is called whenever a `RemotePeer` established a connection with the `LocalPeer`.

The first closure gives you access to `RemotePeer` objects, which can be used to establish connections with those peers.

** Establishing Connections and Sending Data **

    // 1. Establishing a connection
    Connection connection = [someRemotePeer connect];
    // 2. Registering a callback the onClose event
    connection.setOnClose(connection -> System.out.println("Connection closed."));
    // 3. Receiving data
    connection.setOnData((connection, data) -> System.out.println(Received data!"));
    // 4. Sending data
    connection.send(someData);

A `Connection` can be established by simply calling the `connect` method on a `RemotePeer`. 

It allows you to register a number of callbacks for various events, for example, `onClose`, which is called when the connection closes. Most of these callbacks are optional, however, you must set the `onTransfer` or `onData` callbacks if you wish to receive data using a connection.

In this example, the `onData` callback is set, which is called when data was received. For more control over data transfers, use the `onTransfer` callback.


** Data Transfers ** 

While the above techniques can be used to send data, you may want access about more information. The `Transfer` class gives access to more information and tools. The following just gives a short example of how using these features might look; however, the `Transfer` class offers more methods and features. Check the class documentation to learn more.

    // 1. Configuring a connection to receive transfers example
    someConnection.setOnTransfer(
        (connection, transfer) -> 
        // 2. Configuring a transfer to let you handle data as it is received, instead of letting the transfer buffer all data
        transfer.setOnPartialData((transfer, data) -> System.out.println("Received a chunk of data!"));
        // 3. Registering for progress updates
        transfer.setOnProgress(transfer -> System.out.println("Current progress: "+transfer.progress+" of "+transfer.length));
    );
   
    // 4. Sending a transfer example
    let transfer = someConnection.send(someData.length, range -> somehowProvideDataForRange(range));
    // 5. Registering for progress updates
    transfer.setOnProgress(transfer -> System.out.println("Current progress: "+transfer.progress+" of "+transfer.length));
