package GeneticAlgorithmKnapsack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;


public class LoadFileAndParameters {

    // Parameters
    static String filename       = "src/GeneticAlgorithmKnapsack/input_1000.txt";
    static int    populationSize = 2000;
    static int    numGenerations = 100;
    static double mutationRate   = 0.6;
    static double crossoverRate  = 0.6;
    static int    tournamentSize = 3;
    static int    eliteSize      = 2;

    static int[] values;
    static int[] weights;
    static int capacity;

    static final Random random_number = new Random();

    static int P = 12;

    static int islands = 2;
    static int migration_interval = 10;   // generations between migrations
    static int migrant_count      = 3;    // how many individuals an island sends each time

    static void load_file() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));


        String[] header = lines.getFirst().split("\\s+");
        capacity = Integer.parseInt(header[1]);

        List<String> items = lines.subList(1, lines.size());
        values  = new int[items.size()];
        weights = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            String[] parts = items.get(i).split("\\s+");
            values[i]  = Integer.parseInt(parts[0]);
            weights[i] = Integer.parseInt(parts[1]);
        }
    }
}
