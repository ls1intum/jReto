package de.tum.in.www1.jReto.module.wlan;

import java.io.IOException;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.module.api.Module;
import de.tum.in.www1.jReto.niotools.Dispatcher;

/**
 * Using a WlanModule with the LocalPeer allows it to discover and connect with other peers on the local network using Bonjour.
 * 
 * If you wish to use it, all you need to do is construct an instance and pass it to the LocalPeer either in the constructor or using the addModule method.
 * */
public class WlanModule implements Module {
	private WlanAdvertiser advertiser;
	private WlanBrowser browser;
	private String networkType;
	
    /**
    * Constructs a new WlanModule that can be used with a LocalPeer. 
    * @param type: Any alphanumeric string used to identify the type of application in the network. Can be anything, but should be unique for the application.
    * @param dispatchQueue: The dispatch queue used with this module. Use the same one as you used with the LocalPeer.
    */
	public WlanModule(String networkType) throws IOException {
		this.networkType = networkType;
	}
	
	@Override
	public Advertiser getAdvertiser() {
		return this.advertiser;
	}

	@Override
	public Browser getBrowser() {
		return this.browser;
	}
	
	@Override
	public void setExecutor(Executor executor) {
		try {
			Dispatcher dispatcher = new Dispatcher(executor);
			this.advertiser = new WlanAdvertiser(dispatcher, networkType);
			this.browser = new WlanBrowser(executor, dispatcher, networkType);
			
			dispatcher.start();
		} catch (IOException e) {
			System.err.println("Error occured when instantiating dispatcher.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
