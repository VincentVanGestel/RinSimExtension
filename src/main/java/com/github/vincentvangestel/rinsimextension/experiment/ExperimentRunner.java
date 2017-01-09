package com.github.vincentvangestel.rinsimextension.experiment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.datgen.pdptw.DatasetGenerator;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ChangeConnectionSpeedEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.PDPDynamicGraphRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.vincentvangestel.rinsimextension.vehicle.Taxi;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ExperimentRunner {

	
	private static final int NUMBER_OF_SHOCKWAVES = 0;
	private static final Optional<StochasticSupplier<Function<Long, Double>>> SHOCKWAVE_EXPANDING_SPEED = Optional.absent();
	private static final Optional<StochasticSupplier<Function<Long, Double>>> SHOCKWAVE_RECEDING_SPEED = Optional.absent();
	private static final Optional<StochasticSupplier<Function<Double, Double>>> SHOCKWAVE_BEHAVIOUR = Optional.absent();


	/**
	 * Usage: args = [ generate/experiment datasetID ]
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		//args = new String[]{ "g", "no_shockwaves"};
		//args = new String[]{"e", "old"};
		
		if(args.length < 2) {
			throw new IllegalArgumentException("Usage: args = [ g/e datasetID]");
		}
		
		String graphPath = new String("files/maps/dot/leuven-large-pruned.dot");
		
		if(args[0].equalsIgnoreCase("g") || args[0].equalsIgnoreCase("generate")) {
			generateDataset(graphPath, args[1].toLowerCase());
		} else if(args[0].equalsIgnoreCase("e") || args[0].equalsIgnoreCase("experiment")) {
			performExperiment(graphPath, args[1].toLowerCase());
		}
		
		/**
		 * Comment this if you don't want a new graph to be constructed!
		 */
		/**
		 Graph<MultiAttributeData> g = new OsmConverter()
		  .setOutputDir("files/maps/dot")
		  .withOutputName("leuven-large-pruned.dot")
		  .withPruner(new CenterPruner())
		  .convert("files/maps/osm/leuven-large.osm");
		 /**/
		
		
		// readGraph("files/maps/dot/leuven-large-pruned.dot");
		// readGraph("files/maps/brussels-simple.dot");

		//Graph<MultiAttributeData> g = returnGraph(graphPath);
		// Graph<MultiAttributeData> g =
		// returnGraph("files/maps/dot/leuven-large-double-pruned.dot");
		// Graph<MultiAttributeData> g =
		// returnGraph("files/maps/dot/leuven-large-double-pruned-second-center-WRONG.dot");

		// g = new RoundAboutPruner().prune(g);
		// g = new CenterPruner().prune(g);
		// g = GraphPruner.pruneRoads(g, Lists.newArrayList("Bondgenotenlaan"),
		// false);

		// Spanned Area
		// List<Point> extremes = Graphs.getExtremes(g);
		// System.out.println("Spanned Area: " + Math.abs(extremes.get(0).x -
		// extremes.get(1).x) + " by " + Math.abs(extremes.get(0).y -
		// extremes.get(1).y));
		// System.out.println("Diagonal: " + Point.distance(extremes.get(0),
		// extremes.get(1)));

		// readGraph(g);
		// DotGraphIO.getMultiAttributeGraphIO().write(g,
		// "files/maps/dot/leuven-large-double-pruned-second-center-WRONG.dot");

		//Optional<String> dataset = Optional.of("files/datasets/0.50-20-1.00-0.scen");
	}

	public static Graph<MultiAttributeData> returnGraph(String file) throws IOException {
		DotGraphIO<MultiAttributeData> dotGraphIO = DotGraphIO.getMultiAttributeGraphIO(Filters.selfCycleFilter());
		return new ListenableGraph<MultiAttributeData>(dotGraphIO.read(file));
	}

	public static void readGraph(Supplier<Graph<MultiAttributeData>> g) {
		final Simulator simulator = Simulator.builder().addModel(DefaultPDPModel.builder())
				.addModel(PDPDynamicGraphRoadModel.builderForDynamicGraphRm(
						RoadModelBuilders.dynamicGraph(ListenableGraph.supplier(g))
								.withSpeedUnit(NonSI.KILOMETERS_PER_HOUR).withDistanceUnit(SI.KILOMETER))
						.withAllowVehicleDiversion(true))
				.addModel(View.builder().with(GraphRoadModelRenderer.builder().withNodeCircles()
						// .withDirectionArrows()
						)).build();

		simulator.start();
	}

	public static void readGraph(String file) throws IOException {
		Supplier<Graph<MultiAttributeData>> g = DotGraphIO.getMultiAttributeDataGraphSupplier(file);
		readGraph(g);
	}
	
	public static void generateDataset(String graphPath, String dataset) {
		DatasetGenerator.builder()
			.withGraphSupplier(
				DotGraphIO.getMultiAttributeDataGraphSupplier(graphPath))
		    .setDynamismLevels(Lists.newArrayList(.2, .5, .8))
		    .setUrgencyLevels(Lists.newArrayList(20L))
		    .setScaleLevels(Lists.newArrayList(5d))
		    .setNumInstances(10)
			.setDatasetDir("files/datasets/" + dataset + "/")
			.setNumberOfShockwaves(NUMBER_OF_SHOCKWAVES)
			.setShockwaveExpandingSpeed(SHOCKWAVE_EXPANDING_SPEED)
			.setShockwaveRecedingSpeed(SHOCKWAVE_RECEDING_SPEED)
			.setShockwaveBehaviour(SHOCKWAVE_BEHAVIOUR)
			.build()
			.generate();
	}

	public static void performExperiment(String graphPath, String dataset) {
		System.out.println(System.getProperty("java.vm.name") + ", "
			      + System.getProperty("java.vm.vendor") + ", "
			      + System.getProperty("java.vm.version") + " (runtime version: "
			      + System.getProperty("java.runtime.version") + ")");
		System.out.println(System.getProperty("os.name") + " "
			      + System.getProperty("os.version") + " "
			      + System.getProperty("os.arch"));
		
		List<Scenario> scenarios = new ArrayList<>();
		
		File dir = new File("files/datasets/" + dataset + "/");
		for(File f : dir.listFiles(new FilenameFilter()
			{ 
            	public boolean accept(File dir, String filename) {
            		return filename.endsWith(".scen");
            	}
			})) {
			scenarios.add(ScenarioIO.reader().apply(f.toPath()));
		}
		
		//Scenario s = ScenarioIO.reader().apply(Paths.get("files/datasets/" + dataset + "/0.50-20-1.00-0.scen"));
	
		final ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance(70);
	    final long rpMs = 100L;
	    final long bMs = 20L;
	    final BidFunction bf = BidFunctions.BALANCED_HIGH;
	    final String masSolverName =
	    	      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
	    
	    final OptaplannerSolvers.Builder opFfdFactory =
	    	      OptaplannerSolvers.builder();
	    	      //.withSolverXmlResource(
	    	      //  "com/github/rinde/jaamas16/jaamas-solver.xml")
	    	      //.withUnimprovedMsLimit(rpMs)
	    	      //.withName(masSolverName)
	    	      //.withObjectiveFunction(objFunc);
		
		ExperimentResults results = Experiment.builder()
			      .computeLocal()
			      .withRandomSeed(123)
			      //.withThreads(1)
			      .repeat(1)
			      .withWarmup(30000)
			      .addResultListener(new CommandLineProgress(System.out))
			      .addResultListener(new VanLonHolvoetResultWriter(new File("files/results/" + dataset), dataset, (Gendreau06ObjectiveFunction)objFunc))
			      .usePostProcessor(new LogProcessor(objFunc))
			      .addConfigurations(mainConfigs(opFfdFactory, objFunc))
				.addScenarios(scenarios)
//				.addConfiguration(MASConfiguration.pdptwBuilder()
//						.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
//				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
//				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
//				// .addEventHandler(AddVehicleEvent.class,
//				// CustomVehicleHandler.INSTANCE)
//				.addEventHandler(AddVehicleEvent.class,
//						DefaultTruckFactory.builder()
//						//.setRoutePlanner(SolverRoutePlanner.supplier(OptaplannerSolvers.builder()
//						.setRoutePlanner(RtSolverRoutePlanner.supplier(OptaplannerSolvers.builder()
//								//.withCheapestInsertionSolver()
//								.withSolverXmlResource("com/github/rinde/jaamas16/jaamas-solver.xml")
//								.withName(masSolverName)
//								//.withFirstFitDecreasingWithTabuSolver()
//								.withUnimprovedMsLimit(rpMs)
//								//.buildSolverSupplier()))
//								.buildRealtimeSolverSupplier()))
//						//.setRoutePlanner(GotoClosestRoutePlanner.supplier())
//		                  .setCommunicator(RtSolverBidder.realtimeBuilder(objFunc,
//	                    opFfdFactory
//	                    //.withSolverKey(masSolverName)
//	                      .withUnimprovedMsLimit(bMs)
//	                      .buildRealtimeSolverSupplier())
//	                		  .withBidFunction(bf))
//						//.setCommunicator(RandomBidder.supplier())
//						.setLazyComputation(false)
//						.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
//						 .build())
//				.addModel(AuctionCommModel.builder(DoubleBid.class)
//						.withStopCondition(AuctionStopConditions.and(AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
//								AuctionStopConditions.<DoubleBid>or(AuctionStopConditions.<DoubleBid>allBidders(),
//										AuctionStopConditions.<DoubleBid>maxAuctionDuration(10000))))
//						.withMaxAuctionDuration(30 * 60 * 1000L))
//						//.withMaxAuctionDuration(5000L))
//				//.addModel(SolverModel.builder())
//				.addModel(RtSolverModel.builder())
//				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
//				.build())
				
//				.showGui(View.builder()
//						.with(RoadUserRenderer.builder().withToStringLabel())
//						.with(RouteRenderer.builder())
//						.with(PDPModelRenderer.builder())
//						.with(GraphRoadModelRenderer.builder()
//								//.withStaticRelativeSpeedVisualization()
//								.withDynamicRelativeSpeedVisualization()
//								)
//						.with(AuctionPanel.builder())
//							.with(RoutePanel.builder())
//							.with(TimeLinePanel.builder())
//							.withResolution(1280, 1024)
//							.withAutoPlay()
//							.withAutoClose())
		
				.perform();
		
	}

	static List<MASConfiguration> mainConfigs(
			OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {
		final long rpMs = 100;
		final long bMs = 20;
		final long maxAuctionDurationSoft = 10000L;

		final List<MASConfiguration> configs = new ArrayList<>();
		configs.add(createMAS(opFfdFactory, objFunc, rpMs, bMs,
				maxAuctionDurationSoft, false, 0L, false));
		final String solverKey =
				"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

		final long centralUnimprovedMs = 10000L;
		configs.add(createCentral(
				opFfdFactory.withSolverXmlResource(
						"com/github/rinde/jaamas16/jaamas-solver.xml")
				.withName(solverKey)
				.withUnimprovedMsLimit(centralUnimprovedMs),
				"OP.RT-FFD-" + solverKey));
		return configs;
	}

	static MASConfiguration createMAS(OptaplannerSolvers.Builder opFfdFactory,
			ObjectiveFunction objFunc, long rpMs, long bMs,
			long maxAuctionDurationSoft, boolean enableReauctions,
			long reauctCooldownPeriodMs, boolean computationsLogging) {
		final BidFunction bf = BidFunctions.BALANCED_HIGH;
		final String masSolverName =
				"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

		final String suffix;
		if (false == enableReauctions) {
			suffix = "-NO-REAUCT";
		} else if (reauctCooldownPeriodMs > 0) {
			suffix = "-reauctCooldownPeriod-" + reauctCooldownPeriodMs;
		} else {
			suffix = "";
		}

		MASConfiguration.Builder b = MASConfiguration.pdptwBuilder()
				.setName(
						"ReAuction-FFD-" + masSolverName + "-RP-" + rpMs + "-BID-" + bMs + "-"
								+ bf + suffix)
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class,
						DefaultTruckFactory.builder()
						.setRoutePlanner(RtSolverRoutePlanner.supplier(
								opFfdFactory.withSolverXmlResource(
										"com/github/rinde/jaamas16/jaamas-solver.xml")
								.withName(masSolverName)
								.withUnimprovedMsLimit(rpMs)
								.withTimeMeasurementsEnabled(computationsLogging)
								.buildRealtimeSolverSupplier()))
						.setCommunicator(

								RtSolverBidder.realtimeBuilder(objFunc,
										opFfdFactory.withSolverXmlResource(
												"com/github/rinde/jaamas16/jaamas-solver.xml")
										.withName(masSolverName)
										.withUnimprovedMsLimit(bMs)
										.withTimeMeasurementsEnabled(computationsLogging)
										.buildRealtimeSolverSupplier())
								.withBidFunction(bf)
								.withReauctionsEnabled(enableReauctions)
								.withReauctionCooldownPeriod(reauctCooldownPeriodMs))
						.setLazyComputation(false)
						.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
						.build())
				.addModel(AuctionCommModel.builder(DoubleBid.class)
						.withStopCondition(
								AuctionStopConditions.and(
										AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
										AuctionStopConditions.<DoubleBid>or(
												AuctionStopConditions.<DoubleBid>allBidders(),
												AuctionStopConditions
												.<DoubleBid>maxAuctionDuration(maxAuctionDurationSoft))))
						.withMaxAuctionDuration(30 * 60 * 1000L))
				.addModel(RtSolverModel.builder()
//						.withThreadPoolSize(3)
//						.withThreadGrouping(true)
						)
				.addModel(RealtimeClockLogger.builder());

		if (computationsLogging) {
			b = b.addModel(AuctionTimeStatsLogger.builder())
					.addModel(RoutePlannerStatsLogger.builder());
		}

		return b.build();
	}
	
	static void addCentral(Experiment.Builder experimentBuilder,
			OptaplannerSolvers.Builder opBuilder, String name) {
		experimentBuilder.addConfiguration(createCentral(opBuilder, name));
	}

	static MASConfiguration createCentral(OptaplannerSolvers.Builder opBuilder,
			String name) {
		return MASConfiguration.pdptwBuilder()
				.addModel(RtCentral.builder(opBuilder.buildRealtimeSolverSupplier())
						.withContinuousUpdates(true)
//						.withThreadGrouping(true)
						)
				.addModel(RealtimeClockLogger.builder())
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler())
				.setName(name)
				.build();
	}

	private static Iterable<TimedEvent> generateHighTrafficEvents(Graph<MultiAttributeData> g) {
		List<TimedEvent> events = new ArrayList<>();

		double[] factors = { 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 };

		RandomGenerator twister = new MersenneTwister();

		for (int i = 0; i < 5000; i++) {
			for (int factorIndex = 0; factorIndex < factors.length; factorIndex++) {
				events.add(ChangeConnectionSpeedEvent.create(0, g.getRandomConnection(twister), factors[factorIndex]));
			}
		}

		return events;
	}

	enum CustomVehicleHandler implements TimedEventHandler<AddVehicleEvent> {
		INSTANCE {
			public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
				// sim.register(new Truck(event.getVehicleDTO(), new
				// GotoClosestRoutePlanner(), null, null, false));
				sim.register(new Taxi(event.getVehicleDTO()));
			}
		};
	}

	enum CustomParcelHandler implements TimedEventHandler<AddParcelEvent> {
		INSTANCE {
			public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
				// System.out.println("Registered parcel at " +
				// event.getParcelDTO().getPickupLocation()
				// + " bound to: " +
				// event.getParcelDTO().getDeliveryLocation());
				AddParcelEvent.defaultHandler().handleTimedEvent(event, sim);
			}
		};
	}

	  @AutoValue
	  abstract static class AuctionStats {
	    abstract int getNumParcels();

	    abstract int getNumReauctions();

	    abstract int getNumUnsuccesfulReauctions();

	    abstract int getNumFailedReauctions();

	    static AuctionStats create(int numP, int numR, int numUn, int numF) {
	      return new AutoValue_ExperimentRunner_AuctionStats(numP, numR, numUn,
	        numF);
	    }
	  }

	  @AutoValue
	  abstract static class ExperimentInfo implements Serializable {
	    private static final long serialVersionUID = 6324066851233398736L;

	    abstract List<LogEntry> getLog();

	    abstract long getRtCount();

	    abstract long getStCount();

	    abstract StatisticsDTO getStats();

	    abstract ImmutableList<RealtimeTickInfo> getTickInfoList();

	    abstract Optional<AuctionStats> getAuctionStats();

	    static ExperimentInfo create(List<LogEntry> log, long rt, long st,
	        StatisticsDTO stats, ImmutableList<RealtimeTickInfo> dev,
	        Optional<AuctionStats> aStats) {
	      return new AutoValue_ExperimentRunner_ExperimentInfo(log, rt, st, stats,
	        dev, aStats);
	    }
	  }

	
	 static class LogProcessor
      implements PostProcessor<ExperimentInfo>, Serializable {
    private static final long serialVersionUID = 5997690791395717045L;
    ObjectiveFunction objectiveFunction;
    
	Logger LOGGER = LoggerFactory.getLogger("LogProcessor");

    LogProcessor(ObjectiveFunction objFunc) {
      objectiveFunction = objFunc;
    }

    @Override
    public ExperimentInfo collectResults(Simulator sim, SimArgs args) {

      @Nullable
      final RealtimeClockLogger logger =
        sim.getModelProvider().tryGetModel(RealtimeClockLogger.class);

      @Nullable
      final AuctionCommModel<?> auctionModel =
        sim.getModelProvider().tryGetModel(AuctionCommModel.class);

      final Optional<AuctionStats> aStats;
      if (auctionModel == null) {
        aStats = Optional.absent();
      } else {
        final int parcels = auctionModel.getNumParcels();
        final int reauctions = auctionModel.getNumAuctions() - parcels;
        final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
        final int failed = auctionModel.getNumFailedAuctions();
        aStats = Optional
          .of(AuctionStats.create(parcels, reauctions, unsuccessful, failed));
      }

      final StatisticsDTO stats =
    	        sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
      //  PostProcessors.statisticsPostProcessor(objectiveFunction)
      //    .collectResults(sim, args);

      LOGGER.info("success: {}", args);
      
      if(aStats.isPresent()) {
    	  System.out.println("Num Parcels: " + aStats.get().getNumParcels());
      	  System.out.println("Num Reauctions: " + aStats.get().getNumReauctions());
      	  System.out.println("Num Unsuccessful Reauctions: " + aStats.get().getNumUnsuccesfulReauctions());
      	  System.out.println("Num Failed Reauctions: " + aStats.get().getNumFailedReauctions());
      }
      
      System.out.println(stats.toString());
      
      if (logger == null) {
        return ExperimentInfo.create(new ArrayList<LogEntry>(), 0,
          sim.getCurrentTime() / sim.getTimeStep(), stats,
          ImmutableList.<RealtimeTickInfo>of(), aStats);
      }
      return ExperimentInfo.create(logger.getLog(), logger.getRtCount(),
        logger.getStCount(), stats, logger.getTickInfoList(), aStats);
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {

      System.out.println("Fail: " + args);
      e.printStackTrace();
      // System.out.println(AffinityLock.dumpLocks());

      return FailureStrategy.RETRY;
      // return FailureStrategy.ABORT_EXPERIMENT_RUN;

    }
  }

}
