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

using namespace std;

MonteCarlo::MonteCarlo() : MAX_RANDOM(1.0 / (1.0 + RAND_MAX)) {
}

MonteCarlo::~MonteCarlo() {
    // TODO Auto-generated destructor stub
}

void MonteCarlo::calcInitialDr() {
    double drMin = calcMinOfMinDistance();
    dr = DR_INITIAL_RATIO * drMin;
}

// to proszę zrównoleglić
double MonteCarlo::calcMinOfMinDistance() {
    double drMinSQ = 100000.0;
    double tmp;
    int i;
    int numOfParticles = particles->getNumberOfParticles();
#pragma omp parallel for shared(drMinSQ, numOfParticles) private(i, tmp) 
    for (i = 0; i < numOfParticles; i++) {
        tmp = particles->getDistanceSQToClosest(i);
#pragma omp critical 
        {
            if (tmp < drMinSQ) {
                drMinSQ = tmp;
            }
        }
    }
    return sqrt(drMinSQ);
}

void MonteCarlo::setParticles(Particles* particles) {
    this->particles = particles;
    calcInitialDr();
}

void MonteCarlo::setPotential(PotentialEnergy* energy) {
    this->energy = energy;
}

// to proszę zrównoleglić
double MonteCarlo::calcContribution(int idx, double xx, double yy) {
    double sum = 0;
    int numOfParticles = particles->getNumberOfParticles();
#pragma omp parallel 
    {
#pragma omp for nowait reduction(+ : sum) 
        for (int i = 0; i < idx; i++) {
            sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
        }
#pragma omp for nowait reduction(+ : sum) 
        for (int i = idx + 1; i < numOfParticles; i++) {
            sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));

        }
    }

    return sum;
}

// to proszę zrównoleglić
double MonteCarlo::calcTotalPotentialEnergy() {
    double tmp = 0;
    int numOfParticles = particles->getNumberOfParticles();
#pragma omp parallel for reduction(+ : tmp) 
    for (int i = 0; i < numOfParticles; i++)
        tmp += calcContribution(i, particles->getX(i), particles->getY(i));

    totalEp = tmp * 0.5;

    return totalEp;
}

double MonteCarlo::deltaEp(int idx, double oldX, double oldY, double newX, double newY) {
    return calcContribution(idx, newX, newY) - calcContribution(idx, oldX, oldY);
}

// UWAGA: tej funkcji NIE WOLNO zrównoleglać.
void MonteCarlo::calcMC(int draws) {
    int accepted = 0;
    int idx;
    double xnew, ynew, xold, yold, dE, prob;

    for (int i = 0; i < draws; i++) {
        // którą z cząstek będzemy próbowali przestawić
        idx = (int)(particles->getNumberOfParticles() * rnd());
        // stara pozycja dla czastki
        xold = particles->getX(idx);
        yold = particles->getY(idx);
        // nowa pozycja dla czastki
        xnew = xold + dr * (rnd() - 0.5);
        ynew = yold + dr * (rnd() - 0.5);

        // wyliczamy zmianę energii potencjalnej gdy cząstka idx
        // przestawiana jest z pozycji old na new
        dE = deltaEp(idx, xold, yold, xnew, ynew);
        // pradopodobieństwo zależy od temperatury
        prob = exp(dE * kBTinv);
        // czy zaakceptowano zmianę położenia ?
        if (rnd() < prob) {
            // tak zaakceptowano -> zmiana położenia i energii
            particles->setXY(idx, xnew, ynew);
            totalEp += dE;
            accepted++;
        }
    }

    // zmiana dr jeśli zmian było ponado 50%, to
    // dr rośnie, jeśli było mniej, to dr maleje.
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

// to proszę zrównoleglić
long* MonteCarlo::getHistogram(int size) {
    long* result = new long[size];
    for (int i = 0; i < size; i++) {
        result[i] = 0;
    }

    double scale = size / particles->getBoxSize();
#pragma omp parallel 
    {
        long* result_cpy = new long[size];
        for (int i = 0; i < size; i++) {
            result_cpy[i] = 0;
        }
        int i, j;
#pragma omp for nowait schedule(dynamic)
        for (i = 0; i < particles->getNumberOfParticles(); i++) {
            for (j = 0; j < i; j++) {
                result_cpy[(int)(particles->getDistanceBetween(i, j) * scale)] ++;
            }
        }
#pragma omp critical 
        {
            for (i = 0; i < size; i++) result[i] += result_cpy[i];
        }
    }

    return result;
}
