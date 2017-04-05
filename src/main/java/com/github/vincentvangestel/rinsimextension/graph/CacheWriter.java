package com.github.vincentvangestel.rinsimextension.graph;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;

import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

public class CacheWriter {

	public static void main(String[] args) throws IOException {

		Table<Point, Point, List<Point>> pathTable = HashBasedTable.create();
		
		DotGraphIO<MultiAttributeData> dotGraphIO = DotGraphIO.getMultiAttributeGraphIO(Filters.selfCycleFilter());
		ListenableGraph<MultiAttributeData> g = new ListenableGraph<MultiAttributeData>(dotGraphIO.read("files/maps/dot/leuven-large-pruned.dot"));

		int size = g.getNodes().size();
		Point depot = getCenterMostPoint(g);
		// Create Cache
		System.out.println("Working on depot to " + size + " nodes");
		for(Point p2 : g.getNodes()) {
			List<Point> path = Graphs.shortestPath(g, depot, p2, GeomHeuristics.theoreticalTime(70d));
			pathTable.put(depot, p2, path);
		}
		System.out.println("Working on " + size + " nodes to depot");
		for(Point p1 : g.getNodes()) {
			List<Point> path = Graphs.shortestPath(g, p1, depot, GeomHeuristics.theoreticalTime(70d));
			pathTable.put(p1, depot, path);
		}

		
		// Write Object
		try {
			FileOutputStream fileOut =
					new FileOutputStream("files/maps/dot/leuven-large-pruned.cache");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(pathTable);
			out.flush();
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in files/maps/dot/leuven-large-pruned.cache");
		}catch(IOException i) {
			i.printStackTrace();
		}
		
		// Write String
		try {
			FileWriter fileOut =
					new FileWriter("files/maps/dot/leuven-large-pruned.scache");
			BufferedWriter out = new BufferedWriter(fileOut);
			out.write(pathTable.toString());
			out.flush();
			out.close();
			fileOut.close();
			System.out.println("String table is saved in files/maps/dot/leuven-large-pruned.scache");
		}catch(IOException i) {
			i.printStackTrace();
		}
		
	}
	
  private static Point getCenterMostPoint(Graph<?> graph) {
	    final ImmutableList<Point> extremes = Graphs.getExtremes(graph);
	    final Point exactCenter =
	      Point.divide(Point.add(extremes.get(0), extremes.get(1)), 2d);
	    Point center = graph.getRandomNode(new MersenneTwister());
	    double distance = Point.distance(center, exactCenter);

	    for (final Point p : graph.getNodes()) {
	      final double pDistance = Point.distance(p, exactCenter);
	      if (pDistance < distance) {
	        center = p;
	        distance = pDistance;
	      }

	      if (center.equals(exactCenter)) {
	        return center;
	      }
	    }

	    return center;
	  }

}
