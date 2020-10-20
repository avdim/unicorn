#!/usr/bin/env bash

rm -rf uni-idea-plugin/build/distributions
./gradlew uni-idea-plugin:clean uni-idea-plugin:buildPlugin

./gradlew update-plugin:runIde -PintegrationTest=true
#./gradlew update-plugin:runIde
