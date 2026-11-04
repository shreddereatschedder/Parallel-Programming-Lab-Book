package Lab4_InteractingThreads;

import java.awt.* ;
import javax.swing.* ;

public class SequentialLapLace {

    final static int N = 256 ;
    final static int CELL_SIZE = 2 ;
    final static int NITER = 100000 ;
    final static int OUTPUT_FREQ = 1 ;

    static float [][] phi = new float [N][N] ;
    static float [][] newPhi = new float [N][N] ;

    static Display display = new Display() ;

    public static void main(String args []) throws Exception {

        // Make voltage non-zero on left and right edges

        for(int j = 0 ; j < N ; j++) {
            phi [0] [j] = 1.0F ;
            phi [N-1] [j] = 1.0F ;
        }

        display.repaint() ;

        // Main update loop.

        long startTime = System.currentTimeMillis();

        for(int iter = 0 ; iter < NITER ; iter++) {

            // Calculate new phi

            for(int i = 1 ; i < N - 1 ; i++) {
                for(int j = 1 ; j < N - 1 ; j++) {

                    newPhi [i] [j] =
                            0.25F * (phi [i] [j - 1] + phi [i] [j + 1] +
                                    phi [i - 1] [j] + phi [i + 1] [j]) ;
                }
            }

            // Update all phi values

            for(int i = 1 ; i < N - 1 ; i++) {
                for(int j = 1 ; j < N - 1 ; j++) {
                    phi [i] [j] = newPhi [i] [j] ;
                }
            }

            if(iter % OUTPUT_FREQ == 0) {
                //System.out.println("iter = " + iter) ;
                display.repaint() ;
            }
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in " + (endTime - startTime) + " milliseconds");

        display.repaint() ;
    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE ;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE)) ;

            JFrame frame = new JFrame("Laplace");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            for(int i = 0 ; i < N ; i++) {
                for(int j = 0 ; j < N ; j++) {
                    float f = phi [i] [j] ;
                    Color c = new Color(f, 0.0F, 1.0F - f) ;
                    g.setColor(c) ;
                    g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                            CELL_SIZE, CELL_SIZE) ;
                }
            }
        }
    }
}

