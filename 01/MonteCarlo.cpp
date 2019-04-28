/*
 * MonteCarlo.cpp
 *
 *  Created on: 13 cze 2018
 *      Author: oramus
 */

#include "MonteCarlo.h"
#include "Consts.h"
#include <math.h>
#include <iostream>
#include <stdlib.h>
#include <omp.h>
#include "MyMPI.h"


using namespace std;

MonteCarlo::MonteCarlo() : MAX_RANDOM( 1.0 / ( 1.0 + RAND_MAX )) {

}

MonteCarlo::~MonteCarlo() {
	// TODO Auto-generated destructor stub
}

void MonteCarlo::calcInitialDr() {
	double drMin = calcAvrMinDistance();
	dr = DR_INITIAL_RATIO * drMin;
}

double MonteCarlo::calcAvrMinDistance() {
	double drMinSQ = 100000.0;
	double tmp;
	for ( int i = 0; i < particles->getNumberOfParticles(); i++ ) {
		tmp = particles->getDistanceSQToClosest(i);
		if ( tmp < drMinSQ )
			drMinSQ = tmp;
	}
	return sqrt( drMinSQ );
}

void MonteCarlo::setParticles( Particles *particles ) {
	this->particles = particles;
	calcInitialDr();
}

void MonteCarlo::setPotential( PotentialEnergy *energy ) {
	this->energy = energy;
}

double MonteCarlo::calcContribution( int idx, double xx, double yy ) {
	double sum = 0;
	for ( int i = 0; i < idx; i++ ) {
		sum += energy->getPotentialEnergyDistanceSQ( particles->getDistanceBetweenSQ( i, xx, yy ));
	}
	for ( int i = idx+1; i < particles->getNumberOfParticles(); i++ ) {
		sum += energy->getPotentialEnergyDistanceSQ( particles->getDistanceBetweenSQ( i, xx, yy ));
	}
	return sum;
}

double MonteCarlo::calcContributionParallel(int idx, double xx, double yy) {
	//cout << "Process" << myRank << " All processes" << processes;

	double sum = 0;
	int pointsToProcess = particles->getNumberOfParticles() / processes;
	int additionalPointsToProcess = 0;
	if (myRank == processes - 1) {
		additionalPointsToProcess = particles->getNumberOfParticles() % pointsToProcess;
	}

	for (int i = myRank * pointsToProcess; i < (myRank + 1) * pointsToProcess + additionalPointsToProcess; i++) {
		if (i == idx) {
			continue;
		}

		sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
	}

	return sum;
}

double MonteCarlo::calcTotalPotentialEnergy() {
	double tmp = 0;
	for ( int i = 0; i < particles->getNumberOfParticles(); i++ )
		tmp += calcContribution( i, particles->getX( i ), particles->getY( i ) );

	totalEp = tmp * 0.5;

	return totalEp;
}

double MonteCarlo::deltaEp(int idx, double oldX, double oldY, double newX, double newY ) {
	double partialResult = calcContributionParallel(idx, newX, newY) - calcContributionParallel(idx, oldX, oldY);
	double globalResult;
	
	myMPI->MPI_Reduce(&partialResult, &globalResult, 1, MPI_DOUBLE, MPI_SUM, 0, MPI_COMM_WORLD);
	
	return myRank == 0 ? globalResult : partialResult;
}

// rozesłanie położeń cząstek z procesu o rank=0 do pozostałych
void MonteCarlo::shareParticles() {
	int num_of_particles = particles->getNumberOfParticles();
	
	double* x = new double[num_of_particles];
	double* y = new double[num_of_particles];
	
	//copy positions 
	if (myRank == 0) {
		for (int i = 0; i < num_of_particles; i++) {
			x[i] = particles->getX(i);
			y[i] = particles->getY(i);
		}
	}

	myMPI->MPI_Bcast(x, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
	myMPI->MPI_Bcast(y, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);

	if (myRank != 0) {
		//set broadcasted positions in proceses 
		for (int i = 0; i < num_of_particles; i++) {
			particles->setXY(i, x[i], y[i]);
		}
	}
}

// proces o rank=0 po zakończeniu tej metody musi zawierać
// zaktualizowane pozycje cząstek
void MonteCarlo::gatherParticles() {

}

void MonteCarlo::calcMC( int draws ) {
	int accepted = 0;
	int idx;
	double xnew, ynew, xold, yold, dE, prob;
	//cout << "\nCalculating dE on process " << myRank << "particles num " << particles->getNumberOfParticles();

	for ( int i = 0; i < draws; i++ ) {
		if (myRank == 0) {
			// którą z cząstek będzemy próbowali przestawić
			idx = (int)(particles->getNumberOfParticles() * rnd());
			// stara pozycja dla czastki
			xold = particles->getX(idx);
			yold = particles->getY(idx);
			// nowa pozycja dla czastki
			xnew = xold + dr * (rnd() - 0.5);
			ynew = yold + dr * (rnd() - 0.5);
		}

// wyliczamy zmianę energii potencjalnej gdy cząstka idx
// przestawiana jest z pozycji old na new
		myMPI->MPI_Bcast(&idx, 1, MPI_INT, 0, MPI_COMM_WORLD);
		myMPI->MPI_Bcast(&xnew, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		myMPI->MPI_Bcast(&ynew, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		xold = particles->getX(idx);
		yold = particles->getY(idx);

		dE = deltaEp( idx, xold, yold, xnew, ynew );
// pradopodobieństwo zależy od temperatury
		int setNewPosition;
		
		if (myRank == 0) {
			prob = exp(dE * kBTinv);
			if (rnd() < prob) {
				setNewPosition = 1;
			}
			else {
				setNewPosition = 0;
			}
		}

		myMPI->MPI_Bcast(&setNewPosition, 1, MPI_INT, 0, MPI_COMM_WORLD);
		
		if (setNewPosition == 1) {
			//cout << "\nSet idx = " << idx << "new x " << xnew << "new y" <<  ynew << "particles num: " << particles->getNumberOfParticles();
			particles->setXY(idx, xnew, ynew);
			//cout << "\nTotal EP = " << totalEp + dE;
			totalEp += dE;
			accepted++;
		}
	}

// zmiana dr jeśli zmian było ponado 50%, to
// dr rośnie, jeśli było mniej, to dr maleje.
	if (myRank == 0) {
		if (accepted * 2 > draws) {
			dr *= (1.0 + DR_CORRECTION);
		}
		else {
			dr *= (1.0 - DR_CORRECTION);
		}
		if (dr > DR_MAX)
			dr = DR_MAX;

		if (dr < DR_MIN)
			dr = DR_MIN;
	}
}

