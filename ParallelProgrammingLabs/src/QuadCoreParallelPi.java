public class QuadCoreParallelPi extends Thread {

    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();

        QuadCoreParallelPi thread1 = new QuadCoreParallelPi();
        thread1.begin = 0;
        thread1.end = numSteps / 4;

        QuadCoreParallelPi thread2 = new QuadCoreParallelPi();
        thread2.begin = numSteps / 4;
        thread2.end = numSteps / 2;

        QuadCoreParallelPi thread3 = new QuadCoreParallelPi();
        thread3.begin = numSteps / 2;
        thread3.end = numSteps * 3 / 4;

        QuadCoreParallelPi thread4 = new QuadCoreParallelPi();
        thread4.begin = numSteps * 3 / 4;
        thread4.end = numSteps;

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        long endTime = System.currentTimeMillis();

        double pi = step * (thread1.sum + thread2.sum + thread3.sum + thread4.sum);

        System.out.println("Value of pi: " + pi);

        System.out.println("Calculated in " +
                (endTime - startTime) + " milliseconds");
    }

    static int numSteps = 10000000;

    static double step = 1.0 / (double) numSteps;

    double sum ;
    int begin, end ;

    public void run() {

        sum = 0.0 ;

        for(int i = begin ; i < end ; i++){
            double x = (i + 0.5) * step ;
            sum += 4.0 / (1.0 + x * x);
        }
    }
}