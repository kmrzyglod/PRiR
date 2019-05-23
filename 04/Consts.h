/*
 * Consts.h
 *
 *  Created on: 13 cze 2018
 *      Author: oramus
 */

#ifndef CONSTS_H_
#define CONSTS_H_

const double DR_CORRECTION = 0.2;
const double DR_MAX = 0.3;
const double DR_MIN = 0.1;
const double DR_INITIAL_RATIO = 0.5 * ( DR_MAX + DR_MIN );

const double BOX_SIZE = 250.0;
const double DISTANCE = 0.8;

const int PARTICLES = 5000;
const double PARTICLES_PER_TEMP_MULTI = 0.33;
const int TEMPERATURES = 2;

const int HISTOGRAM_SIZE = 100;

// włączamy periodyczne warunki brzegowe
#define PBC

#endif /* CONSTS_H_ */
