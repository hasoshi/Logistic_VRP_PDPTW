package com.github.tlkh.vrp.mpdptw.alns;

import com.github.tlkh.vrp.mpdptw.OptimalRequestSolver;
import com.github.tlkh.vrp.mpdptw.ProblemInstance;
import com.github.tlkh.vrp.mpdptw.Solution;
import com.github.tlkh.vrp.mpdptw.SolutionUtils;
import com.github.tlkh.vrp.mpdptw.operators.InsertionMethod;
import com.github.tlkh.vrp.mpdptw.operators.InsertionOperator;
import com.github.tlkh.vrp.mpdptw.operators.PickupMethod;


import java.util.Random;

public class InsertionHeuristic {

    private ProblemInstance instance;

    private InsertionOperator insertionOperator;

    private Random random;

    public InsertionHeuristic(ProblemInstance instance, Random random) {
        this.instance = instance;
        this.random = random;
        this.insertionOperator = new InsertionOperator(instance, random);
    }

    public Solution createInitialSolution() {
        Solution solution = SolutionUtils.createEmptyAnt(instance);
        for (int r = 0; r < instance.getNumReq(); r++) {
            addRequests(solution, r);
        }
        instance.solutionEvaluation(solution);
        return solution;
    }

    public void addRequests(Solution solution, int r) {
        boolean found = false;
        int kMax = solution.tours.size();
        for (int k = 0; k < kMax; k++) {
            if (insertionOperator.insertRequestOnVehicle(solution, k, r, PickupMethod.Random, InsertionMethod.Greedy)) {
                solution.requests.get(k).add(r);
                found = true;
                break;
            }
        }
        if (!found) {
            SolutionUtils.addEmptyVehicle(solution);
            OptimalRequestSolver optimalRequestSolver = new OptimalRequestSolver(r, instance);
            optimalRequestSolver.optimize();
            int lastK = solution.tours.size() - 1;
            solution.tours.get(lastK).clear();
            for (int i = 0; i < optimalRequestSolver.getBestRoute().length; i++) {
                solution.tours.get(lastK).add(optimalRequestSolver.getBestRoute()[i]);
            }
            solution.requests.get(lastK).add(r);
            instance.solutionEvaluation(solution, lastK);
        }
    }

}
