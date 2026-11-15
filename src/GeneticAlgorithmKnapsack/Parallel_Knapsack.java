package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.*;

public class Parallel_Knapsack {
    // Parameters
    static String filename       = "src/GeneticAlgorithmKnapsack/input_1000.txt";
    static int    populationSize = 2000;
    static int    numGenerations = 100;
    static double mutationRate   = 0.6;
    static double crossoverRate  = 0.6;
    static int    tournamentSize = 3;
    static int    eliteSize      = 2;

    static int    P              = 12;

    // Problem data
    static int[] values;
    static int[] weights;
    static int capacity;

    static ExecutorService pool;

    static final Random rng = new Random();

    // Setup
    static void load_file() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));


        String[] header = lines.getFirst().split("\\s+");
        capacity = Integer.parseInt(header[1]);   // header[0] is ignored

        List<String> items = lines.subList(1, lines.size());
        values  = new int[items.size()];
        weights = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            String[] parts = items.get(i).split("\\s+");
            values[i]  = Integer.parseInt(parts[0]);
            weights[i] = Integer.parseInt(parts[1]);
        }
    }

    // GA components
    static int weight_sum(boolean[] individual) {
        int w = 0;
        for (int i = 0; i < individual.length; i++) if (individual[i]) w += weights[i];
        return w;
    }

    static int fitness(boolean[] individual) {
        int w = 0, v = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) { w += weights[i]; v += values[i]; }
        }
        return w <= capacity ? v : 0;
    }

    static int[] all_fitness(List<boolean[]> population) { // get fitness from the whole population
        int[] result = new int[population.size()];
        for (int i = 0; i < population.size(); i++) {
            result[i] = fitness(population.get(i));
        }
        return result;
    }

    static List<boolean[]> generate_population() {

        List<boolean[]> population = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (population.size() < populationSize) {
            boolean[] individual = new boolean[weights.length];
            for (int i = 0; i < individual.length; i++) individual[i] = rng.nextBoolean();

            int weight = weight_sum(individual);
            while (weight > capacity) {
                List<Integer> ones = new ArrayList<>();
                for (int i = 0; i < individual.length; i++) {
                    if (individual[i]) {
                        ones.add(i);
                    }
                }

                int idx = ones.get(rng.nextInt(ones.size()));
                individual[idx] = false;
                weight -= weights[idx];
            }

            if (seen.add(Arrays.toString(individual))) population.add(individual);
        }
        return population;
    }

    static boolean[][] selection(List<boolean[]> population, int[] fitnessValues) {
        Random rng = ThreadLocalRandom.current();

        int size = Math.min(tournamentSize, population.size());

        Set<Integer> chosen = new HashSet<>();
        while (chosen.size() < size) {
            chosen.add(rng.nextInt(population.size()));   // a Set ignores repeats
        }

        List<Integer> tournament = new ArrayList<>(chosen);
        tournament.sort((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]));

        return new boolean[][] { population.get(tournament.get(0)), population.get(tournament.get(1)) };
    }

    static boolean[][] crossover(boolean[][] parents) {
        Random rng = ThreadLocalRandom.current();

        boolean[] child1 = parents[0].clone();
        boolean[] child2 = parents[1].clone();

        if (rng.nextDouble() < crossoverRate) {
            int n = child1.length;
            int point = 1 + rng.nextInt(n - 1);
            System.arraycopy(parents[1], point, child1, point, n - point);
            System.arraycopy(parents[0], point, child2, point, n - point);
        }
        return new boolean[][] { child1, child2 };
    }

    static void mutate(boolean[][] individuals) {
        Random rng = ThreadLocalRandom.current();

        for (boolean[] individual : individuals) {
            int totalWeight = weight_sum(individual);
            for (int i = 0; i < individual.length; i++) {
                if (rng.nextDouble() < mutationRate) {
                    if (!individual[i]) {
                        int newWeight = totalWeight + weights[i];
                        if (newWeight <= capacity) {   // only add if it still fits
                            individual[i] = true;
                            totalWeight = newWeight;
                        }
                    } else {
                        individual[i] = false;
                        totalWeight -= weights[i];
                    }
                }
            }
        }
    }

    static List<boolean[]> elitism(List<boolean[]> population, int[] fitnessValues, int eliteSize) {
        return IntStream.range(0, population.size()).boxed()
                .sorted((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]))
                .limit(eliteSize)
                .map(population::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static List<boolean[]> next_generation(List<boolean[]> population, int[] fitnessValues) throws InterruptedException, ExecutionException {

        List<boolean[]> next = elitism(population, fitnessValues, Math.min(eliteSize, population.size()));
        int pairs = (population.size() - next.size() + 1) / 2;   // each pair gives 2 children

        // One task per pair: select parents, cross them over, mutate the children
        List<Callable<boolean[][]>> tasks = new ArrayList<>();
        for (int k = 0; k < pairs; k++) {
            tasks.add(() -> {
                boolean[][] children = crossover(selection(population, fitnessValues));
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


    // Main loop
    record Result(boolean[] bestSolution, List<Integer> fitnessOverTime) {}

    static Result solve_knapsack() throws InterruptedException, ExecutionException {
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
            pool.shutdown();   // without this the program never exits
        }
    }

    static void save_fitness_csv(List<Integer> fitnessOverTime, String path) throws IOException {
        StringBuilder sb = new StringBuilder("generation,fitness\n");

        for (int g = 0; g < fitnessOverTime.size(); g++)
            sb.append(g).append(',').append(fitnessOverTime.get(g)).append('\n');

        Files.writeString(Paths.get(path), sb.toString());
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_knapsack();
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);

//        String baseName = Paths.get(filename).getFileName().toString();
//        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
//        save_fitness_csv(result.fitnessOverTime(), "src/GeneticAlgorithmKnapsack/parallel_fitness_over_time_" + baseName + ".csv");
//        System.out.println("Fitness curve written to parallel_fitness_over_time_" + baseName + ".csv");

        long endTime = System.currentTimeMillis();
        System.out.println("Parallel GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
