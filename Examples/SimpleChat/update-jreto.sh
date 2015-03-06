#!/bin/bash

jRetoDir=../../Source

(cd $jRetoDir; mvn -Dmaven.test.skip=true clean install)
cp $jRetoDir/target/de.tum.in.www1.jReto-1.0-jar-with-dependencies.jar lib