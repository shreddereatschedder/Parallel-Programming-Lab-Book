package GeneticAlgorithmKnapsack;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class IslandKnapsack {

    static ExecutorService pool;

    static final ThreadLocal<Integer> current_island = new ThreadLocal<>(); // Designates the island a thread belongs to

    static final List<ConcurrentLinkedQueue<boolean[]>> transit = new ArrayList<>(); // transit hold  the migrants waiting to be picked up by island i

    static void migrate(int island, List<boolean[]> population, int[] fitnessValues) { // Sends copies of the best individuals to the next island and takes in migrant individuals
        int migrants = Math.min(migrant_count, population.size() / 2);
        int replaced = 0;
        boolean[] genes;

        if (transit.size() < 2) { // edge case when an island has nobody to exchange with
            return;
        }

        List<Integer> order = IntStream.range(0, population.size()) // Indices of the population's best fitness first
                .boxed()
                .sorted((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]))
                .toList();

        ConcurrentLinkedQueue<boolean[]> outbox = transit.get((island + 1) % transit.size()); // Send copies of the best individuals to the next island
        for (int k = 0; k < migrants; k++) outbox.add(population.get(order.get(k)).clone());

        ConcurrentLinkedQueue<boolean[]> inbox = transit.get(island); // Replace the worst individuals with migrant individuals

        while (replaced < migrants && (genes = inbox.poll()) != null) {
            int worst = order.get(order.size() - 1 - replaced);
            population.set(worst, genes);
            fitnessValues[worst] = fitness(genes); // keep the fitness array in step
            replaced++;
        }
        inbox.clear(); // drop anything older than this round
    }

    // Run one island per thread and return the best result found
    static Result solve_islands(int islands) throws InterruptedException, ExecutionException {
        transit.clear();
        for (int i = 0; i < islands; i++) transit.add(new ConcurrentLinkedQueue<>());

        // Generate the original population only once.
        List<boolean[]> originalPopulation = generate_population();

        // Calculate how many individuals each island receives.
        int baseSize = originalPopulation.size() / islands;
        int remainder = originalPopulation.size() % islands;

        pool = Executors.newFixedThreadPool(islands);
        try {
            List<Future<Result>> futures = new ArrayList<>();

            int start = 0;
            for (int i = 0; i < islands; i++) {
                int island = i;
                int localSize = baseSize + (i < remainder ? 1 : 0);
                int end = start + localSize;

                // Extract this island's portion of the original population.
                List<boolean[]> localPopulation = new ArrayList<>(originalPopulation.subList(start, end));

                start = end;

                futures.add(pool.submit(() -> {
                    current_island.set(island);
                    return solve_knapsack_islands(localPopulation);
                }));
            }

            // Collect the best result from all islands.
            Result best = null;

            for (Future<Result> f : futures) {
                Result result = f.get();

                if (best == null || fitness(result.bestSolution()) > fitness(best.bestSolution())) {
                    best = result;
                }
            }

            return best;

        } finally {
            pool.shutdown();
        }
    }

    static Result solve_knapsack_islands(List<boolean[]> population) {
        int[] fitnessValues = all_fitness(population);

        List<Integer> fitnessOverTime = new ArrayList<>();
        boolean[] bestSolution = null;

        for (int generation = 0; generation < numGenerations; generation++) {
            population = next_generation(population, fitnessValues);
            fitnessValues = all_fitness(population);

            Integer island = current_island.get();

            if (island != null && (generation + 1) % migration_interval == 0) {
                migrate(island, population, fitnessValues);
            }

            int bestIndex = 0;

            for (int i = 1; i < fitnessValues.length; i++) {
                if (fitnessValues[i] > fitnessValues[bestIndex]) {
                    bestIndex = i;
                }
            }

            bestSolution = population.get(bestIndex).clone();
            fitnessOverTime.add(fitnessValues[bestIndex]);
        }
        return new Result(bestSolution, fitnessOverTime);
    }

    public static void main() throws Exception {
        load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_islands(islands);

        long endTime = System.currentTimeMillis();

        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);
        System.out.println("Island GA (" + islands + " islands) completed in " + (endTime - startTime) + " milliseconds");
    }
}