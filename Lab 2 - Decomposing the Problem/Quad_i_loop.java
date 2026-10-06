    import java.awt.Color ;
    import java.awt.image.BufferedImage ;
    
    import javax.imageio.ImageIO;
    
    import java.io.File ;
    
    public class Quad_i_loop extends Thread {
    
        final static int N = 4096 ;
        final static int CUTOFF = 100 ; 
    
        static int [] [] set = new int [N] [N] ;
    
        public static void main(String [] args) throws Exception {
    
            // Calculate set
    
            long startTime = System.currentTimeMillis();

            int numThreads = 4;
            Quad_i_loop [] threads = new Quad_i_loop [numThreads] ;

            for (int t = 0; t < numThreads; t++) {
                threads [t] = new Quad_i_loop(t, numThreads);
                threads [t].start();
            }

            for (int t = 0; t < numThreads; t++) {
                threads [t].join();
            }
            
    
            long endTime = System.currentTimeMillis();
    
            System.out.println("Calculation completed in " +
                                (endTime - startTime) + " milliseconds");
    
            // Plot image
    
            BufferedImage img = new BufferedImage(N, N,
                                                    BufferedImage.TYPE_INT_ARGB) ;
    
            // Draw pixels
    
            for (int i = 0 ; i < N ; i++) {
                for (int j = 0 ; j < N ; j++) {
    
                    int k = set [i] [j] ;
    
                    float level ;
                    if(k < CUTOFF) {
                        level = (float) k / CUTOFF ;
                    }
                    else {
                        level = 0 ;
                    }
                    Color c = new Color(0, level, 0) ;  // Green
                    img.setRGB(i, j, c.getRGB()) ;
                }
            }
        
    
            // Print file
    
            ImageIO.write(img, "PNG", new File("Mandelbrot.png"));
        }
    
        int me ;
        int numThreads ;
    
        public Quad_i_loop(int me, int numThreads) {
            this.me = me ;
            this.numThreads = numThreads ;
        }
    
        public void run() {
                    int begin = me * N / numThreads ;
                    int end = (me + 1) * N / numThreads ;
            
            for(int i = begin ; i < end ; i++) {
                for(int j = 0 ; j <N ; j++) {

                    double cr = (4.0 * i - 2 * N) / N ;
                    double ci = (4.0 * j - 2 * N) / N ;

                    double zr = cr, zi = ci ;

                    int k = 0 ;
                    while (k < CUTOFF && zr * zr + zi * zi < 4.0) {

                        // z = c + z * z

                        double newr = cr + zr * zr - zi * zi ;
                        double newi = ci + 2 * zr * zi ;

                        zr = newr ;
                        zi = newi ;

                        k++ ;
                    }

                    set [i] [j] = k ;
                }
            }
            
        }
    
    }