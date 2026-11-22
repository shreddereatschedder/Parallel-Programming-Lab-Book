package GeneticAlgorithmKnapsack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Island-model GA for the knapsack problem.
 *
 * Every island is a separate program (JVM), usually on a separate machine. Each one runs its own
 * complete GA. Every `migrationInterval` generations it sends copies of its best individuals to the
 * next island in a ring, and replaces its own worst individuals with whatever has arrived.
 *
 * Usage (one command per island; the list of "host:port" is the same for all islands, in ring order):
 *   java GeneticAlgorithmKnapsack.IslandKnapsack <myIndex> host0:port0 host1:port1 host2:port2 ...
 *
 * Example, three islands on one machine:
 *   java GeneticAlgorithmKnapsack.IslandKnapsack 0 localhost:5001 localhost:5002 localhost:5003 &
 *   java GeneticAlgorithmKnapsack.IslandKnapsack 1 localhost:5001 localhost:5002 localhost:5003 &
 *   java GeneticAlgorithmKnapsack.IslandKnapsack 2 localhost:5001 localhost:5002 localhost:5003 &
 */
public class IslandKnapsack {

    // Parameters
    static String filename          = "src/GeneticAlgorithmKnapsack/input_1000.txt";
    static int    populationSize    = 200;
    static int    numGenerations    = 100;
    static double mutationRate      = 0.6;
    static double crossoverRate     = 0.6;
    static int    tournamentSize    = 3;
    static int    eliteSize         = 2;
    static int    migrationInterval = 10;   // generations between migrations
    static int    migrantCount      = 3;    // how many individuals are exchanged each time

    // Problem data
    static int[] values, weights;
    static int capacity;

    static final Random rng = new Random();   // only used by the main GA thread

    // Migration state
    static final ConcurrentLinkedQueue<boolean[]> inbox = new ConcurrentLinkedQueue<>();  // filled by listener threads
    static final AtomicInteger totalReceived = new AtomicInteger();
    static String neighbourHost;
    static int neighbourPort;
    static Socket neighbourSocket;
    static DataOutputStream toNeighbour;

    // ------------------------------------------------------------------
    // The GA itself (sequential inside each island)
    // ------------------------------------------------------------------

    static class Individual {
        final boolean[] genes;
        final int fitness;

        Individual(boolean[] genes) {
            this.genes = genes;
            int weight = sum(weights, genes);
            this.fitness = weight <= capacity ? sum(values, genes) : 0;
        }
    }

    static final Comparator<Individual> BEST_FIRST =
            Comparator.comparingInt((Individual ind) -> ind.fitness).reversed();

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
        Set<String> seen = new HashSet<>();

        while (population.size() < populationSize) {
            boolean[] genes = new boolean[weights.length];
            for (int i = 0; i < genes.length; i++) genes[i] = rng.nextBoolean();

            int weight = sum(weights, genes);
            while (weight > capacity) {                      // repair: switch items off until it fits
                int i = rng.nextInt(genes.length);
                if (genes[i]) { genes[i] = false; weight -= weights[i]; }
            }

            if (seen.add(Arrays.toString(genes))) population.add(new Individual(genes));
        }
        return population;
    }

    static List<Individual> selectParents(List<Individual> population) {
        return rng.ints(0, population.size()).distinct().limit(Math.min(tournamentSize, population.size()))
                .mapToObj(population::get).sorted(BEST_FIRST).limit(2).toList();
    }

    static boolean[][] crossover(Individual a, Individual b) {
        boolean[] child1 = a.genes.clone(), child2 = b.genes.clone();
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

    // ------------------------------------------------------------------
    // Migration over TCP
    // ------------------------------------------------------------------

    /** Starts background threads that accept connections and put incoming individuals into `inbox`. */
    static void startListener(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);
        Thread acceptor = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = server.accept();
                    Thread reader = new Thread(() -> readMigrants(socket));
                    reader.setDaemon(true);
                    reader.start();
                }
            } catch (IOException e) {
                // server socket closed: nothing to do
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** Wire format: repeated [int length][length booleans]. Plain data, no Java serialization. */
    static void readMigrants(Socket socket) {
        try (socket; DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            while (true) {
                int n = in.readInt();
                if (n != weights.length) return;             // not a compatible island: ignore it
                boolean[] genes = new boolean[n];
                for (int i = 0; i < n; i++) genes[i] = in.readBoolean();
                inbox.add(genes);
                totalReceived.incrementAndGet();
            }
        } catch (IOException e) {
            // neighbour finished or disconnected
        }
    }

    /** Best effort: if the neighbour isn't reachable, skip this migration and try again next time. */
    static void sendToNeighbour(List<Individual> migrants) {
        try {
            if (toNeighbour == null) {
                neighbourSocket = new Socket();
                neighbourSocket.connect(new InetSocketAddress(neighbourHost, neighbourPort), 1000);
                toNeighbour = new DataOutputStream(new BufferedOutputStream(neighbourSocket.getOutputStream()));
            }
            for (Individual m : migrants) {
                toNeighbour.writeInt(m.genes.length);
                for (boolean gene : m.genes) toNeighbour.writeBoolean(gene);
            }
            toNeighbour.flush();
        } catch (IOException e) {
            closeNeighbour();
        }
    }

    static void closeNeighbour() {
        toNeighbour = null;
        if (neighbourSocket != null) {
            try { neighbourSocket.close(); } catch (IOException ignored) { }
            neighbourSocket = null;
        }
    }

    /** `population` must be sorted best-first. Sends our best, then replaces our worst with what arrived. */
    static void migrate(List<Individual> population) {
        int m = Math.min(migrantCount, population.size() / 2);
        sendToNeighbour(new ArrayList<>(population.subList(0, m)));

        int replaced = 0;
        boolean[] genes;
        while (replaced < m && (genes = inbox.poll()) != null) {
            population.set(population.size() - 1 - replaced, new Individual(genes));
            replaced++;
        }
        inbox.clear();                                       // drop anything older than this migration
        population.sort(BEST_FIRST);
    }

    // ------------------------------------------------------------------
    // Main
    // ------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: IslandKnapsack <myIndex> host0:port0 host1:port1 ...");
            return;
        }
        int me = Integer.parseInt(args[0]);
        String[] nodes = Arrays.copyOfRange(args, 1, args.length);
        String[] mine = nodes[me].split(":");
        String[] next = nodes[(me + 1) % nodes.length].split(":");
        neighbourHost = next[0];
        neighbourPort = Integer.parseInt(next[1]);

        loadFile();
        startListener(Integer.parseInt(mine[1]));

        long start = System.currentTimeMillis();

        List<Individual> population = generatePopulation();
        population.sort(BEST_FIRST);

        for (int gen = 0; gen < numGenerations; gen++) {
            List<Individual> next_gen = new ArrayList<>(population.subList(0, Math.min(eliteSize, populationSize)));
            while (next_gen.size() < populationSize) {
                List<Individual> parents = selectParents(population);
                for (boolean[] child : crossover(parents.get(0), parents.get(1))) {
                    mutate(child);
                    next_gen.add(new Individual(child));
                }
            }
            population = new ArrayList<>(next_gen.subList(0, populationSize));
            population.sort(BEST_FIRST);

            if ((gen + 1) % migrationInterval == 0) migrate(population);
        }

        closeNeighbour();
        Individual best = population.get(0);
        System.out.println("Island " + me + ": best value " + best.fitness
                + ", weight " + sum(weights, best.genes) + "/" + capacity
                + ", migrants received " + totalReceived.get()
                + ", " + (System.currentTimeMillis() - start) + " ms");
    }
}