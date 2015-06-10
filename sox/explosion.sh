#!/bin/bash
sox -b8 -c1 -n $1 synth whitenoise fade h 0 4 4 flanger 0 2 0 30 0.1 synth 4 exp fmod 50-400
