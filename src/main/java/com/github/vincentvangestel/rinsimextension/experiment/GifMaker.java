package com.github.vincentvangestel.rinsimextension.experiment;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import com.github.rinde.datgen.pdptw.DatasetGenerator;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RandomBidder;
import com.github.rinde.logistics.pdptw.mas.route.RandomRoutePlanner;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ChangeConnectionSpeedEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.common.base.Optional;

public class GifMaker {

	public static void main(String[] args) throws IOException {
		
		String graphPath = new String("files/maps/dot/leuven-large-pruned.dot");
		
		Optional<String> dataset = Optional.of("files/datasets/0.50-20-1.00-0.scen");

		performExperiment(graphPath, dataset);

	}

	public static void performExperiment(String graphPath, Optional<String> dataset) {
		Scenario s;
		if (dataset.isPresent()) {
			s = ScenarioIO.reader().apply(Paths.get(dataset.get()));
		} else {
			Iterator<Scenario> iS = DatasetGenerator.builder()
					.withGraphSupplier(
					DotGraphIO.getMultiAttributeDataGraphSupplier(graphPath))
					.setNumInstances(1)
					.setDatasetDir("files/datasets/").build().generate();
			s = iS.next();

		}

		// Simulated Time for testing purposes
		s = ScenarioConverters.toSimulatedtime().apply(s);
		
//		final ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance(70);
//	    final long rpMs = 1000L;
//	    final long bMs = 10L;
//	    final BidFunction bf = BidFunctions.BALANCED_HIGH;
//	    final String masSolverName =
//	    	      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
//	    
//	    final OptaplannerSolvers.Builder opFfdFactory =
//	    	      OptaplannerSolvers.builder()
//	    	      //.withCheapestInsertionSolver()
//	    	      //.withFirstFitDecreasingWithTabuSolver()
//	    	      .withSolverXmlResource(
//	    	        "com/github/rinde/jaamas16/jaamas-solver.xml")
//	    	      .withUnimprovedMsLimit(rpMs)
//	    	      .withName(masSolverName)
//	    	      .withObjectiveFunction(objFunc);
		
		ExperimentResults results = Experiment.builder()
			      .computeLocal()
			      .withRandomSeed(123)
			      .withThreads(1)
			      .repeat(1)
			      //.withWarmup(30000)
			      //.addResultListener(new CommandLineProgress(System.out))
//			      .addResultListener(new VanLonHolvoetResultWriter(new File("files/results/result"), (Gendreau06ObjectiveFunction)objFunc))
				.addScenario(s).addConfiguration(MASConfiguration.pdptwBuilder()
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
				// .addEventHandler(AddVehicleEvent.class,
				// CustomVehicleHandler.INSTANCE)
				.addEventHandler(AddVehicleEvent.class,
						DefaultTruckFactory.builder()
						.setCommunicator(RandomBidder.supplier())
						.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
						.setRoutePlanner(RandomRoutePlanner.supplier())
						//.setRoutePlanner(SolverRoutePlanner.supplier(OptaplannerSolvers.builder()
//						.setRoutePlanner(RtSolverRoutePlanner.supplier(OptaplannerSolvers.builder()
								//.withCheapestInsertionSolver()
//								.withSolverXmlResource("com/github/rinde/jaamas16/jaamas-solver.xml")
//								.withName(masSolverName)
								//.withFirstFitDecreasingWithTabuSolver()
//								.withUnimprovedMsLimit(rpMs)
								//.buildSolverSupplier()))
//								.buildRealtimeSolverSupplier()))
						//.setRoutePlanner(GotoClosestRoutePlanner.supplier())
//		                  .setCommunicator(RtSolverBidder.realtimeBuilder(objFunc,
//	                    opFfdFactory
	                    //.withSolverKey(masSolverName)
//	                      .withUnimprovedMsLimit(bMs)
//	                      .buildRealtimeSolverSupplier())
//	                		  .withBidFunction(bf))
//						//.setCommunicator(RandomBidder.supplier())
//						.setLazyComputation(false)
//						.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
						 .build())
				.addModel(AuctionCommModel.builder(DoubleBid.class)
						.withStopCondition(AuctionStopConditions.and(AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
								AuctionStopConditions.<DoubleBid>or(AuctionStopConditions.<DoubleBid>allBidders(),
										AuctionStopConditions.<DoubleBid>maxAuctionDuration(5000))))
						.withMaxAuctionDuration(30 * 60 * 1000L))
						//.withMaxAuctionDuration(5000L))
				.addModel(SolverModel.builder())
				//.addModel(RtSolverModel.builder())
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.build())
				.showGui(View.builder()
						.with(GraphRoadModelRenderer.builder()
								//.withStaticRelativeSpeedVisualization()
								.withDynamicRelativeSpeedVisualization()
								)
							.withResolution(1280, 1024)
							.withAutoClose())
				.perform();
		
	}
}
