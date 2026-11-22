package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static GeneticAlgorithmKnapsack.Sequential_Knapsack.*;


public class IslandKnapsack2 {
    static int migration_interval = 10;   // generations between migrations
    static int migrant_count      = 3;    // how many individuals an island sends each time

    // The island a thread belongs to. null in the normal sequential run, so the hook does nothing there.
    static final ThreadLocal<Integer> current_island = new ThreadLocal<>();

    // mailboxes.get(i) holds the migrants waiting to be picked up by island i
    static final List<ConcurrentLinkedQueue<boolean[]>> mailboxes = new ArrayList<>();

    /** Sends copies of this island's best individuals to the next island and takes in what has arrived. */
    static void migrate(int island, List<boolean[]> population, int[] fitnessValues) {
        if (mailboxes.size() < 2) return;                                     // a single island has nobody to exchange with
        int m = Math.min(migrant_count, population.size() / 2);

        // Indices of the population, best fitness first
        List<Integer> order = IntStream.range(0, population.size())
                .boxed()
                .sorted((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]))
                .toList();

        // Send copies of my best m individuals to the next island in the ring
        ConcurrentLinkedQueue<boolean[]> outbox = mailboxes.get((island + 1) % mailboxes.size());
        for (int k = 0; k < m; k++) outbox.add(population.get(order.get(k)).clone());

        // Replace my worst individuals with whatever the previous island has sent me
        ConcurrentLinkedQueue<boolean[]> inbox = mailboxes.get(island);
        int replaced = 0;
        boolean[] genes;
        while (replaced < m && (genes = inbox.poll()) != null) {
            int worst = order.get(order.size() - 1 - replaced);
            population.set(worst, genes);
            fitnessValues[worst] = fitness(genes);                            // keep the fitness array in step
            replaced++;
        }
        inbox.clear();                                                         // drop anything older than this round
    }

    /** Runs one island per thread and returns the best result found by any of them. */
    static Sequential_Knapsack.Result solve_islands(int islands) throws InterruptedException, ExecutionException {
        mailboxes.clear();
        for (int i = 0; i < islands; i++) mailboxes.add(new ConcurrentLinkedQueue<>());

        ExecutorService pool = Executors.newFixedThreadPool(islands);
        try {
            List<Future<Sequential_Knapsack.Result>> futures = new ArrayList<>();
            for (int i = 0; i < islands; i++) {
                int island = i;
                futures.add(pool.submit(() -> {
                    current_island.set(island);
                    return solve_knapsack();                                  // the original GA, unchanged
                }));
            }

            Result best = null;
            for (Future<Result> f : futures) {
                Result result = f.get();                                      // wait for this island to finish
                if (best == null || fitness(result.bestSolution()) > fitness(best.bestSolution())) best = result;
            }
            return best;
        } finally {
            pool.shutdown();
        }
    }

    /** Entry point with an argument. With no argument it simply runs the original sequential main(). */
    public static void main(String[] args) throws Exception {

        int islands = 2;
        LoadFileAndParameters.load_file();

        long startTime = System.currentTimeMillis();

        Sequential_Knapsack.Result result = solve_islands(islands);
        boolean[] best = result.bestSolution();

        System.out.println("Total Value: " + fitness(best));
        System.out.println("Total Weight: " + weight_sum(best) + " / " + LoadFileAndParameters.capacity);
        System.out.println("Island GA (" + islands + " islands) completed in " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }
}