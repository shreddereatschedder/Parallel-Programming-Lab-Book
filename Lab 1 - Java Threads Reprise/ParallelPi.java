/**
 * Calculates an approximation of pi in parallel, using the rectangle
 * rule for numerical integration, and times how long the calculation takes.
 */
public class ParallelPi extends Thread {

    static int numSteps = 10000000; // number of rectangles to sum
    static double step = 1.0 / (double) numSteps; // width of each rectangle

    double sum; // this thread's partial sum
    int begin, end; // range of steps this thread will handle

    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis(); // start timer

        ParallelPi thread1 = new ParallelPi();
        thread1.begin = 0;
        thread1.end = numSteps / 2;

        ParallelPi thread2 = new ParallelPi();
        thread2.begin = numSteps / 2;
        thread2.end = numSteps;

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        long endTime = System.currentTimeMillis(); // stop timer

        double pi = step * (thread1.sum + thread2.sum);

        System.out.println("Value of pi: " + pi);
        System.out.println("Calculated in " + (endTime - startTime) + " milliseconds");
    }

    public void run() {
        sum = 0.0;
        for (int i = begin; i < end; i++) {
            double x = (i + 0.5) * step; // midpoint of this slice
            sum += 4.0 / (1.0 + x * x); // add rectangle's area
        }
    }
}