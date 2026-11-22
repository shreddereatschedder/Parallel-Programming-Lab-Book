package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;


public class Parallel_Knapsack {

    static ExecutorService pool;

    // Main loop
    record Result(boolean[] bestSolution, List<Integer> fitnessOverTime) {}

    static Result solve_knapsack_threads() throws InterruptedException, ExecutionException {
        List<boolean[]> population = generate_population();

        pool = Executors.newFixedThreadPool(P);
        try {
            int[] fitnessValues = all_fitness(population);

            List<Integer> fitnessOverTime = new ArrayList<>();
            boolean[] bestSolution = null;

            for (int gen = 0; gen < numGenerations; gen++) {
                population = next_generation(population, fitnessValues);
                fitnessValues = all_fitness(population); // evaluated once per generation

                int bestIdx = 0;
                for (int i = 1; i < fitnessValues.length; i++)
                    if (fitnessValues[i] > fitnessValues[bestIdx]) bestIdx = i;

                bestSolution = population.get(bestIdx);
                fitnessOverTime.add(fitnessValues[bestIdx]);
            }
            return new Result(bestSolution, fitnessOverTime);
        } finally {
            pool.shutdown();
        }
    }

    public static void main() throws IOException, ExecutionException, InterruptedException {
        load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_knapsack_threads();
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        long endTime = System.currentTimeMillis();

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);
        System.out.println("Parallel GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
