package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class Parallel_Knapsack {

    static ExecutorService pool;

    static List<boolean[]> next_generation_threads(List<boolean[]> population, int[] fitnessValues) throws InterruptedException, ExecutionException {

        List<boolean[]> next = elitism(population, fitnessValues, Math.min(eliteSize, population.size())); // Preserve the elite individuals.

        int remaining = population.size() - next.size();

        if (remaining <= 0) {
            return next;
        }

        int pairs = (remaining + 1) / 2; // Number of pairs needed to generate the remaining children.
        int targetTasks = P * 4; // Aim for approximately 4 tasks per worker.
        int chunkSize = Math.max(1, (pairs + targetTasks - 1) / targetTasks);

        List<Callable<List<boolean[]>>> tasks = new ArrayList<>();

        for (int start = 0; start < pairs; start += chunkSize) { // Each task processes multiple pairs.

            final int begin = start;
            final int end = Math.min(start + chunkSize, pairs);

            tasks.add(() -> {
                List<boolean[]> childrenList = new ArrayList<>();

                for (int k = begin; k < end; k++) {
                    boolean[][] parents = selection(population, fitnessValues);
                    boolean[][] children = crossover(parents);

                    mutate(children);

                    childrenList.add(children[0]);
                    childrenList.add(children[1]);
                }
                return childrenList;
            });
        }

        List<Future<List<boolean[]>>> results = pool.invokeAll(tasks); // Execute tasks concurrently.

        for (Future<List<boolean[]>> result : results) { // Collect the children.
            next.addAll(result.get());
        }

        return new ArrayList<>(next.subList(0, population.size())); // Trim the population if an odd number of children was generated.
    }

    static Result solve_knapsack_threads() throws ExecutionException, InterruptedException {
        List<boolean[]> population = generate_population();

        pool = Executors.newFixedThreadPool(P);

        try {
            int[] fitnessValues = all_fitness(population);

            List<Integer> fitnessOverTime = new ArrayList<>();
            boolean[] bestSolution = null;

            for (int gen = 0; gen < numGenerations; gen++) {
                population = next_generation_threads(population, fitnessValues);
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

        long endTime = System.currentTimeMillis();

        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);
        System.out.println("Parallel GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
