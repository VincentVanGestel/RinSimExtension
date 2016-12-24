package com.github.vincentvangestel.rinsimextension.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.rinde.datgen.pdptw.DatasetGenerator;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;

public class GraphPruner {

	/**
	 * Prunes all nodes, unreachable from the center most point from the graph. The supplied graph is modified (no copy is taken)
	 * @param g The graph to be pruned
	 * @return The modified graph
	 * @deprecated Use OsmConverter.withPruner(new CenterPruner()) instead
	 */
	@Deprecated
	public static Graph<MultiAttributeData> pruneFromCenter(Graph<MultiAttributeData> g) {
		Point center = DatasetGenerator.getCenterMostPoint(g);
		
		List<Point> toRemove = new ArrayList<>();
		
		for(Point node : g.getNodes()) {
			try {
				Graphs.shortestPathEuclideanDistance(g, center, node);
				Graphs.shortestPathEuclideanDistance(g, node, center);
			} catch(PathNotFoundException e) {
				// Unreachable
				toRemove.add(node);
			}
		}
		
		for(Point node : toRemove) {
			g.removeNode(node);
		}
		
		Logger.getGlobal().info("GraphPruner pruned " + toRemove.size() + " nodes from the graph");
		return g;
	}
	
	/**
	 * Either prune all roads present in the given list, or prune everything else
	 * @param g The graph to be pruned
	 * @param roads The roads to prune or keep
	 * @param remove Whether to remove (true) or keep (false) the given roads
	 * @return The pruned graph
	 */
	public static Graph<MultiAttributeData> pruneRoads(Graph<MultiAttributeData> g, List<String> roads, boolean remove) {
		List<Connection<MultiAttributeData>> toRemove = new ArrayList<>();
		for(Connection<MultiAttributeData> conn : g.getConnections()) {
			if(!(roads.contains(conn.data().get().getAttributes().get("n")) ^ remove)) {
				toRemove.add(conn);
			}
		}
		
		for(Connection<MultiAttributeData> conn : toRemove) {
			g.removeConnection(conn.from(), conn.to());
		}
		
		return g;
	}
}
