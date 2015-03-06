package de.tum.in.www1.jReto.module.api;

import java.util.concurrent.Executor;

/**
* A Reto module encapsulates a networking technology that can be passed to a LocalPeer.
* It consists of an Advertiser that advertises peers (i.e. makes the local peer discoverable), and a Browser that finds other peers (i.e. discovers other peers).
*/
public interface Module {
    /** The Module's advertiser */
	Advertiser getAdvertiser();
    /** The Module's browser */
	Browser getBrowser();
    /** 
	* Sets an Executor for this module.
    * It is expected that delegate methods will be called on this Executor.
    */
	void setExecutor(Executor executor);
}
