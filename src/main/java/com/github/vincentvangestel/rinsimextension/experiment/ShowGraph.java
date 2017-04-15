package com.github.vincentvangestel.rinsimextension.experiment;

import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.google.common.collect.ImmutableList;

public class ShowGraph {

	public static void main(String[] args) throws IOException {

		Graph<MultiAttributeData> graph = DotGraphIO.getMultiAttributeGraphIO().read("files/maps/dot/leuven-large-simplified-50.dot");
		
		Scenario s = Scenario.builder()
				.addModel(RoadModelBuilders.staticGraph(graph))
			    .addModel(DefaultPDPModel.builder())
				.build();

		ExperimentResults results = Experiment.builder()
				.computeLocal()
				.withRandomSeed(123)
				.repeat(1)
				.withThreads(1)
				.addConfiguration(MASConfiguration.builder().build())
				.addScenario(s)
				.showGui(View.builder()
						.withTitleAppendix("Heavy2")
						.with(GraphRoadModelRenderer.builder()
								//.withStaticRelativeSpeedVisualization()
								//.withDynamicRelativeSpeedVisualization()
								)
						.withResolution(1280, 1024))
						.perform();

	}
	
	  /**
	   * Returns the point closest to the exact center of the area spanned by the
	   * graph.
	   * @param graph The graph.
	   * @return The point of the graph closest to the exact center of the area
	   *         spanned by the graph.
	   */
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
