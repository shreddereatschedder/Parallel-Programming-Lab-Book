package GeneticAlgorithmKnapsack;

import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class Autorun {
    static String run_which_ga = "Sequential";

    public static void main(String[] args) throws Exception {

        if (run_which_ga.equals("Sequential")) {
            for (int i = 0; i < 20; i++) {
                System.out.println("Running Sequential, run attempt:" + i);
                Sequential_Knapsack.main();
            }
        } else if (run_which_ga.equals("Threads")) {
            for (int i = 0; i < 20; i++) {
                System.out.println("Running Threads with " + P + " threads, run attempt:" + i);
                Parallel_Knapsack.main();
            }
        } else {
            for (int i = 0; i < 20; i++) {
                System.out.println("Running Islands with " + islands + " islands, run attempt:" + i);
                IslandKnapsack.main();
            }
        }
    }
}
