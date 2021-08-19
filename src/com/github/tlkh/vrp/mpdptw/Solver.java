package com.github.tlkh.vrp.mpdptw;

import com.github.tlkh.statistic.GlobalStatistics;
import com.github.tlkh.statistic.IterationStatistic;
import com.github.tlkh.tsp.utils.Maths;
import com.github.tlkh.vrp.mpdptw.aco.SequentialFeasiblePDPTW;
import com.github.tlkh.vrp.mpdptw.aco.SolutionBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Solver implements Runnable {

    private String problemName;

    private ProblemInstance instance;

    private List<IterationStatistic> iterationStatistics;

    private SMMAS smmas;

    private GlobalStatistics globalStatistics = new GlobalStatistics();

    private int maxIterations;

    private int seed;

    private double rho;

    private int statisticInterval;

    private boolean showLog;

    private LocalSearch localSearch;

    private boolean lsActive;

    private Class<? extends SolutionBuilder> solutionBuilderClass = SequentialFeasiblePDPTW.class;

    private boolean parallel;

    private String finalSolution;

    private boolean generateFile = Boolean.FALSE;

    public Solver(String problemName, ProblemInstance instance, int maxIterations, int seed, double rho, int statisticInterval, boolean showLog) {
        this.problemName = problemName;
        this.instance = instance;
        this.maxIterations = maxIterations;
        this.seed = seed;
        this.rho = rho;
        this.statisticInterval = statisticInterval;
        this.showLog = showLog;
    }

    @Override
    public void run() {
        // Initialization of SMMAS
        globalStatistics.startTimer();
        initProblemInstance();
        smmas.setRho(rho);
        smmas.setAlpha(1.0);
        smmas.setBeta(2.0);
        smmas.setnAnts(100);
        smmas.setDepth(instance.getNumNodes());
        smmas.allocateAnts();
        smmas.allocateStructures();
        smmas.setRandom(new Random(seed));
        //smmas.setParallel(parallel);
        smmas.setSolutionBuilder(solutionBuilderClass);
        smmas.computeNNList();
        smmas.initTry();
        globalStatistics.endTimer("SMMAS Initialization");

        // Init local search
        this.localSearch = new LocalSearch(instance, new Random(seed));

        // Execute SMMAS
        globalStatistics.startTimer();
        smmas.getBestSoFar().feasible = false;
        for (int i = 1; i <= maxIterations; i++) {
            IterationStatistic iterationStatistic = new IterationStatistic();
            smmas.setCurrentIteration(i);
            // Construction
            iterationStatistic.startTimer();
            smmas.constructSolutions();
            iterationStatistic.endTimer("Construction");
            // Daemon
            if (lsActive) {
                executeLocalSearch();
            }
            boolean hasBest = smmas.updateBestSoFar();
            if (hasBest) {
                if (lsActive) {
                    smmas.setPheromoneBoundsForLS();
                } 
            }
            smmas.updateRestartBest();
            iterationStatistic.endTimer("Daemon");
            // Pheromone
            iterationStatistic.startTimer();
            smmas.evaporation();
            smmas.pheromoneUpdate();
            smmas.checkPheromoneTrailLimits();
            smmas.searchControl();
            iterationStatistic.endTimer("Pheromone");
            // Statistics
            if (i % statisticInterval == 0) {
                iterationStatistic.setIteration(i);
                iterationStatistic.setBestSoFar(smmas.getBestSoFar().totalCost);
                iterationStatistic.setDiversity(smmas.calculateDiversity());
                iterationStatistic.setBranchFactor(smmas.nodeBranching());
                iterationStatistic.setIterationBest(smmas.findBest().totalCost);
                iterationStatistic.setIterationWorst(smmas.findWorst().totalCost);
                iterationStatistic.setFeasible(smmas.getBestSoFar().feasible ? 1.0 : 0.0);
                iterationStatistic.setIterationMean(Maths.getMean(smmas.getAntPopulation().stream().map(Solution::getCost).collect(Collectors.toList())));
                iterationStatistic.setIterationSd(Maths.getStd(smmas.getAntPopulation().stream().map(Solution::getCost).collect(Collectors.toList())));
                iterationStatistic.setPenaltyRate(smmas.getPenaltyRate());
                iterationStatistics.add(iterationStatistic);
                if (showLog) {
                    System.out.println(iterationStatistic);
                    logInFile(iterationStatistic.toString());
                }
            }
        }
        globalStatistics.endTimer("Algorithm");
        printFinalRoute();
    }

    private void printFinalRoute() {
        Solution ant = smmas.getBestSoFar();
        String msg = "";
        instance.restrictionsEvaluation(ant);
        boolean feasible = true;
        double cost;
        for (ArrayList route : ant.tours) {
            feasible &= instance.restrictionsEvaluation(route).feasible;
        }
        ant.feasible &= feasible;
        ant.totalCost = 0.0;
        for (int i = 0; i < ant.tours.size(); i++) {
            cost = instance.costEvaluation(ant.tours.get(i));
            ant.tourCosts.set(i, cost);
            ant.totalCost += cost;
        }
        msg += "\nInstance = " + problemName;
        msg += "\nBest solution feasibility = " + ant.feasible + "\nRoutes";
        for (ArrayList route : ant.tours) {
            msg += "\n" + StringUtils.join(route, "-");
        }
        msg += "\nRequests";
        for (ArrayList requests : ant.requests) {
            msg += "\n" + StringUtils.join(requests, "-");
        }
        msg += "\nNum. Vehicles = " + (ant.tours.size()-1);
        msg += "\nCost = " + ant.totalCost;
        msg += "\nPenalty = " + ant.timeWindowPenalty;
        msg += "\nTotal time (ms) = " + globalStatistics.getTimeStatistics().get("Algorithm");
        finalSolution = msg;
        System.out.println(msg);
        logInFile(msg);
        Set<Integer> processedNodes = new HashSet<>();
        for (int k = 0; k < smmas.getBestSoFar().tours.size(); k++) {
            for (int i = 1; i < smmas.getBestSoFar().tours.get(k).size() - 1; i++) {
                if (processedNodes.contains(smmas.getBestSoFar().tours.get(k).get(i))) {
                    //throw new RuntimeException("Invalid route, duplicated nodes");
                } else {
                    processedNodes.add(smmas.getBestSoFar().tours.get(k).get(i));
                }
            }
        }
        MapPrinter.printResult(ant, instance, 1200, 1000, problemName);
    }

    private void initProblemInstance() {
        try {
            smmas = new SMMAS(instance);
            iterationStatistics = new ArrayList<>(maxIterations);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void executeLocalSearch() {
        Solution bestAnt = smmas.findBest();
        instance.solutionEvaluation(bestAnt);
        Solution improvedAnt = localSearch.optimize(bestAnt);
        if (instance.getBest(bestAnt, improvedAnt) != bestAnt) {
            int antIndex = smmas.getAntPopulation().indexOf(bestAnt);
            smmas.getAntPopulation().set(antIndex, improvedAnt);
        }
    }

    private void logInFile(String text) {
        if (generateFile) {
            try {
                FileUtils.writeStringToFile(new File("C:\\Temp\\mpdptw\\result-" + problemName), text + "\n", "UTF-8", true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSolutionBuilderClass(Class<? extends SolutionBuilder> solutionBuilderClass) {
        this.solutionBuilderClass = solutionBuilderClass;
    }

    public GlobalStatistics getGlobalStatistics() {
        return globalStatistics;
    }

    public List<IterationStatistic> getIterationStatistics() {
        return iterationStatistics;
    }

    public Solution getBestSolution() {
        return smmas.getBestSoFar();
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public void setLsActive(boolean lsActive) {
        this.lsActive = lsActive;
    }

    public String getFinalSolution() {
        return finalSolution;
    }

    public void setGenerateFile(boolean generateFile) {
        this.generateFile = generateFile;
    }
}