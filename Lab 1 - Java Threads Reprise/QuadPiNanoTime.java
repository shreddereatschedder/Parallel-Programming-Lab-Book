/**
 * Calculates an approximation of pi in parallel using four threads,
 * each summing a quarter of the range of rectangles, and times how
 * long the calculation takes using nanosecond precision.
 */
public class QuadPiNanoTime extends Thread {

    static int numSteps = 10000000; // number of rectangles to sum
    static double step = 1.0 / (double) numSteps; // width of each rectangle

    double sum; // this thread's partial sum
    int begin, end; // range of steps this thread will handle

    public static void main(String[] args) throws Exception {

        long startTime = System.nanoTime(); // start timer

        int numThreads = 4;
        QuadPiNanoTime[] threads = new QuadPiNanoTime[numThreads];
        int chunk = numSteps / numThreads; // size of each quarter

        // create and start each thread with its own quarter of the range
        for (int t = 0; t < numThreads; t++) {
            threads[t] = new QuadPiNanoTime();
            threads[t].begin = t * chunk;
            threads[t].end = (t == numThreads - 1) ? numSteps : (t + 1) * chunk; // last thread takes any remainder
            threads[t].start();
        }

        // wait for all four threads to finish
        double totalSum = 0.0;
        for (int t = 0; t < numThreads; t++) {
            threads[t].join();
            totalSum += threads[t].sum;
        }

        long endTime = System.nanoTime(); // stop timer

        double pi = step * totalSum;

        System.out.println("Value of pi: " + pi);
        System.out.println("Calculated in " + (endTime - startTime) + " nanoseconds");
    }

    public void run() {
        sum = 0.0;
        for (int i = begin; i < end; i++) {
            double x = (i + 0.5) * step; // midpoint of this slice
            sum += 4.0 / (1.0 + x * x); // add rectangle's area
        }
    }
}