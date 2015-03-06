package jReto.routing;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.junit.Test;

import de.tum.in.www1.jReto.routing.algorithm.MinimumSteinerTreeApproximation;
import de.tum.in.www1.jReto.routing.algorithm.Tree;

public class SteinerTreeTest {
	
	@Test
	public void testTrivial() {
		DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		
		addSymmetricEdge(graph, 1, 2, 10);
		addSymmetricEdge(graph, 2, 3, 10);
		
		Tree<Integer> tree = MinimumSteinerTreeApproximation.approximateSteinerTree(graph, 1, new HashSet<>(Arrays.asList(1,2,3)));
		
		Tree<Integer> expectedResult = 
			new Tree<>(1, 
				new Tree<>(2, 
					new Tree<>(3)
				)
			);
		
		assertTrue("Did not get expected result.", tree.equals(expectedResult));
	}
	
	@Test
	public void testSingleSteinerVertex() {
		DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		graph.addVertex(4);
		
		addSymmetricEdge(graph, 1, 2, 10);
		addSymmetricEdge(graph, 2, 3, 10);
		addSymmetricEdge(graph, 3, 1, 10);
		
		addSymmetricEdge(graph, 1, 4, 1);
		addSymmetricEdge(graph, 2, 4, 1);
		addSymmetricEdge(graph, 3, 4, 1);
		
		Tree<Integer> tree = MinimumSteinerTreeApproximation.approximateSteinerTree(graph, 1, new HashSet<>(Arrays.asList(1,2,3)));
		
		Tree<Integer> expectedResult =
			new Tree<>(1, 
				new Tree<>(4, 
					new Tree<>(2), 
					new Tree<>(3)
				)
			);
		
		assertTrue("Did not get expected result.", tree.equals(expectedResult));
	}
	
	@Test
	public void testTwoClustersBroadcast() {
		DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		
		graph.addVertex(4);
		graph.addVertex(5);
		graph.addVertex(6);
		
		/**
		 * Imagine two groups, 1,2,3 and 4,5,6, each internally connected via wifi, but 1,2, and 5 have internet access in addition.
		 * */
		addSymmetricEdge(graph, 1, 2, 1);
		addSymmetricEdge(graph, 2, 3, 2);
		addSymmetricEdge(graph, 3, 1, 1);
		
		addSymmetricEdge(graph, 4, 5, 1);
		addSymmetricEdge(graph, 5, 6, 1);
		addSymmetricEdge(graph, 6, 4, 2);
		
		addSymmetricEdge(graph, 1, 5, 10);
		addSymmetricEdge(graph, 2, 5, 11);
		
		Tree<Integer> tree = MinimumSteinerTreeApproximation.approximateSteinerTree(graph, 1, new HashSet<>(Arrays.asList(1,2,3,4,5,6)));

		Tree<Integer> expectedResult =
			new Tree<>(1, 
				new Tree<>(2),
				new Tree<>(3),
				new Tree<>(5,
					new Tree<>(4), 
					new Tree<>(6)
				)
			);
		
		assertTrue("Did not get expected result.", tree.equals(expectedResult));
	}
	
	@Test
	public void testTwoClustersMulticast() {
		DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
		
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		
		graph.addVertex(4);
		graph.addVertex(5);
		graph.addVertex(6);
		
		/**
		 * Imagine two groups, 1,2,3 and 4,5,6, each internally connected via wifi, but 1,2, and 5 have internet access in addition.
		 * */
		addSymmetricEdge(graph, 1, 2, 1);
		addSymmetricEdge(graph, 2, 3, 2);
		addSymmetricEdge(graph, 3, 1, 1);
		
		addSymmetricEdge(graph, 4, 5, 1);
		addSymmetricEdge(graph, 5, 6, 1);
		addSymmetricEdge(graph, 6, 4, 2);
		
		addSymmetricEdge(graph, 1, 5, 10);
		addSymmetricEdge(graph, 2, 5, 11);
		
		Tree<Integer> tree = MinimumSteinerTreeApproximation.approximateSteinerTree(graph, 1, new HashSet<>(Arrays.asList(1,5,6)));

		Tree<Integer> expectedResult =
			new Tree<>(1,
				new Tree<>(5,
					new Tree<>(6)
				)
			);
		
		assertTrue("Did not get expected result.", tree.equals(expectedResult));
	}
	
	static <V, E> void addSymmetricEdge(DirectedWeightedPseudograph<V, E> graph, V start, V destination, double weight) {
		graph.setEdgeWeight(graph.addEdge(start, destination), weight);
		graph.setEdgeWeight(graph.addEdge(destination, start), weight);
	}
}
