package jReto.routing;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable;
import de.tum.in.www1.jReto.routing.algorithm.LinkStateRoutingTable.Change.NowReachableInformation;

public class RoutingTableTests {

	@Test
	public void testRoutingTableWithNeighbor() {
		LinkStateRoutingTable<String> routingTable = new LinkStateRoutingTable<String>("Local");
		LinkStateRoutingTable.Change<String> change = routingTable.getRoutingTableChangeForNeighborUpdate("A", 10);
		
		assertTrue("New neighbor not reachable.", change.nowReachable.size() == 1);
		assertTrue("Node unreachable", change.nowUnreachable.size() == 0);
		assertTrue("Node unreachable", change.routeChanged.size() == 0);
		
		assertTrue("Incorrect node reachable", change.nowReachable.get(0).nextHop.equals("A"));
		assertTrue("Incorrect cost", change.nowReachable.get(0).cost == 10);
	}
	
	@Test
	public void testRoutingTableWithIneffectualLinkStateInformation() {
		LinkStateRoutingTable<String> routingTable = new LinkStateRoutingTable<String>("Local");
		routingTable.getRoutingTableChangeForNeighborUpdate("A", 10);
		LinkStateRoutingTable.Change<String> change = 
				routingTable.getRoutingTableChangeForLinkStateInformationUpdate("B", 
						Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("C",  1), 
								new LinkStateRoutingTable.NeighborInformation<String>("D", 1)
						)
				);
		
		assertTrue("Change empty", change.isEmpty());
	}

	@Test
	public void testRoutingTableWithEffectualLinkStateInformation() {
		LinkStateRoutingTable<String> routingTable = new LinkStateRoutingTable<String>("Local");
		routingTable.getRoutingTableChangeForNeighborUpdate("A", 10);
		
		routingTable.getRoutingTableChangeForLinkStateInformationUpdate("B", 
				Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("C",  1), 
						new LinkStateRoutingTable.NeighborInformation<String>("D", 1)
				)
		);
		
		LinkStateRoutingTable.Change<String> change = routingTable.getRoutingTableChangeForLinkStateInformationUpdate("A", Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("B", 1)));
		
		assertTrue("New nodes reachable", change.nowReachable.size() == 3);
		assertTrue("No other changes", change.nowUnreachable.size() == 0 && change.routeChanged.size() == 0);
		
		Map<String, String> nextHops = new HashMap<>();
		Map<String, Double> costs = new HashMap<>();
		
		for (NowReachableInformation<String> nowReachable : change.nowReachable) {
			nextHops.put(nowReachable.node, nowReachable.nextHop);
			costs.put(nowReachable.node, nowReachable.cost);
		}
		
		assertTrue("B now reachable information correct", nextHops.get("B").equals("A") && costs.get("B") == 11);
		assertTrue("C now reachable information correct", nextHops.get("C").equals("A") && costs.get("C") == 12);
		assertTrue("D now reachable information correct", nextHops.get("D").equals("A") && costs.get("D") == 12);
	}
	
	@Test
	public void testRoutingTableForRouteChanges() {
		LinkStateRoutingTable<String> routingTable = new LinkStateRoutingTable<String>("Local");
		routingTable.getRoutingTableChangeForNeighborUpdate("A", 10);
		
		routingTable.getRoutingTableChangeForLinkStateInformationUpdate("B", 
				Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("C",  1), 
						new LinkStateRoutingTable.NeighborInformation<String>("D", 1)
				)
		);
		
		routingTable.getRoutingTableChangeForLinkStateInformationUpdate("A", Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("B", 1)));
		LinkStateRoutingTable.Change<String> change = routingTable.getRoutingTableChangeForNeighborUpdate("A", 5);
		
		assertTrue("routes changed", change.routeChanged.size() == 4);
		assertTrue("reachablitiy changed", change.nowReachable.size() == 0 && change.nowUnreachable.size() == 0);
	}
	
	@Test
	public void testRoutingTableUnreachability() {
		LinkStateRoutingTable<String> routingTable = new LinkStateRoutingTable<String>("Local");
		routingTable.getRoutingTableChangeForNeighborUpdate("A", 10);
		routingTable.getRoutingTableChangeForLinkStateInformationUpdate("A", Arrays.asList(new LinkStateRoutingTable.NeighborInformation<String>("B", 1)));
		LinkStateRoutingTable.Change<String> change = routingTable.getRoutingTableChangeForNeighborRemoval("A");
		
		assertTrue("nodes got unreachable", change.nowUnreachable.size() == 2);
		assertTrue("no other changes", change.nowReachable.size() == 0 && change.routeChanged.size() ==0 );
	}
}
