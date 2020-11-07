#!/usr/bin/env bash

repeat_wrapper () {
#  ("$@" && "$@" && "$@") ||  exit 1
  ("$@") ||  exit 1
}

#export UNI_BUILD_TYPE="integration-test"

#rm -rf uni-idea-plugin/build/distributions
#./gradlew uni-idea-plugin:clean uni-idea-plugin:buildPlugin

repeat_wrapper ./gradlew update-plugin:runIde -PuniBuildType=integration-test
#./gradlew update-plugin:runIde
