package com.github.tlkh.vrp.mpdptw;

import com.github.tlkh.vrp.mpdptw.operators.ExchangeRequestOperator;
import com.github.tlkh.vrp.mpdptw.operators.InsertionMethod;
import com.github.tlkh.vrp.mpdptw.operators.InsertionOperator;
import com.github.tlkh.vrp.mpdptw.operators.PickupMethod;
import com.github.tlkh.vrp.mpdptw.operators.RelocateNodeOperator;
import com.github.tlkh.vrp.mpdptw.operators.RelocateRequestOperator;
import com.github.tlkh.vrp.mpdptw.operators.RemovalMethod;
import com.github.tlkh.vrp.mpdptw.operators.RemovalOperator;
import com.github.tlkh.vrp.mpdptw.operators.Req;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalSearch {

    private ProblemInstance instance;

    private Random random;

    private RemovalOperator removalOperator;

    private InsertionOperator insertionOperator;

    private RelocateNodeOperator relocateNodeOperator;

    private RelocateRequestOperator relocateRequestOperator;

    private ExchangeRequestOperator exchangeRequestOperator;

    public LocalSearch(ProblemInstance instance, Random random) {
        this.instance = instance;
        this.random = random;
        this.removalOperator = new RemovalOperator(instance, random);
        this.insertionOperator = new InsertionOperator(instance, random);
        this.relocateRequestOperator = new RelocateRequestOperator(instance, random);
        this.relocateNodeOperator = new RelocateNodeOperator(instance);
        this.exchangeRequestOperator = new ExchangeRequestOperator(instance, random);
    }

    public Solution optimize(Solution ant) {
        Solution tempAnt = ant;
        boolean improvement = true;
        double oldCost = ant.totalCost + ant.timeWindowPenalty;
        double newCost;
        while (improvement) {
            improvement = false;
            tempAnt = relocateNodeOperator.relocate(tempAnt);
//            tempAnt = optimize(tempAnt, RemovalMethod.Random, PickupMethod.Random, InsertionMethod.Greedy);
//            tempAnt = optimize(tempAnt, RemovalMethod.ExpensiveNode, PickupMethod.Random, InsertionMethod.Greedy);
            tempAnt = optimize(tempAnt, RemovalMethod.ExpensiveRequest, PickupMethod.Random, InsertionMethod.Greedy);
//            tempAnt = optimize(tempAnt, RemovalMethod.Shaw, PickupMethod.Random, InsertionMethod.Greedy);
//            tempAnt = optimize(tempAnt, RemovalMethod.Random, PickupMethod.Random, InsertionMethod.Regret3);
//            tempAnt = optimize(tempAnt, RemovalMethod.ExpensiveNode, PickupMethod.Random, InsertionMethod.Regret3);
//            tempAnt = optimize(tempAnt, RemovalMethod.ExpensiveRequest, PickupMethod.Random, InsertionMethod.Regret3);
//            tempAnt = optimize(tempAnt, RemovalMethod.Shaw, PickupMethod.Random, InsertionMethod.Regret3);
            tempAnt = relocateRequestOperator.relocate(tempAnt);
            tempAnt = exchangeRequestOperator.exchange(tempAnt);
            newCost = tempAnt.totalCost + tempAnt.timeWindowPenalty;
            if (newCost < oldCost) {
                oldCost = tempAnt.totalCost + tempAnt.timeWindowPenalty;
                improvement = true;
            }
        }
        return tempAnt;
    }

    public Solution optimize(Solution ant, RemovalMethod removalMethod, PickupMethod pickupMethod, InsertionMethod insertionMethod) {
        Solution tempAnt = SolutionUtils.createEmptyAnt(instance);
        Solution improvedAnt = SolutionUtils.createEmptyAnt(instance);
        SolutionUtils.copyFromTo(ant, tempAnt);
        SolutionUtils.copyFromTo(ant, improvedAnt);
        boolean improvement = true;
        boolean improved = false;
        while (improvement) {
            List<Req> removedRequests = removeRequests(tempAnt, removalMethod);
            insertionOperator.insertRequests(tempAnt, removedRequests, pickupMethod, insertionMethod, 0);
            instance.solutionEvaluation(tempAnt);
            SolutionUtils.removeEmptyVehicles(tempAnt);
            improvement = instance.getBest(improvedAnt, tempAnt) == tempAnt;
            if (improvement) {
                SolutionUtils.copyFromTo(tempAnt, improvedAnt);
                improved = true;
            }
        }
        if (improved) {
            return improvedAnt;
        } else {
            return ant;
        }
    }

    private List<Req> removeRequests(Solution tempAnt, RemovalMethod removalType) {
        List<Req> removedRequests = new ArrayList<>();
        switch (removalType) {
            case Random:
                removedRequests = removalOperator.removeRandomRequest(tempAnt.tours, tempAnt.requests, generateNoRemovalRequests());
                break;
            case Shaw:
                removedRequests = removalOperator.removeShawRequests(tempAnt, generateNoRemovalRequests());
                break;
            case ExpensiveNode:
                removedRequests = removalOperator.removeMostExpensiveNodes(tempAnt.tours, tempAnt.requests, generateNoRemovalRequests());
                break;
            case ExpensiveRequest:
                removedRequests = removalOperator.removeExpensiveRequests(tempAnt.tours, tempAnt.requests, generateNoRemovalRequests());
                break;
        }

        return removedRequests;
    }

    private int generateNoRemovalRequests() {
        int min = (int) Math.min(6, 0.15 * instance.getNumReq());
        int max = (int) Math.min(18, 0.4 * instance.getNumReq()) + 1;
        return min + (int) (random.nextDouble() * (max - min));
    }
}
