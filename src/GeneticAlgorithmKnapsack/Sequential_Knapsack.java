package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.util.*;

import static GeneticAlgorithmKnapsack.CommonMethods.*;
import static GeneticAlgorithmKnapsack.LoadFileAndParameters.*;

public class Sequential_Knapsack {

    static Result solve_knapsack() {
        List<boolean[]> population = generate_population();
        int[] fitnessValues = all_fitness(population);

        List<Integer> fitnessOverTime = new ArrayList<>();
        boolean[] bestSolution = null;

        for (int generation = 0; generation < numGenerations; generation++) {
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

    public static void main() throws IOException  {
        load_file();

        long startTime = System.currentTimeMillis();

        Result result = solve_knapsack();
        boolean[] best = result.bestSolution();
        int bestValue  = fitness(best);
        int bestWeight = weight_sum(best);

        long endTime = System.currentTimeMillis();

        System.out.println("Total Value: " + bestValue);
        System.out.println("Total Weight: " + bestWeight + " / " + capacity);
        System.out.println("Sequential GA completed in " + (endTime - startTime) + " milliseconds");
    }
}
