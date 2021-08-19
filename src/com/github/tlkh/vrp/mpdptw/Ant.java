package com.github.tlkh.vrp.mpdptw;

import java.util.*;
import java.io.File;
import java.io.IOException;

public class Ant {
	public static void main(String[] args) throws IOException {
		String rootDirectory = "resources/mpdptw/l_4_25_1.txt";
		DataReader dat = new DataReader();
		File folder = new File(rootDirectory);
		if (folder.exists()) {
			Solver solver = new Solver(folder.getAbsolutePath(), dat.getMpdptwInstance(folder), 2000, 1, 0.03, 10,true);
			solver.run();
			// solver.printFinalRoute();
		} else {
			System.out.println("Cannot load file");
		}

	}
}
