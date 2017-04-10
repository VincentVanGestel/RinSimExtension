package com.github.vincentvangestel.rinsimextension.graph;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.christofluyten.data.NextHop;
import com.github.christofluyten.data.RoutingTable;
import com.github.christofluyten.data.RoutingTables;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;

public class CacheWriter {

	public static void main(String[] args) throws IOException, ClassNotFoundException {

//		FileInputStream fileIn = new FileInputStream("files/maps/dot/leuven-large-simplified2.cache");
//		ObjectInputStream in = new ObjectInputStream(fileIn);
//		RoutingTable cache = (RoutingTable) in.readObject();
//		in.close();
//		fileIn.close();
		
		RoutingTable table = RoutingTables.createDefaultTable();
		
		DotGraphIO<MultiAttributeData> dotGraphIO = DotGraphIO.getMultiAttributeGraphIO(Filters.selfCycleFilter());
		ListenableGraph<MultiAttributeData> g = new ListenableGraph<MultiAttributeData>(dotGraphIO.read("files/maps/dot/leuven-large-simplified.dot"));

//		for(Point p1 : g.getNodes()) {
//			for(Point p2 : g.getNodes()) {
//				if(!cache.containsRoute(p1, p2)) {
//					System.out.println("Cache lacks route from " + p1 + " to " + p2);
//					cache = addPath(cache, g, p1, p2, SI.MILLI(SI.SECOND), SI.KILOMETER, Measure.valueOf(70d, NonSI.KILOMETERS_PER_HOUR), GeomHeuristics.theoreticalTime(70d));
//				}
//			}
//		}
		
		int size = g.getNodes().size();
		int index = 1;
		// Create Cache
		for(Point node : g.getNodes()) {
			System.out.println("Working on node " + index + " of " + size);
			for(Point target : g.getNodes()) {
				if(!table.containsRoute(node, target)) {
					table = addPath(table, g, node, target, SI.MILLI(SI.SECOND), SI.KILOMETER, Measure.valueOf(70d, NonSI.KILOMETERS_PER_HOUR), GeomHeuristics.theoreticalTime(70d));
				}
			}
			index++;
		}
		
		// Write Object
		try {
			FileOutputStream fileOut =
					new FileOutputStream("files/maps/dot/leuven-large-simplified.cache");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(table);
			out.flush();
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in files/maps/dot/leuven-large-simplified.cache");
		}catch(IOException i) {
			i.printStackTrace();
		}
	}
	
	private static RoutingTable addPath(RoutingTable table, Graph graph, Point from, Point to, Unit<Duration> timeUnit, Unit<Length> distanceUnit, Measure<Double, Velocity> speed,
			GeomHeuristic heuristic) {
		List<Point> path = Graphs.shortestPath(graph, from, to, heuristic);
		Point movingFrom = from;
		checkArgument(path.size() > 1, "A path between two points must at least be of length two");
		for(Point hop : path.subList(1, path.size())) {
			if(table.containsRoute(movingFrom, to)) {
				break;
			}
			table.addHop(movingFrom, to, new NextHop(hop,
					heuristic.calculateTravelTime(graph, movingFrom, hop, distanceUnit, speed, timeUnit)));
			movingFrom = hop;
		}
		return table;
	}
}
