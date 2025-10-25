#!/usr/bin/bash

lizard -l java -l javascript -C 7 -T nloc=27 -s cyclomatic_complexity -a 4 -x "./src/test/*" --csv > lizard.csv

