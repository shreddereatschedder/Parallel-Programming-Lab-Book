/**
 * Calculates an approximation of pi sequentially, using the rectangle
 * rule for numerical integration, and times how long the calculation
 * takes using nanosecond precision.
 */
public class SequentialPiNanoTime {

    public static void main(String[] args) {

        long startTime = System.nanoTime(); // start timer

        int numSteps = 10000000; // number of rectangles to sum
        double step = 1.0 / (double) numSteps; // width of each rectangle
        double sum = 0.0;

        for (int i = 0; i < numSteps; i++) {
            double x = (i + 0.5) * step; // midpoint of this slice
            sum += 4.0 / (1.0 + x * x); // add rectangle's area
        }

        double pi = step * sum;

        long endTime = System.nanoTime(); // stop timer

        System.out.println("Value of pi: " + pi);
        System.out.println("Calculated in " + (endTime - startTime) + " nanoseconds");
    }
}