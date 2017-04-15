package com.github.vincentvangestel.rinsimextension.experiment;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import javax.measure.unit.SI;

import org.eclipse.swt.widgets.Display;

import com.github.rinde.datgen.pdptw.DatasetGenerator;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RandomBidder;
import com.github.rinde.logistics.pdptw.mas.route.RandomRoutePlanner;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ChangeConnectionSpeedEvent;
import com.github.rinde.rinsim.pdptw.common.PDPDynamicGraphRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.generator.DynamicSpeeds;
import com.github.rinde.rinsim.scenario.generator.DynamicSpeeds.DynamicSpeedGenerator;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class GifMaker {

	public static void main(String[] args) throws IOException {

		//String graphPath = new String("files/maps/dot/leuven-large-pruned.dot");

		//Optional<String> dataset = Optional.of("files/datasets/0.50-20-1.00-0.scen");

		//performExperiment(graphPath, dataset);
		
		trafficLightExample();

	}

	public static void performExperiment(String graphPath, Optional<String> dataset) {
		Scenario s;
		if (dataset.isPresent()) {
			s = ScenarioIO.reader().apply(Paths.get(dataset.get()));
		} else {
			Iterator<Scenario> iS = DatasetGenerator.builder()
					.withGraphSupplier(
							DotGraphIO.getMultiAttributeDataGraphSupplier(graphPath))
					.setNumInstances(1,1)
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

	public static void trafficLightExample() {
		final double TEN_KM_H_IN_M_MILLIS = 0.002777777777777778d;
		final double FIFTEEN_KM_H_IN_M_MILLIS = 0.004166666666666667d;

		final long MINUTE = 60000;
		final long SECOND = 1000;

		final Function<Double, Double> ZERO = new Function<Double, Double>() {

			@Override
			public Double apply(@SuppressWarnings("null") Double t) {
				return 0.00001d;
			}
		};
		final Function<Long, Double> FIFTEEN_KM_H = new Function<Long, Double>() {
			@Override
			public Double apply(@SuppressWarnings("null") Long input) {
				return FIFTEEN_KM_H_IN_M_MILLIS;
			}
		};


		ListenableGraph<MultiAttributeData> graph;
		DynamicSpeeds.Builder builder;

		graph = new ListenableGraph<>(new TableGraph<>());
		builder = DynamicSpeeds.builder()
				.numberOfShockwaves(StochasticSuppliers.constant(1));
		long scenarioLength = 20 * MINUTE;

		Point a, A, B, C, D, E, F, f;
		Point x, y;
		a = new Point(0, 0);
		A = new Point(50, 0);
		B = new Point(100, 0);
		C = new Point(150, 0);
		D = new Point(200, 0);
		E = new Point(250, 0);
		F = new Point(300, 0);
		f = new Point(350, 0);

		x = new Point(350,50);
		y = new Point(350,-50);
		
		Connection<MultiAttributeData> conna, connA, connB, connC, connD, connE,
		connF;
		conna = Connection.create(A, a,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connA = Connection.create(B, A,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connB = Connection.create(C, B,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connC = Connection.create(D, C,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connD = Connection.create(E, D,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connE = Connection.create(F, E,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());
		connF = Connection.create(f, F,
				MultiAttributeData.builder().addAttribute("ts", "50").setLength(50).setMaxSpeed(50).build());

		final Function<Long, Double> twentySomethingKmH =
				new Function<Long, Double>() {
			@Override
			public Double apply(@SuppressWarnings("null") Long input) {
				return 2.093 * TEN_KM_H_IN_M_MILLIS;
			}
		};
		
		graph.addConnection(Connection.create(f, x,
				MultiAttributeData.builder().addAttribute("ts", 50d).setLength(50).setMaxSpeed(50).build()));
		graph.addConnection(Connection.create(f, y,
				MultiAttributeData.builder().addAttribute("ts", 50d).setLength(50).setMaxSpeed(50).build()));

		graph.addConnections(
				Lists.newArrayList(conna, connA, connB, connC, connD, connE, connF));

		final DynamicSpeedGenerator gen = builder.withGraph(graph)
				.startConnections(StochasticSuppliers.constant(connA))
				.shockwaveWaitForRecedeDurations(
						StochasticSuppliers.constant(17 * SECOND))
				.shockwaveBehaviour(StochasticSuppliers.constant(ZERO))
				.shockwaveExpandingSpeed(StochasticSuppliers.constant(FIFTEEN_KM_H))
				.shockwaveRecedingSpeed(StochasticSuppliers.constant(twentySomethingKmH))
				.build();

		final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
				gen.generate(123, scenarioLength));
		
		Scenario s = Scenario.builder()
				.addModel(PDPDynamicGraphRoadModel.builderForDynamicGraphRm(RoadModelBuilders.dynamicGraph(graph).withDistanceUnit(SI.METER)))
			    .addModel(DefaultPDPModel.builder())
			    .addModel(TimeModel.builder().withRealTime().withTimeUnit(SI.MILLI(SI.SECOND)))
			    .addEvents(events)
				.build();

		ExperimentResults results = Experiment.builder()
				.computeLocal()
				.withRandomSeed(123)
				.repeat(1)
				.withThreads(1)
				.addConfiguration(MASConfiguration.builder()
						.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler()).build())
				.addScenario(s)
				.showGui(View.builder()
						.withTitleAppendix("Traffic Light Example")
						.with(GraphRoadModelRenderer.builder()
								.withNodeCircles()
								.withDynamicRelativeSpeedVisualization()
								)
						.withResolution(800, 600))
						.perform();

	}
}
