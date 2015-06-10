#!/bin/bash
sox -b8 -c1 -n $1 synth pinknoise fade t 0:00.5 0:07 0:05 flanger 0 2 0 60 0.12
