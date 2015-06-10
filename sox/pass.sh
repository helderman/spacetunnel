#!/bin/bash
sox -b8 -c1 -n $1 synth square 60 vol 0.5 fade t 0.2 0.4 0.2 flanger 0 2 0 60 2.5
