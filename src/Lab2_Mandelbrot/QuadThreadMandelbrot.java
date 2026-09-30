package Lab2_Mandelbrot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class QuadThreadMandelbrot extends Thread {

    int i_begin;
    int i_end;
    int j_begin;
    int j_end;
    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];
    static int[][] threadUsed = new int[N][N];

    public void run() {

        int threadID;

        if (i_begin == 0 && j_begin == 0) {
            threadID = 1;
        } else if (i_begin == 0 && j_begin == N/2) {
            threadID = 2;
        } else if (i_begin == N/2 && j_begin == 0) {
            threadID = 3;
        } else {
            threadID = 4;
        }

        // main calculation loop
        for (int i = i_begin; i < i_end; i++) {
            for (int j = j_begin; j < j_end; j++) {

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
                threadUsed[i][j] = threadID;
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
        thread1.i_begin = 0;
        thread1.i_end   = N/2;
        thread1.j_begin = 0;
        thread1.j_end   = N/2;

        QuadThreadMandelbrot thread2 = new QuadThreadMandelbrot();
        thread2.i_begin = 0;
        thread2.i_end   = N/2;
        thread2.j_begin = N/2;
        thread2.j_end   = N;

        QuadThreadMandelbrot thread3 = new QuadThreadMandelbrot();
        thread3.i_begin = N/2;
        thread3.i_end   = N;
        thread3.j_begin = 0;
        thread3.j_end   = N/2;


        QuadThreadMandelbrot thread4 = new QuadThreadMandelbrot();
        thread4.i_begin = N/2;
        thread4.i_end   = N;
        thread4.j_begin = N/2;
        thread4.j_end   = N;

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

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

                if (threadUsed[i][j] == 1) {
                    c = new Color(level/2, level/2, level);
                } else if (threadUsed[i][j] == 2) {
                    c = new Color(level/2, level, level);
                } else if (threadUsed[i][j] == 3) {
                    c = new Color(level, level/2, level/2);
                } else {
                    c = new Color(level, level/2, level);
                }

                img.setRGB(i, j, c.getRGB());
            }
        }

        // Print file
        ImageIO.write(img, "PNG", new File("src/Lab2_Mandelbrot/Quad_Thread_Mandelbrot.png"));
    }
}