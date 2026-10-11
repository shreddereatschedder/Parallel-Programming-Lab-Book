 #include <omp.h>
  #include <stdio.h>
  #include <stdlib.h>

  #define N 32
  #define NITER 1024

  int state [N] [N] ;
  int sums  [N] [N] ;

  //void printCells() ;

  int main(int argc, char *args []) {

      // Define initial state of Life board

      for(int i = 0 ; i < N ; i++) {
          for(int j = 0 ; j < N ; j++) {
              state [i] [j] = random() > (RAND_MAX/2) ? 1 : 0 ;
          }
      }

      // Main update loop.

      for(int iter = 0 ; iter < NITER ; iter++) {

          //printf("iter = %d\n", iter++) ;

          // Calculate neighbour sums.

          #pragma omp parallel for
          for(int i = 0 ; i < N ; i++) {
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

          // Update state of board values.

          #pragma omp parallel for
          for(int i = 0 ; i < N ; i++) {
              for(int j = 0 ; j < N ; j++) {
                  switch (sums [i] [j]) {
                      case 2 : break;
                      case 3 : state [i] [j] = 1; break;
                      default: state [i] [j] = 0; break;
                  }
              }
          }

          //printCells() ;
      }
  }

  void printCells() {
      for(int i = 0 ; i < N ; i++) {
          for(int j = 0 ; j < N ; j++) {
              if(state [i] [j])
                  printf(" o") ;
              else
                  printf("  ") ;
          }
          printf("\n") ;
      }
  }
