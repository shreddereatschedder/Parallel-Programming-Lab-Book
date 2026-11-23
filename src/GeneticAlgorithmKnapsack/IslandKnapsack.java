package GeneticAlgorithmKnapsack;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class IslandKnapsack {
    static final ThreadLocal<Integer> current_island = new ThreadLocal<>(); // Designates the island a thread belongs to

    static final List<ConcurrentLinkedQueue<boolean[]>> mailboxes = new ArrayList<>(); // mailboxes hold  the migrants waiting to be picked up by island i

    static void migrate(int island, List<boolean[]> population, int[] fitnessValues) { // Sends copies of the best individuals to the next island and takes in migrant individuals
        if (mailboxes.size() < 2) { // edge case when an island has nobody to exchange with
            return;
        }

        int m = Math.min(migrant_count, population.size() / 2);

        List<Integer> order = IntStream.range(0, population.size()) // Indices of the population's best fitness first
                .boxed()
                .sorted((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]))
                .toList();


        ConcurrentLinkedQueue<boolean[]> outbox = mailboxes.get((island + 1) % mailboxes.size()); // Send copies of best individuals to the next island
        for (int k = 0; k < m; k++) outbox.add(population.get(order.get(k)).clone());

        ConcurrentLinkedQueue<boolean[]> inbox = mailboxes.get(island); // Replace worst individuals with migrant individuals
        int replaced = 0;
        boolean[] genes;
        while (replaced < m && (genes = inbox.poll()) != null) {
            int worst = order.get(order.size() - 1 - replaced);
            population.set(worst, genes);
            fitnessValues[worst] = fitness(genes); // keep the fitness array in step
            replaced++;
        }
        inbox.clear(); // drop anything older than this round
    }

    // Run one island per thread and return the best result found
    static Result solve_islands(int islands) throws InterruptedException, ExecutionException {
        mailboxes.clear();
        for (int i = 0; i < islands; i++) mailboxes.add(new ConcurrentLinkedQueue<>());

        ExecutorService pool = Executors.newFixedThreadPool(islands);
        try {
            List<Future<Result>> futures = new ArrayList<>();
            for (int i = 0; i < islands; i++) {
                int island = i;
                futures.add(pool.submit(() -> {
                    current_island.set(island);
                    return solve_knapsack_islands();
                }));
            }

            Result best = null;
            for (Future<Result> f : futures) {
                Result result = f.get(); // wait for this island to finish
                if (best == null || fitness(result.bestSolution()) > fitness(best.bestSolution())) best = result;
            }
            return best;
        } finally {
            pool.shutdown();
        }
    }

    static Result solve_knapsack_islands() {
        List<boolean[]> population = generate_population();
        int[] fitnessValues = all_fitness(population);

        List<Integer> fitnessOverTime = new ArrayList<>();
        boolean[] bestSolution = null;

        for (int generation = 0; generation < numGenerations; generation++) {
            population = next_generation(population, fitnessValues);
            fitnessValues = all_fitness(population); // evaluated once per generation

            Integer island = current_island.get(); // null when running the normal sequential GA
            if (island != null && (generation + 1) % migration_interval == 0) {
                migrate(island, population, fitnessValues);
            }

            int bestIndex = 0;
            for (int i = 1; i < fitnessValues.length; i++) {
                if (fitnessValues[i] > fitnessValues[bestIndex]) bestIndex = i;
            }

            bestSolution = population.get(bestIndex);
            fitnessOverTime.add(fitnessValues[bestIndex]);
        }
        return new Result(bestSolution, fitnessOverTime);
    }

    public static void main() throws Exception {
        load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_islands(islands);
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        long endTime = System.currentTimeMillis();

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);
        System.out.println("Island GA (" + islands + " islands) completed in "
                + (endTime - startTime) + " milliseconds");
    }
}