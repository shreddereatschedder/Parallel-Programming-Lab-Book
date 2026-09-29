import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class DualThreadMandelbrot extends Thread {

    int begin;
    int end;

    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];

    public void run() {

        // main calculation loop
        for (int i = begin; i < end; i++) {
            for (int j = 0; j < N; j++) {

                double cr = (4.0 * i - 2 * N) / N;
                double ci = (4.0 * j - 2 * N) / N;

                double zr = cr, zi = ci;

                int k = 0;
                while (k < CUTOFF && zr * zr + zi * zi < 4.0) {

                    double newr = cr + zr * zr - zi * zi;
                    double newi = ci + 2 * zr * zi;

                    zr = newr;
                    zi = newi;

                    k++;
                }

                set[i][j] = k;
            }
        }
    }

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

        // Plot image
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);

        // Create Threads
        DualThreadMandelbrot thread1 = new DualThreadMandelbrot();
        thread1.begin = 0;
        thread1.end = N / 2;


        DualThreadMandelbrot thread2 = new DualThreadMandelbrot();
        thread2.begin = N / 2;
        thread2.end = N;

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in " + (endTime - startTime) + " milliseconds");

        // Draw pixels
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                int k = set[i][j];

                float level;
                if (k < CUTOFF) {
                    level = (float) k / CUTOFF;
                } else {
                    level = 0;
                }
                Color c = new Color(level / 2, level / 2, level);  // Blueish
                img.setRGB(i, j, c.getRGB());
            }
        }

        // Print file
        ImageIO.write(img, "PNG", new File("Dual_Thread_Mandelbrot.png"));
    }
}