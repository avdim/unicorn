#!/usr/bin/env bash

function open_or_skip {
  open uni-idea-plugin/build/distributions/* || nautilus uni-idea-plugin/build/distributions/* || (thunar uni-idea-plugin/build/distributions & > /dev/null)
}

./gradlew uni-idea-plugin:clean uni-idea-plugin:buildPlugin && open_or_skip
