package de.tum.in.www1.jReto.routing.managed_module;

import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.module.api.Module;
import de.tum.in.www1.jReto.util.Timer;

/**
 * A ManagedModule wraps a Modules browser and advertiser in their Managed classes, which automatically restart them if starting fails.
 * */
public class ManagedModule implements Module {
	public final static Timer.BackoffTimerSettings DEFAULT_TIMER_SETTINGS = new Timer.BackoffTimerSettings(5, 1.5, 60);

	private final Module module;
	private final ManagedAdvertiser advertiser;
	private final ManagedBrowser browser;
	
	public ManagedModule(Module module, Executor executor) {
		this.module = module;
		this.module.setExecutor(executor);
		this.advertiser = new ManagedAdvertiser(module.getAdvertiser(), executor);
		this.browser = new ManagedBrowser(module.getBrowser(), executor);
	}
	
	public Module getModule() {
		return this.module;
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
		this.module.setExecutor(executor);
	}
}
