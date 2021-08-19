package com.github.tlkh.vrp.mpdptw.aco;

import com.github.tlkh.vrp.mpdptw.SMMAS;
import com.github.tlkh.vrp.mpdptw.ProblemInstance;

import java.util.Random;

public interface SolutionBuilder {

    void init(ProblemInstance instance, Random random, SMMAS mmas);

    void constructSolutions();

    void onSearchControlExecute();

}
