package com.github.vincentvangestel.rinsimextension.experiment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.github.rinde.logistics.pdptw.mas.comm.RandomBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RandomRoutePlanner;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.GeomHeuristics;
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
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
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
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.github.vincentvangestel.rinsimextension.vehicle.Taxi;
import com.github.vincentvangestel.roadmodelext.CachedDynamicGraphRoadModel;
import com.github.vincentvangestel.roadmodelext.ShortestPathCache;
import com.github.vincentvangestel.roadmodelext.ShortestPathCache.StaticSPCacheSup;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ExperimentRunner {

	private static final int SCENARIO_LENGTH_HOURS = 4;
	private static final int NUM_INSTANCES = 10;
	private static final long MS_IN_MIN = 60000L;
	private static final long MS_IN_H = 60 * MS_IN_MIN;
	private static final double TWENTYFIVE_KMH_IN_KMMS = 0.00000694444444;
	private static final Function<Long, Double> DEFAULT_SPEED_FUNCTION =
			new Function<Long, Double>() {
		@Override
		public Double apply(Long input) {
			return TWENTYFIVE_KMH_IN_KMMS;
		}
	};

	
	private static List<Integer> numberOfShockwaves = new ArrayList<Integer>();
	private static Optional<List<StochasticSupplier<Function<Long, Double>>>> shockwaveExpandingSpeeds = Optional.absent();
	private static Optional<List<StochasticSupplier<Function<Long, Double>>>> shockwaveRecedingSpeeds = Optional.absent();
	private static Optional<List<StochasticSupplier<Function<Double, Double>>>> shockwaveBehaviors = Optional.absent();
	private static Optional<List<StochasticSupplier<Long>>> shockwaveDurations = Optional.absent();
	private static Optional<List<StochasticSupplier<Long>>> shockwaveCreationTimes = Optional.absent();

	/**
	 * Usage: args = [ generate/experiment datasetID #buckets bucketID]
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
	    //args = new String[]{ "e", "profiler", "30", "1", "local", "t"};
		//args = new String[]{"e", "ssh1cllsml", "5", "1", "local", "c"};
		//args = new String[]{"e", "generateTest", "1", "1", "local", "t"};
		//args = new String[]{"g", "generateTest", "10", "1", "true", "32", "900000", "3", "0.5", "low"};
		//args = new String[]{"g", "ssh1cllsml", "2", "1", "false", "32", "7200000", "4", "0.5", "low"};
		//args = new String[]{"g", "profiler", "1", "1", "true", "16", "1800000", "2", "0.5", "low"};
		//args = new String[]{"v", "ssh1cllsml", "5", "0"};
		//args = new String[]{"v", "generateTest", "10", "0"};
		//args = new String[]{"v", "ssh1tllsml", "10", "0"};
		//args = new String[]{"c", "ssh1cllsml", "ssh1tllsml"};
		
		if(args.length < 2) {
			throw new IllegalArgumentException("Usage: args = [ g/e datasetID #buckets bucketID {Generate Options}]");
		}
		
		//String graphPath = new String("/home/vincent/Dropbox/UNI/Thesis/eclipse_workspace/RinSimExtension/files/maps/dot/leuven-large-pruned.dot");
		//String graphPath = new String("/home/r0373187/Thesis/RinSimExtension/files/maps/dot/leuven-large-pruned.dot");
		String graphPath = new String("files/maps/dot/leuven-large-simplified.dot");
		String cachePath = new String("files/maps/dot/leuven-large-simplified.cache");
		String datasetID = args[1].toLowerCase();		
		
		if(args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("convert")) {
			convertDataset(graphPath, datasetID, args[2].toLowerCase(), cachePath);
			System.exit(0);
		}
		
		int numberOfBuckets = Integer.parseInt(args[2]);
		int bucket = Integer.parseInt(args[3]);
		
		if(args[0].equalsIgnoreCase("v") || args[0].equalsIgnoreCase("visualize")) {
			showVisualization(datasetID, bucket);
		} 
		
		if(bucket > numberOfBuckets || bucket < 1) {
			throw new IllegalArgumentException("The bucket identifier should be a valid bucket (i.e. 1 <= bucketID <= #buckets)");
		}
		
		if(args[0].equalsIgnoreCase("g") || args[0].equalsIgnoreCase("generate")) {
			if(args.length < 10) {
				throw new IllegalArgumentException("Usage: args = [ g/e/v datasetID #buckets bucketID {cached} {#shockwaves,*} {shockwaveDuration,*} {shockwaveDistance,*} {shockwaveImpact,*} {frequency modifier (l,h)}]");
			}
			Optional<String> cache = Optional.absent();
			if(Boolean.parseBoolean(args[4])) {
				cache = Optional.of(cachePath);
			}
			
			ShockwaveFrequencyModifier mod = new ShockwaveFrequencyModifier(args[9]);
			numberOfShockwaves = parseNumberOfShockwaves(args[5], mod.getAmountModifier());
			shockwaveDurations = parseShockwaveDuration(args[6]);
			
			ShockwaveBehaviorMetadata sbm = new ShockwaveBehaviorMetadata(args[7], args[8], mod.getSizeModifier());
			shockwaveBehaviors = sbm.getBehaviorList();
			
			shockwaveCreationTimes = Optional.of(
					Collections.nCopies(
							numberOfShockwaves.size(),
							StochasticSuppliers.uniformLong(0, (int)(SCENARIO_LENGTH_HOURS * MS_IN_H))));
			
			shockwaveExpandingSpeeds = defaultShockwaveSpeed(numberOfShockwaves.size());
			shockwaveRecedingSpeeds = shockwaveExpandingSpeeds;
			
			System.out.println("> Generating " + (int)(NUM_INSTANCES/numberOfBuckets) * 3 + " instances of Datasets with shockwave parameters:");
			System.out.println("  - With a cached road model: " + cache.isPresent());
			System.out.println("  - Number of shockwaves: " + numberOfShockwaves.toString());
			System.out.println("  - Shockwave Durations: " + shockwaveDurations.get().toString());
			System.out.println("  - Shockwave Size: " + Arrays.toString(sbm.getSizes()));
			System.out.println("  - Shockwave Impacts: " + Arrays.toString(sbm.getImpacts()));
			System.out.println("  - Shockwave Frequency: " + mod.toString());
			
			generateDataset(graphPath, datasetID, numberOfBuckets, bucket, cache);
		} else if(args[0].equalsIgnoreCase("e") || args[0].equalsIgnoreCase("experiment")) {
			if(args.length < 6) {
				throw new IllegalArgumentException("Usage: experiment DatasetID #Buckets BucketID Local/Distributed Heuristic");
			}
			boolean local = parseLocal(args[4]);
			GeomHeuristic heuristic = parseHeuristic(args[5]);
			performExperiment(datasetID, numberOfBuckets, bucket, local, heuristic);
		} else {
			throw new IllegalArgumentException("Usage: args = [ g/e/v datasetID #buckets bucketID {Generate Options}]");
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

	private static Optional<List<StochasticSupplier<Function<Long, Double>>>> defaultShockwaveSpeed(int size) {
		return Optional.of(Collections.nCopies(size, StochasticSuppliers.constant(DEFAULT_SPEED_FUNCTION)));
	}

	private static List<Integer> parseNumberOfShockwaves(String numberOfShockwavesListString, int modifier) {
		String[] numberOfShockwavesStrings = numberOfShockwavesListString.split(",");
		List<Integer> numberOfShockwaves = new ArrayList<>();
		for(String numberOfShockwavesString : numberOfShockwavesStrings) {
			int nos = Integer.parseInt(numberOfShockwavesString);
			if(nos != 0) {
				numberOfShockwaves.add(nos * modifier);
			}
		}
		return numberOfShockwaves;
	}

	private static Optional<List<StochasticSupplier<Long>>> parseShockwaveDuration(String shockwaveDurationsListString) {
		String[] shockwaveDurationStrings = shockwaveDurationsListString.split(",");
		List<StochasticSupplier<Long>> shockwaveDurations = new ArrayList<>();
		for(String shockwaveDuration : shockwaveDurationStrings) {
			shockwaveDurations.add(StochasticSuppliers.constant(Long.parseLong(shockwaveDuration)));
		}
		return Optional.of(shockwaveDurations);
	}	
	
	private static boolean parseLocal(String local) {
		if(local.equalsIgnoreCase("local") || local.equalsIgnoreCase("l")) {
			return true;
		} else {
			return false;
		}
	}
	
	private static GeomHeuristic parseHeuristic(String heuristic) {
		if(heuristic.equalsIgnoreCase("t") || heuristic.equalsIgnoreCase("theoretical")) {
			return GeomHeuristics.theoreticalTime(70d);
		} else if(heuristic.equalsIgnoreCase("c") || heuristic.equalsIgnoreCase("current")) {
			return GeomHeuristics.time(70d);
		} else {
			return null;
		}
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
	
	public static void convertDataset(String graphPath, String datasetFrom, String datasetTo, String cachePath) {
		List<Scenario> scenarios = new ArrayList<>();
		
		File dir = new File("files/datasets/" + datasetFrom + "/");
		File[] fileList = dir.listFiles(new FilenameFilter()
		{ 
        	public boolean accept(File dir, String filename) {
        		return filename.endsWith(".scen");
        	}
		});
		Arrays.sort(fileList);
		System.out.println("Converting Datasets");
		
		try {
			Files.createDirectories(Paths.get("files/datasets/" + datasetTo + "/"));
		} catch (IOException e1) {
			System.out.println("Failed creating output directory");
			e1.printStackTrace();
		}
		
		for(File f : fileList) {
			try {
				ScenarioIO.write(
				Scenario.builder(ScenarioIO.reader().apply(f.toPath()))
					.removeModelsOfType(PDPDynamicGraphRoadModel.Builder.class)
					.addModel(PDPDynamicGraphRoadModel
							.builderForDynamicGraphRm(CachedDynamicGraphRoadModel
									.builder(ListenableGraph.supplier(
											(Supplier<? extends Graph<MultiAttributeData>>) DotGraphIO.getMultiAttributeDataGraphSupplier(graphPath)),
											null, cachePath)
									.withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
									.withDistanceUnit(SI.KILOMETER)
									.withModificationCheck(false))
							.withAllowVehicleDiversion(true)).build(), Paths.get("files/datasets/" + datasetTo + "/" + f.getName()));
			} catch (IOException e) {
				System.out.println("Failed converting file: " + f.getName());
				e.printStackTrace();
			}
		}
		System.out.println("Done");
		
	}
	
	public static void generateDataset(String graphPath, String dataset, int numberOfBuckets, int bucket, Optional<String> cachePath) {
		DatasetGenerator.Builder b = DatasetGenerator.builder()
			//.setNumThreads(1)
			.withGraphSupplier(
				DotGraphIO.getMultiAttributeDataGraphSupplier(graphPath))
		    .setDynamismLevels(Lists.newArrayList(.2, .5, .8))
			//.setDynamismLevels(Lists.newArrayList(.2))
			.setScenarioLength(SCENARIO_LENGTH_HOURS)
		    .setUrgencyLevels(Lists.newArrayList(20L))
		    .setScaleLevels(Lists.newArrayList(5d))
		    //.setScaleLevels(Lists.newArrayList(0.5d))
		    .setNumInstances((int)(NUM_INSTANCES/numberOfBuckets), (bucket - 1) * (int)(NUM_INSTANCES/numberOfBuckets))
			.setDatasetDir("files/datasets/" + dataset + "/")
			.setNumberOfShockwaves(numberOfShockwaves)
			.setShockwaveExpandingSpeed(shockwaveExpandingSpeeds)
			.setShockwaveRecedingSpeed(shockwaveRecedingSpeeds)
			.setShockwaveBehaviour(shockwaveBehaviors)
			.setShockwaveDuration(shockwaveDurations)
			.setShockwaveCreationTimes(shockwaveCreationTimes);
		if(cachePath.isPresent()) {
			//b.withCacheSupplier(ShortestPathCache.getShortestPathCacheSupplier(cachePath.get()));
			b.withCachePath(cachePath.get());
		}
			b.build()
			.generate();
	}
	
	public static void showVisualization(String dataset, int index) {

		Scenario scenario;
		
		File dir = new File("files/datasets/" + dataset + "/");
		File[] fileList = dir.listFiles(new FilenameFilter()
		{ 
        	public boolean accept(File dir, String filename) {
        		return filename.endsWith(".scen");
        	}
		});
		Arrays.sort(fileList);
		System.out.println("Showing Run " + fileList[index].getName());
		
		scenario = ScenarioIO.reader().apply(fileList[index].toPath());
		
//		scenario = ScenarioConverters.toSimulatedtime().apply(scenario);
		
		final ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance(70);
	    final long rpMs = 100L;
	    final long bMs = 20L;
	    final BidFunction bf = BidFunctions.BALANCED_HIGH;
	    final String masSolverName =
	    	      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
	    
	    final OptaplannerSolvers.Builder opFfdFactory =
	    	      OptaplannerSolvers.builder()
	    	      	.withSolverHeuristic(GeomHeuristics.time(70d));
	    	      //.withSolverXmlResource(
	    	      //  "com/github/rinde/jaamas16/jaamas-solver.xml")
	    	      //.withUnimprovedMsLimit(rpMs)
	    	      //.withName(masSolverName)
	    	      //.withObjectiveFunction(objFunc);
		
		//final ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance(70);
	    
		ExperimentResults results = Experiment.builder()
				  .computeLocal()  
			      .withRandomSeed(7919)
			      .repeat(1)
			      .withThreads(1)
			      .usePostProcessor(new LogProcessor(objFunc))
			      .addConfigurations(mainConfigs(opFfdFactory, objFunc, GeomHeuristics.time(70d)))
			      .addScenario(scenario)
//			      .addConfiguration(MASConfiguration.pdptwBuilder()
//							.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
//							.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
//							.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
//					.addEventHandler(AddVehicleEvent.class,
//							DefaultTruckFactory.builder()
//							.setCommunicator(RandomBidder.supplier())
//							.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
//							.setRoutePlanner(RandomRoutePlanner.supplier())
//							 .build())
//					.addModel(AuctionCommModel.builder(DoubleBid.class)
//							.withStopCondition(AuctionStopConditions.and(AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
//									AuctionStopConditions.<DoubleBid>or(AuctionStopConditions.<DoubleBid>allBidders(),
//											AuctionStopConditions.<DoubleBid>maxAuctionDuration(5000))))
//							.withMaxAuctionDuration(30 * 60 * 1000L))
//							//.withMaxAuctionDuration(5000L))
//					.addModel(SolverModel.builder())
//					.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
//					.build())
				.showGui(View.builder()
						.with(RoadUserRenderer.builder().withToStringLabel())
						.with(RouteRenderer.builder())
						.with(PDPModelRenderer.builder())
						.with(GraphRoadModelRenderer.builder()
								//.withStaticRelativeSpeedVisualization()
								.withDynamicRelativeSpeedVisualization()
								)
						.with(AuctionPanel.builder())
							.with(RoutePanel.builder())
							.with(TimeLinePanel.builder())
							.withResolution(1280, 1024)
							.withAutoPlay()
							.withAutoClose())
		
				.perform();
		
		System.out.println(results.toString());
		System.exit(0);

	}

	public static void performExperiment(String dataset, int numberOfBuckets, int bucket, boolean local, GeomHeuristic heuristic) {
		System.out.println(System.getProperty("java.vm.name") + ", "
			      + System.getProperty("java.vm.vendor") + ", "
			      + System.getProperty("java.vm.version") + " (runtime version: "
			      + System.getProperty("java.runtime.version") + ")");
		System.out.println(System.getProperty("os.name") + " "
			      + System.getProperty("os.version") + " "
			      + System.getProperty("os.arch"));
		System.out.println("Performing experiment ");
		
		List<Scenario> scenarios = new ArrayList<>();
		
		File dir = new File("files/datasets/" + dataset + "/");
		File[] fileList = dir.listFiles(new FilenameFilter()
		{ 
        	public boolean accept(File dir, String filename) {
        		return filename.endsWith(".scen");
        	}
		});
		Arrays.sort(fileList);
		int from = (bucket - 1) * (int)(fileList.length / numberOfBuckets);
		int to = (bucket * (int)(fileList.length / numberOfBuckets));
		System.out.println("Running Experiments from " + fileList[from].getName() + " to " + fileList[to-1].getName());
		
		for(File f : Arrays.copyOfRange(fileList, from, to)) {
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
	    	      OptaplannerSolvers.builder()
	    	      	.withSolverHeuristic(heuristic);
	    	      //.withSolverXmlResource(
	    	      //  "com/github/rinde/jaamas16/jaamas-solver.xml")
	    	      //.withUnimprovedMsLimit(rpMs)
	    	      //.withName(masSolverName)
	    	      //.withObjectiveFunction(objFunc);
	    
	    Experiment.Builder exBuilder = Experiment.builder();
	    
	    if(local) {
	    	exBuilder = exBuilder.computeLocal();
	    } else {
	    	exBuilder = exBuilder
	    			.computeDistributed()
	    			.numBatches(1);
	    }
	    
		ExperimentResults results = exBuilder
			      .withRandomSeed(7919)
			      .repeat(1)
			      //.withThreads(1)
			      //.withWarmup(30000)
			      .addResultListener(new CommandLineProgress(System.out))
			      .addResultListener(new VanLonHolvoetResultWriter(
			    		  new File("files/results/" + dataset),
			    		  dataset, bucket,
			    		  (Gendreau06ObjectiveFunction)objFunc))
			      .usePostProcessor(new LogProcessor(objFunc))
			      .addConfigurations(mainConfigs(opFfdFactory, objFunc, heuristic))
				.addScenarios(scenarios)
				.perform();
		
		System.out.println(results.toString());
		System.exit(0);
	}

	static List<MASConfiguration> mainConfigs(
			OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc,
			GeomHeuristic heuristic) {
		final long rpMs = 100;
		final long bMs = 20;
		final long maxAuctionDurationSoft = 10000L;

		final List<MASConfiguration> configs = new ArrayList<>();
		configs.add(createMAS(opFfdFactory, objFunc, rpMs, bMs,
				maxAuctionDurationSoft, false, 0L, false, heuristic));
		final String solverKey =
				"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

		final long centralUnimprovedMs = 10000L;
		configs.add(createCentral(
				opFfdFactory.withSolverXmlResource(
						"com/github/rinde/jaamas16/jaamas-solver.xml")
				.withSolverHeuristic(heuristic)
				.withName(solverKey)
				.withUnimprovedMsLimit(centralUnimprovedMs),
				"OP.RT-FFD-" + solverKey, heuristic));
		return configs;
	}

	static MASConfiguration createMAS(OptaplannerSolvers.Builder opFfdFactory,
			ObjectiveFunction objFunc, long rpMs, long bMs,
			long maxAuctionDurationSoft, boolean enableReauctions,
			long reauctCooldownPeriodMs, boolean computationsLogging,
			GeomHeuristic heuristic) {
		final BidFunction bf = BidFunctions.BALANCED_HIGH;
		final String masSolverName =
				"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

		final StringBuilder suffix = new StringBuilder();
		if (false == enableReauctions) {
			suffix.append("-NO-REAUCT");
		} else if (reauctCooldownPeriodMs > 0) {
			suffix.append("-reauctCooldownPeriod-" + reauctCooldownPeriodMs);
		}
		suffix.append("-heuristic-" + opFfdFactory.getSolverHeuristic().toString());

		MASConfiguration.Builder b = MASConfiguration.pdptwBuilder()
				.setName(
						"ReAuction-FFD-" + masSolverName + "-RP-" + rpMs + "-BID-" + bMs + "-"
								+ bf + suffix.toString())
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class,
						DefaultTruckFactory.builder()
						.setRouteHeuristic(heuristic)
						.setRoutePlanner(RtSolverRoutePlanner.supplier(
								opFfdFactory.withSolverXmlResource(
										"com/github/rinde/jaamas16/jaamas-solver.xml")
								.withSolverHeuristic(heuristic)
								.withName(masSolverName)
								.withUnimprovedMsLimit(rpMs)
								.withTimeMeasurementsEnabled(computationsLogging)
								.buildRealtimeSolverSupplier()))
						.setCommunicator(

								RtSolverBidder.realtimeBuilder(objFunc,
										opFfdFactory.withSolverXmlResource(
												"com/github/rinde/jaamas16/jaamas-solver.xml")
										.withSolverHeuristic(heuristic)
										.withName(masSolverName)
										.withUnimprovedMsLimit(bMs)
										.withTimeMeasurementsEnabled(computationsLogging)
										.buildRealtimeSolverSupplier())
								.withGeomHeuristic(heuristic)
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
			OptaplannerSolvers.Builder opBuilder, String name, GeomHeuristic heuristic) {
		experimentBuilder.addConfiguration(createCentral(opBuilder, name, heuristic));
	}

	static MASConfiguration createCentral(OptaplannerSolvers.Builder opBuilder,
			String name, GeomHeuristic heuristic) {
		return MASConfiguration.pdptwBuilder()
				.addModel(RtCentral.builder(opBuilder.withSolverHeuristic(heuristic).buildRealtimeSolverSupplier())
						.withContinuousUpdates(true)
//						.withThreadGrouping(true)
						)
				.addModel(RealtimeClockLogger.builder())
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(ChangeConnectionSpeedEvent.class, ChangeConnectionSpeedEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler(heuristic))
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
	  abstract static class AuctionStats implements Serializable {
		private static final long serialVersionUID = -597628566631371202L;

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
    private final ObjectiveFunction objectiveFunction;
    
	static final Logger LOGGER = LoggerFactory.getLogger("LogProcessor");

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
//    	        sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
        PostProcessors.statisticsPostProcessor(objectiveFunction)
          .collectResults(sim, args);

      //LOGGER.info("success: {}", args);
      
//      if(aStats.isPresent()) {
//    	  System.out.println("Num Parcels: " + aStats.get().getNumParcels());
//      	  System.out.println("Num Reauctions: " + aStats.get().getNumReauctions());
//      	  System.out.println("Num Unsuccessful Reauctions: " + aStats.get().getNumUnsuccesfulReauctions());
//      	  System.out.println("Num Failed Reauctions: " + aStats.get().getNumFailedReauctions());
//      }
//      
//      System.out.println(stats.toString());
      
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
	 
	 static class ShockwaveBehaviorMetadata {

		 private final Optional<List<StochasticSupplier<Function<Double, Double>>>> behaviorList;
		 private double[] sizes;
		 private double[] impacts;

		 ShockwaveBehaviorMetadata(String distanceListString,
				 String impactListString, double modifier) {
			 String[] distanceStrings = distanceListString.split(",");
			 String[] impactStrings = impactListString.split(",");

			 sizes = new double[distanceStrings.length];
			 impacts = new double[impactStrings.length];

			 if(distanceStrings.length != impacts.length) {
				 throw new IllegalArgumentException("The Generate Options should all have the same size!");
			 }

			 List<StochasticSupplier<Function<Double,Double>>> behaviors = new ArrayList<>();

			 for(int i = 0; i < distanceStrings.length; i++) {
				 final double distance = Double.parseDouble(distanceStrings[i]) / modifier;
				 final double impact = Double.parseDouble(impactStrings[i]);

				 sizes[i] = distance;
				 impacts[i] = impact;

				 behaviors.add(StochasticSuppliers.constant(new Function<Double,Double>() {
					 @Override
					 public Double apply(Double input) {
						 if(input >= distance) {
							 return 1d;
						 } else {
							 return impact;
						 }
					 }
				 }));
			 }

			 behaviorList = Optional.of(behaviors);
		 }

		 public Optional<List<StochasticSupplier<Function<Double, Double>>>> getBehaviorList() {
			 return behaviorList;
		 }

		 public double[] getSizes() {
			 return sizes.clone();
		 }

		 public double[] getImpacts() {
			 return impacts.clone();
		 }

	 }
	 
	 static class ShockwaveFrequencyModifier {
		 
		 final boolean modified;
		 
		 ShockwaveFrequencyModifier(String m) {
			 modified = m.equalsIgnoreCase("h") || m.equalsIgnoreCase("high"); 
		 }
		 
		 int getAmountModifier() {
			 return modified ? 2 : 1;
		 }
		 
		 double getSizeModifier() {
			 return modified ? Math.sqrt(2) : 1;
		 }
		 
		 public String toString() {
			 return modified ? "High" : "Low";
		 }
	 }

}
