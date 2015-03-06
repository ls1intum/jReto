package de.tum.in.www1.jReto.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executor;

import de.tum.in.www1.jReto.module.api.Address;
import de.tum.in.www1.jReto.module.api.Advertiser;
import de.tum.in.www1.jReto.module.api.Browser;
import de.tum.in.www1.jReto.module.api.Connection;
import de.tum.in.www1.jReto.module.api.Module;
import de.tum.in.www1.jReto.module.util.CompositeAdvertiser;
import de.tum.in.www1.jReto.module.util.CompositeBrowser;
import de.tum.in.www1.jReto.routing.managed_module.ManagedModule;

/** A DefaultRouter uses Reto Modules to discover other peers. */
public class DefaultRouter extends Router implements Advertiser.Handler, Browser.Handler {
	private final CompositeAdvertiser advertiser;
	private final CompositeBrowser browser;
	private final Collection<ManagedModule> modules;
	
	public DefaultRouter(UUID localIdentifier, Executor executor, Collection<Module> modules, Router.BroadcastDelaySettings linkStateBroadcastDelaySettings) {
		super(localIdentifier, executor, linkStateBroadcastDelaySettings);
		
		HashSet<ManagedModule> managedModules = new HashSet<>();
		for (Module module : modules) {
			managedModules.add(new ManagedModule(module, executor));
		}
		this.modules = managedModules;
		
		HashSet<Advertiser> advertisers = new HashSet<>();
		HashSet<Browser> browsers = new HashSet<>();
		
		for (ManagedModule module : this.modules) {
			advertisers.add(module.getAdvertiser());
			browsers.add(module.getBrowser());
		}
		
		this.advertiser = new CompositeAdvertiser(localIdentifier, advertisers, this);
		this.browser = new CompositeBrowser(browsers, this);
	}
	
	public void addModule(Module module) {
		ManagedModule newModule = new ManagedModule(module, this.getExecutor());
		
		this.advertiser.addAdvertiser(newModule.getAdvertiser());
		this.browser.addBrowser(newModule.getBrowser());
		this.modules.add(newModule);
	}
	public void removeModule(Module module) {
		ArrayList<ManagedModule> removedModules = new ArrayList<>();
		
		for (ManagedModule currentModule : this.modules) {
			if (currentModule.getModule() == module) {
				removedModules.add(currentModule);
			}
		}
		
		for (ManagedModule removedModule : removedModules) {
			this.modules.remove(removedModule);
			
			this.advertiser.removeAdvertiser(removedModule.getAdvertiser());
			this.browser.removeBrowser(removedModule.getBrowser());
		}
	}
	
	public void start() {
		this.advertiser.startAdvertisingWithPeerIdentifier(this.getLocalNodeIdentifier());
		this.browser.startBrowsing();
	}
	public void stop() {
		this.advertiser.stopAdvertising();
		this.browser.stopBrowsing();
		this.disconnectAll();
	}
	
	@Override
	public void onBrowsingStarted(Browser browser) {}
	@Override
	public void onBrowsingStopped(Browser browser, Object error) {}
	@Override
	public void onAddressDiscovered(Browser browser, Address address, UUID identifier) {
		this.addAddress(identifier, address);
	}
	@Override
	public void onAddressRemoved(Browser browser, Address address, UUID identifier) {
		this.removeAddress(identifier, address);
	}

	@Override
	public void onAdvertisingStarted(Advertiser advertiser) {}
	@Override
	public void onAdvertisingStopped(Advertiser advertiser) {}
	@Override
	public void onConnection(Advertiser advertiser, Connection connection) {
		this.handleDirectConnection(connection);
	}
}
