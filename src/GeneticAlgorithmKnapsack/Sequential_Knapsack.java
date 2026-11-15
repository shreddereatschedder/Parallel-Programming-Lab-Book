package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Sequential_Knapsack {
    // Parameters
    static String filename       = "src/GeneticAlgorithmKnapsack/input_1000.txt";
    static int    populationSize = 2000;
    static int    numGenerations = 100;
    static double mutationRate   = 0.6;
    static double crossoverRate  = 0.6;
    static int    tournamentSize = 3;
    static int    eliteSize      = 2;

    // Problem data
    static int[] values;
    static int[] weights;
    static int capacity;

    static final Random rng = new Random();

    // Load Dataset from txt file
    static void load_file() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));

        String[] header = lines.getFirst().split("\\s+"); // splits dataset based on whitespace
        capacity = Integer.parseInt(header[1]);   // header[0] is ignored

        List<String> items = lines.subList(1, lines.size());
        values  = new int[items.size()]; // store values in one array
        weights = new int[items.size()]; // store weights in another array
        for (int i = 0; i < items.size(); i++) {
            String[] parts = items.get(i).split("\\s+");
            values[i]  = Integer.parseInt(parts[0]);
            weights[i] = Integer.parseInt(parts[1]);
        }
    }

    static int weight_sum(boolean[] individual) { // sum the weights of individuals
        int weight = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) {
                weight += weights[i];
            }
        }
        return weight;
    }

    static int fitness(boolean[] individual) { // get individual fitness
        int weight = 0, value = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) { weight += weights[i]; value += values[i]; }
        }
        if (weight <= capacity) {
            return value;
        } else {
            return 0;
        }
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

    static List<boolean[]> next_generation(List<boolean[]> population, int[] fitnessValues) {
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
            population = next_generation(population, fitnessValues);
            fitnessValues = all_fitness(population); // evaluated once per generation

            int bestIdx = 0;
            for (int i = 1; i < fitnessValues.length; i++) {
                if (fitnessValues[i] > fitnessValues[bestIdx]) bestIdx = i;
            }

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

    public static void main(String[] args) throws IOException  {
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
//        save_fitness_csv(result.fitnessOverTime(), "src/GeneticAlgorithmKnapsack/fitness_over_time_" + baseName + ".csv");
//        System.out.println("Fitness written to fitness_over_time_" + baseName + ".csv");

        long endTime = System.currentTimeMillis();
        System.out.println("Sequential GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
