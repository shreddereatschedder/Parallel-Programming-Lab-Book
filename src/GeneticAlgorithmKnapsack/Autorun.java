package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Autorun {
    static int P_amount = LoadFileAndParameters.P;
    static Boolean run_sequential = false;

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        if (run_sequential == true) {
            for (int i = 0; i < 20; i++) {
                System.out.println("Running Sequential, run attempt:" + i);
                Sequential_Knapsack.main();
            }
        } else {
            for (int i = 0; i < 20; i++) {
                System.out.println("Running Parallel with " + P_amount + " threads, run attempt:" + i);
                Parallel_Knapsack.main();
            }
        }
    }
}
