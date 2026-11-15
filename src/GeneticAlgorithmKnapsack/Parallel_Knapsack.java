package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Parallel_Knapsack {
    // Parameters
    static String filename       = "src/GeneticAlgorithmKnapsack/debug_10.txt";
    static int    populationSize = 10;
    static int    numGenerations = 20;
    static double mutationRate   = 0.3;
    static double crossoverRate  = 0.6;
    static int    tournamentSize = 3;

    // Problem data
    static int[] values;
    static int[] weights;
    static int capacity;

    static final Random rng = new Random();

    // Setup
    static void load_file() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename)).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

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

    static int[] all_fitness(List<boolean[]> population) {
        IntStream s = IntStream.range(0, population.size());
        return s.map(i -> fitness(population.get(i))).toArray(); // order preserved
    }

    static List<boolean[]> generate_population() {
        List<boolean[]> population = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (population.size() < populationSize) {
            boolean[] individual = new boolean[weights.length];
            for (int i = 0; i < individual.length; i++) individual[i] = rng.nextBoolean();

            int w = weight_sum(individual);
            while (w > capacity) {
                List<Integer> ones = new ArrayList<>();
                for (int i = 0; i < individual.length; i++) if (individual[i]) ones.add(i);
                if (ones.isEmpty()) break;
                int idx = ones.get(rng.nextInt(ones.size()));
                individual[idx] = false;
                w -= weights[idx];
            }

            if (seen.add(Arrays.toString(individual))) population.add(individual);
        }
        return population;
    }

    static boolean[][] selection(List<boolean[]> population, int[] fitnessValues) {
        List<Integer> idx = IntStream.range(0, population.size()).boxed().collect(Collectors.toList());
        List<Integer> tournament = new ArrayList<>(idx.subList(0, tournamentSize));
        tournament.sort((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]));
        return new boolean[][] { population.get(tournament.get(0)), population.get(tournament.get(1)) };
    }

    static boolean[][] crossover(boolean[][] parents) {
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

    static List<boolean[]> next_generation(List<boolean[]> population, int[] fitnessValues, int eliteSize) {
        List<boolean[]> next = elitism(population, fitnessValues, Math.min(eliteSize, population.size()));

        while (next.size() < population.size()) {
            boolean[][] parents  = selection(population, fitnessValues);
            boolean[][] children = crossover(parents);
            mutate(children);
            next.add(children[0]);
            next.add(children[1]);
        }
        return new ArrayList<>(next.subList(0, population.size()));
    }

    // Main loop
    record Result(boolean[] bestSolution, List<Integer> fitnessOverTime) {}

    static Result solve_knapsack() {
        List<boolean[]> population = generate_population();
        int[] fitnessValues = all_fitness(population);

        List<Integer> fitnessOverTime = new ArrayList<>();
        boolean[] bestSolution = null;

        for (int gen = 0; gen < numGenerations; gen++) {
            population = next_generation(population, fitnessValues, 2);
            fitnessValues = all_fitness(population); // evaluated once per generation

            int bestIdx = 0;
            for (int i = 1; i < fitnessValues.length; i++)
                if (fitnessValues[i] > fitnessValues[bestIdx]) bestIdx = i;

            bestSolution = population.get(bestIdx);
            fitnessOverTime.add(fitnessValues[bestIdx]);
        }
        return new Result(bestSolution, fitnessOverTime);
    }

    static void save_fitness_csv(List<Integer> fitnessOverTime, String path) throws IOException {
        StringBuilder sb = new StringBuilder("generation,fitness\n");
        for (int g = 0; g < fitnessOverTime.size(); g++)
            sb.append(g).append(',').append(fitnessOverTime.get(g)).append('\n');
        Files.writeString(Paths.get(path), sb.toString());
    }

    public static void main(String[] args) throws IOException {
        load_file();

        Result result = solve_knapsack();
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        System.out.println("Total Value of Current Attempt: " + bestValue);

        int[] bits = new int[best.length];
        for (int i = 0; i < best.length; i++) bits[i] = best[i] ? 1 : 0;

        System.out.println("Best Solution: " + Arrays.toString(bits));
        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);

        save_fitness_csv(result.fitnessOverTime(), "src/GeneticAlgorithmKnapsack/fitness_over_time.csv");
        System.out.println("Fitness curve written to fitness_over_time.csv");

    }
}
