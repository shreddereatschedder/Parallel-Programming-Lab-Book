package Lab2_Mandelbrot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class QuadThreadMandelbrot extends Thread {

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
        QuadThreadMandelbrot thread1 = new QuadThreadMandelbrot();
        thread1.begin = 0;
        thread1.end = N / 4;


        QuadThreadMandelbrot thread2 = new QuadThreadMandelbrot();
        thread2.begin = N / 4;
        thread2.end = N / 2;

        QuadThreadMandelbrot thread3 = new QuadThreadMandelbrot();
        thread3.begin = N/2;
        thread3.end = N * 3/ 4;


        QuadThreadMandelbrot thread4 = new QuadThreadMandelbrot();
        thread4.begin = N * 3 / 4;
        thread4.end = N;

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.start();
        thread4.start();

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
                Color c;

                if (i < N / 2) {
                    c = new Color(level, level/2, level);
                } else {
                    c = new Color(level/2, level/2, level);
                }

                img.setRGB(i, j, c.getRGB());
            }
        }

        // Print file
        ImageIO.write(img, "PNG", new File("Quad_Thread_Mandelbrot_coloured.png"));
    }
}