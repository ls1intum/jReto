package jReto.module.dummy;

import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.module.api.Module;
import jReto.util.RunLoop;

public class DummyModule implements Module {
	private Advertiser advertiser;
	private Browser browser;
	
	public DummyModule(DummyNetworkInterface networkInterface, RunLoop runloop) {		
		this.browser = new DummyBrowser(networkInterface, runloop);
		this.advertiser = new DummyAdvertiser(networkInterface, runloop);
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
	}
}
