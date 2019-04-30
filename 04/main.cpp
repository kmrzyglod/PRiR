#include<iostream>
#include<sys/time.h>
#include<stdio.h>

#include"LennardJonesPotential.h"
#include"MonteCarlo.h"
#include"Particles.h"

using namespace std;


double getTime() {
  struct timeval tf;
  gettimeofday( &tf, NULL );

  return tf.tv_sec + tf.tv_usec * 0.000001;
}

double showTime( double last ) {
  double now = getTime();

  printf( "--- Time report: %09.6lfsec.\n", now - last ); 

  return now;
} 

void calc( Particles *particles ) {
	LennardJonesPotential *p = new LennardJonesPotential();
	MonteCarlo *mc = new MonteCarlo();
	mc->setParticles(particles);
	mc->setPotential(p);
	double kBT = 0.9;

	cout << "START" << endl;

    double tStart = getTime();
	double tStart0 = tStart;
    double Etot = mc->calcTotalPotentialEnergy();
	tStart = showTime( tStart );

    cout << "Etot          = " << Etot << endl;
    	
	for (int i = 0; i < TEMPERATURES; i++) {
		mc->setKBTinv(kBT); // ustalenie parametrów temperatury
		mc->calcMC ((int)( PARTICLES * PARTICLES_PER_TEMP_MULTI ) );
		kBT += 0.1;
	}

    tStart = showTime( tStart );
// koniec pomiaru czasu 

    double Eget = mc->getTotalPotentialEnergy();

    tStart = getTime();
	Etot = mc->calcTotalPotentialEnergy();
    tStart = showTime( tStart );

	double avrMinDist = mc->calcMinOfMinDistance();
    tStart = showTime( tStart );

	long *histogram = mc->getHistogram( HISTOGRAM_SIZE );
	for ( int i = 0 ; i < HISTOGRAM_SIZE; i++ ) {
		if ( histogram[ i  ])
			cout << i << " " << histogram[ i ] << endl;
	}
    tStart = showTime( tStart );

	cout << "Etot get   = " << Eget << endl;
	cout << "Etot calc  = " << Etot << endl;
	cout << "Najmniejsza odleglosc do najblizszego sasiada: " << avrMinDist << endl;
	cout << "Total time = " << (tStart - tStart0) << endl;
}

int main(int argc, char **argv) {
	srandom( time( NULL ) );

	Particles *pa = new Particles(PARTICLES);
	Particles *paBackup;
	pa->setBoxSize( BOX_SIZE );

	pa->initializePositions(BOX_SIZE, DISTANCE);
	paBackup = new Particles( pa ); // tu tworzona jest kopia położeń cząstek

	calc( pa );
}
