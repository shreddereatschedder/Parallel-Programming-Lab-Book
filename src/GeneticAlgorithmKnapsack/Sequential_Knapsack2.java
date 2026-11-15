package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class Sequnetial_Knapsack2 {

    // Parameters
    static String filename       = "src/GeneticAlgorithmKnapsack/debug_10.txt";
    static int    populationSize = 10;
    static int    numGenerations = 20;
    static int    tournamentSize = 3;
    static int    eliteSize      = 2;
    static double mutationRate   = 0.3;
    static double crossoverRate  = 0.6;

    // Problem data
    static int[] values, weights;
    static int capacity;

    static final Random rng = new Random();

    static class Individual {
        final boolean[] genes;
        final int fitness;

        Individual(boolean[] genes) {
            this.genes = genes;
            int weight = sum(weights, genes);
            this.fitness = weight <= capacity ? sum(values, genes) : 0;
        }
    }

    static final Comparator<Individual> BEST_FIRST = Comparator.comparingInt((Individual ind) -> ind.fitness).reversed();

    static int sum(int[] data, boolean[] genes) {
        int total = 0;
        for (int i = 0; i < genes.length; i++) if (genes[i]) total += data[i];
        return total;
    }

    static void loadFile() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename)).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        capacity = Integer.parseInt(lines.getFirst().split("\\s+")[1]);
        values   = new int[lines.size() - 1];
        weights  = new int[lines.size() - 1];
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\\s+");
            values[i - 1]  = Integer.parseInt(parts[0]);
            weights[i - 1] = Integer.parseInt(parts[1]);
        }
    }

    static List<Individual> generatePopulation() {
        List<Individual> population = new ArrayList<>();
        Set<String> seen = new HashSet<>();   // no duplicate individuals

        while (population.size() < populationSize) {
            boolean[] genes = new boolean[weights.length];
            for (int i = 0; i < genes.length; i++) genes[i] = rng.nextBoolean();

            // Repair: switch off random items until it fits
            int weight = sum(weights, genes);
            while (weight > capacity) {
                int i = rng.nextInt(genes.length);
                if (genes[i]) { genes[i] = false; weight -= weights[i]; }
            }

            if (seen.add(Arrays.toString(genes))) population.add(new Individual(genes));
        }
        return population;
    }

    static List<Individual> selectParents(List<Individual> population) {
        return rng.ints(0, population.size()).distinct().limit(tournamentSize)
                .mapToObj(population::get).sorted(BEST_FIRST).limit(2).toList();
    }

    static boolean[][] crossover(Individual a, Individual b) {
        boolean[] child1 = a.genes.clone(), child2 = b.genes.clone();   // clone: arrays are references
        if (rng.nextDouble() < crossoverRate) {
            int point = 1 + rng.nextInt(child1.length - 1);
            for (int i = point; i < child1.length; i++) {
                child1[i] = b.genes[i];
                child2[i] = a.genes[i];
            }
        }
        return new boolean[][] { child1, child2 };
    }

    static void mutate(boolean[] genes) {
        int weight = sum(weights, genes);
        for (int i = 0; i < genes.length; i++) {
            if (rng.nextDouble() >= mutationRate) continue;
            if (genes[i]) {
                genes[i] = false;
                weight -= weights[i];
            } else if (weight + weights[i] <= capacity) {
                genes[i] = true;
                weight += weights[i];
            }
        }
    }

    public static void main(String[] args) throws IOException {
        loadFile();

        List<Individual> population = generatePopulation();
        population.sort(BEST_FIRST);
        StringBuilder csv = new StringBuilder("generation,fitness\n");

        for (int gen = 0; gen < numGenerations; gen++) {
            // Keep the elites, then breed until the new population is full
            List<Individual> next = new ArrayList<>(population.subList(0, Math.min(eliteSize, populationSize)));
            while (next.size() < populationSize) {
                List<Individual> parents = selectParents(population);
                for (boolean[] child : crossover(parents.get(0), parents.get(1))) {
                    mutate(child);
                    next.add(new Individual(child));
                }
            }
            population = new ArrayList<>(next.subList(0, populationSize));
            population.sort(BEST_FIRST);
            csv.append(gen).append(',').append(population.getFirst().fitness).append('\n');
        }

        Individual best = population.getFirst();
        System.out.println("Best Solution: " + IntStream.range(0, best.genes.length).mapToObj(i -> best.genes[i] ? 1 : 0).toList());
        System.out.println("Total Value: " + best.fitness);
        System.out.println("Total Weight: " + sum(weights, best.genes) + " / " + capacity);
        System.out.println("Fitness curve written to fitness_over_time.csv");
        Files.writeString(Paths.get("src/GeneticAlgorithmKnapsack/fitness_over_time.csv"), csv.toString());

    }
}