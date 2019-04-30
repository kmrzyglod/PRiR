/*
 * MonteCarlo.h
 *
 *  Created on: 13 cze 2018
 *      Author: oramus
 */

#ifndef MONTECARLO_H_
#define MONTECARLO_H_

#include"Particles.h"
#include "PotentialEnergy.h"

class MonteCarlo {
private:
	double dx, dy, dr;
	double kBTinv;
	double MAX_RANDOM;
	Particles *particles;
	PotentialEnergy *energy;
	double totalEp;
	void calcInitialDr();
	double calcContribution( int idx, double xx, double yy );
	double deltaEp( int idx, double oldX, double oldY, double newX, double newY );
	double rnd() {
		return random() * MAX_RANDOM;
	}
public:
	MonteCarlo();
	virtual ~MonteCarlo();
	void setParticles( Particles *particles );
	void setPotential( PotentialEnergy *energy );
	void calcMC( int draws );
	double calcMinOfMinDistance();
	double calcTotalPotentialEnergy();

	double getTotalPotentialEnergy() {
		return totalEp;
	}
	void setKBTinv( double kBTinv ) {
		this->kBTinv = -kBTinv;
	}

// tak - wiem, tej metody nie powinno być w tej klasie, ale
// dzięki takiemu układowi kodu całość kodu do zrównoleglenia
// zamknięta jest w MonteCarlo.
	long *getHistogram( int size );
};

#endif /* MONTECARLO_H_ */
