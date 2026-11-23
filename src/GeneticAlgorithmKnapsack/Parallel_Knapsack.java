package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class Parallel_Knapsack {

    static ExecutorService pool;

    static  List<boolean[]> next_generation_threads(List<boolean[]> population, int[] fitnessValues) throws InterruptedException, ExecutionException {
        List<boolean[]> next = elitism(population, fitnessValues, Math.min(eliteSize, population.size()));
        int pairs = (population.size() - next.size() + 1) / 2;   // each pair gives 2 children

        // One task per pair: select parents, cross them over, mutate the children
        List<Callable<boolean[][]>> tasks = new ArrayList<>();
        for (int k = 0; k < pairs; k++) {
            tasks.add(() -> {
                boolean[][] parents  = selection(population, fitnessValues);
                boolean[][] children = crossover(parents);
                mutate(children);
                return children;
            });
        }

        // invokeAll runs them on the pool and waits until every task is done
        for (Future<boolean[][]> f : pool.invokeAll(tasks)) {
            next.addAll(Arrays.asList(f.get()));
        }
        return new ArrayList<>(next.subList(0, population.size()));
    }

    static int[] all_fitness_threads(List<boolean[]> population) throws InterruptedException, ExecutionException {
        int n = population.size();
        int[] result = new int[n];
        int chunk = (n + P - 1) / P;

        List<Future<?>> futures = new ArrayList<>();
        for (int start = 0; start < n; start += chunk) {
            int from = start, to = Math.min(n, start + chunk);
            futures.add(pool.submit(() -> {
                for (int i = from; i < to; i++) result[i] = fitness(population.get(i));
            }));
        }
        for (Future<?> f : futures) f.get();   // wait until every chunk is done
        return result;
    }

    static Result solve_knapsack_threads() throws ExecutionException, InterruptedException {
        List<boolean[]> population = generate_population();

        pool = Executors.newFixedThreadPool(P);

        try {
            int[] fitnessValues = all_fitness_threads(population);

            List<Integer> fitnessOverTime = new ArrayList<>();
            boolean[] bestSolution = null;

            for (int gen = 0; gen < numGenerations; gen++) {
                population = next_generation_threads(population, fitnessValues);
                fitnessValues = all_fitness_threads(population); // evaluated once per generation

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
