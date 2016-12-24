package com.github.vincentvangestel.rinsimextension.pdptw_dataset_gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.datgen.pdptw.DatasetGenerator;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.vincentvangestel.rinsimextension.experiment.ExperimentRunner;

public class GeneralTests {
	
	@Before
	public void setUp() {
		
	}
	
	@Test
	public void actualConnectionTest() throws IOException {
		Graph<MultiAttributeData> graph = ExperimentRunner.returnGraph("files/maps/leuven-large.dot");
		
		Point depot = DatasetGenerator.getCenterMostPoint(graph);
		
		Point aPoint = graph.getRandomNode(new MersenneTwister(123));
		
		Iterator<Point> path = Graphs.shortestPathEuclideanDistance(graph, depot, aPoint).iterator();
		
		Point from = path.next();
		Point to = path.next();
		assertEquals(2, Graphs.shortestPathEuclideanDistance(graph, from, to).size());
		assertTrue(graph.hasConnection(from, to));
		
		Point prev = path.next();
		while(path.hasNext()) {
			Point cur = path.next();
			assertTrue(graph.hasConnection(prev, cur));
			prev = cur;
		}
		
	}
	
}
