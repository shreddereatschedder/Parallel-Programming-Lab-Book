package Lab4_InteractingThreads;

import java.awt.* ;
import javax.swing.* ;

import java.util.concurrent.CyclicBarrier ;

// CLASS COPIED AND PASTED FROM https://moodle.port.ac.uk/pluginfile.php/1163393/mod_resource/content/4/week4-lab.html
public class ParallelLife extends Thread {

    final static int N = 256 ;
    final static int P = 256 ;  // Number of threads
    final static int CELL_SIZE = 4 ;
    final static int DELAY = 0 ;

    static int [][] state = new int [N][N] ;
    static int [][] sums  = new int [N][N] ;

    static Display display = new Display() ;

    static CyclicBarrier barrier = new CyclicBarrier(P) ;

    public static void main(String args []) throws Exception {

        // Define initial state of Life board

        for(int i = 0 ; i < N ; i++) {
            for(int j = 0 ; j < N ; j++) {
                state [i] [j] = Math.random() > 0.5 ? 1 : 0 ;
            }
        }

        display.repaint() ;
        pause() ;

        ParallelLife [] threads = new ParallelLife [P] ;
        for(int me = 0 ; me < P ; me++) {
            threads [me] = new ParallelLife(me) ;
            threads [me].start() ;
        }

        for(int me = 0 ; me < P ; me++) {
            threads [me].join() ;
        }
    }

    int me ;

    ParallelLife(int me) {
        this.me = me ;
    }

    final static int B = N / P ;  // block size

    public void run() {

        int begin = me * B ;
        int end = begin + B ;

        // Main update loop.

        int iter = 0 ;
        while(true) {

            if(me == 0)
                System.out.println("iter = " + iter++) ;

            // Calculate neighbour sums.

            for(int i = begin ; i < end ; i++) {
                for(int j = 0 ; j < N ; j++) {

                    // find neighbours...
                    int ip = (i + 1) % N ;
                    int im = (i - 1 + N) % N ;
                    int jp = (j + 1) % N ;
                    int jm = (j - 1 + N) % N ;

                    sums [i] [j] =
                            state [im] [jm] + state [im] [ j] + state [im] [jp] +
                                    state [ i] [jm]                   + state [ i] [jp] +
                                    state [ip] [jm] + state [ip] [ j] + state [ip] [jp] ;
                }
            }

            synch() ;

            // Update state of board values.

            for(int i = begin ; i < end ; i++) {
                for(int j = 0 ; j < N ; j++) {
                    switch (sums [i] [j]) {
                        case 2 : break;
                        case 3 : state [i] [j] = 1; break;
                        default: state [i] [j] = 0; break;
                    }
                }
            }

            synch() ;

            if(me == 0)
                display.repaint() ;

            pause() ;
        }
    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE ;

        Display() {

            JFrame frame = new JFrame("Life");
            frame.setSize(WINDOW_SIZE, WINDOW_SIZE);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            g.setColor(Color.WHITE) ;
            for(int i = 0 ; i < N ; i++) {
                for(int j = 0 ; j < N ; j++) {
                    if(state [i] [j] == 1) {
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                CELL_SIZE, CELL_SIZE) ;
                    }
                }
            }
        }
    }

    static void synch() {
        try {
            barrier.await() ;
        }
        catch(Exception e) {
            e.printStackTrace() ;
            System.exit(1) ;
        }
    }

    static void pause() {
        try {
            Thread.sleep(DELAY) ;
        }
        catch(InterruptedException e) {
            e.printStackTrace() ;
            System.exit(1) ;
        }
    }
}
