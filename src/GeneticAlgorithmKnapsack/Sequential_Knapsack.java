package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Sequential_Knapsack {
    static int weight_sum(boolean[] individual) { // sum the weights of individuals
        int weight = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) {
                weight += LoadFileAndParameters.weights[i];
            }
        }
        return weight;
    }

    static int fitness(boolean[] individual) { // get individual fitness
        int weight = 0;
        int value  = 0;

        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) {
                weight += LoadFileAndParameters.weights[i]; value += LoadFileAndParameters.values[i];
            }
        }

        if (weight <= LoadFileAndParameters.capacity) {
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

        while (population.size() < LoadFileAndParameters.populationSize) {
            boolean[] individual = new boolean[LoadFileAndParameters.weights.length];
            for (int i = 0; i < individual.length; i++) individual[i] = LoadFileAndParameters.random_number.nextBoolean();

            int weight = weight_sum(individual);
            while (weight > LoadFileAndParameters.capacity) {
                List<Integer> items_in_knapsack = new ArrayList<>();

                for (int i = 0; i < individual.length; i++) {
                    if (individual[i]) {
                        items_in_knapsack.add(i);
                    }
                }

                int index = items_in_knapsack.get(LoadFileAndParameters.random_number.nextInt(items_in_knapsack.size()));
                individual[index] = false;
                weight -= LoadFileAndParameters.weights[index];
            }
            if (seen.add(Arrays.toString(individual))) population.add(individual);
        }
        return population;
    }

    static boolean[][] selection(List<boolean[]> population, int[] fitnessValues) {
        int size = Math.min(LoadFileAndParameters.tournamentSize, population.size());

        Set<Integer> chosen_individuals = new HashSet<>();
        while (chosen_individuals.size() < size) {
            chosen_individuals.add(LoadFileAndParameters.random_number.nextInt(population.size()));   // a Set ignores repeats
        }

        List<Integer> tournament = new ArrayList<>(chosen_individuals);
        tournament.sort((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]));

        return new boolean[][] { population.get(tournament.get(0)), population.get(tournament.get(1)) };
    }

    static boolean[][] crossover(boolean[][] parents) {
        boolean[] child1 = parents[0].clone();
        boolean[] child2 = parents[1].clone();

        if (LoadFileAndParameters.random_number.nextDouble() < LoadFileAndParameters.crossoverRate) {
            int n = child1.length;
            int point = 1 + LoadFileAndParameters.random_number.nextInt(n - 1);
            System.arraycopy(parents[1], point, child1, point, n - point);
            System.arraycopy(parents[0], point, child2, point, n - point);
        }

        return new boolean[][] { child1, child2 };
    }

    static void mutate(boolean[][] individuals) {

        for (boolean[] individual : individuals) {
            mutate_one(individual);
        }
    }

    static void mutate_one(boolean[] individual) {
        int totalWeight = weight_sum(individual);

        for (int i = 0; i < individual.length; i++) {
            if (LoadFileAndParameters.random_number.nextDouble() >= LoadFileAndParameters.mutationRate) {
                continue;                                   // no mutation: leave this gene alone
            }

            if (individual[i]) {                            // item is in the knapsack: take it out
                individual[i] = false;
                totalWeight -= LoadFileAndParameters.weights[i];
            } else if (totalWeight + LoadFileAndParameters.weights[i] <= LoadFileAndParameters.capacity) {   // item is out: add it only if it still fits
                individual[i] = true;
                totalWeight += LoadFileAndParameters.weights[i];
            }
        }
    }

    static List<boolean[]> elitism(List<boolean[]> population, int[] fitnessValues, int eliteSize) {
        return IntStream.range(0, population.size())
                .boxed()
                .sorted((a, b) -> Integer.compare(fitnessValues[b], fitnessValues[a]))
                .limit(eliteSize)
                .map(population::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static List<boolean[]> next_generation(List<boolean[]> population, int[] fitnessValues) {
        List<boolean[]> next = elitism(population, fitnessValues, Math.min(LoadFileAndParameters.eliteSize, population.size()));

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

        for (int generation = 0; generation < LoadFileAndParameters.numGenerations; generation++) {
            population = next_generation(population, fitnessValues);
            fitnessValues = all_fitness(population); // evaluated once per generation

            int bestIndex = 0;
            for (int i = 1; i < fitnessValues.length; i++) {
                if (fitnessValues[i] > fitnessValues[bestIndex]) bestIndex = i;
            }

            bestSolution = population.get(bestIndex);
            fitnessOverTime.add(fitnessValues[bestIndex]);
        }
        return new Result(bestSolution, fitnessOverTime);
    }

    static void save_fitness_csv(List<Integer> fitnessOverTime, String path) throws IOException {
        StringBuilder sb = new StringBuilder("generation,fitness\n");

        for (int g = 0; g < fitnessOverTime.size(); g++)
            sb.append(g).append(',').append(fitnessOverTime.get(g)).append('\n');

        Files.writeString(Paths.get(path), sb.toString());
    }

    public static void main() throws IOException  {
        LoadFileAndParameters.load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_knapsack();
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + LoadFileAndParameters.capacity);

//        String baseName = Paths.get(filename).getFileName().toString();
//        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
//        save_fitness_csv(result.fitnessOverTime(), "src/GeneticAlgorithmKnapsack/fitness_over_time_" + baseName + ".csv");
//        System.out.println("Fitness written to fitness_over_time_" + baseName + ".csv");

        long endTime = System.currentTimeMillis();
        System.out.println("Sequential GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
