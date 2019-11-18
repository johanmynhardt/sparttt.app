#!/usr/bin/env bash

NEW_VERSION=$(git log --format=oneline | head -1 | cut -c 1-7)

echo "New Version: $NEW_VERSION"

echo "dir: `pwd`"

SW_FILE=./resources/public/service-worker.js

sed -i ${SW_FILE} -e "s#const PRECACHE \=*.*#const PRECACHE = 'precache-v-${NEW_VERSION}';#g"

echo "Updated PRECACHE in ${SW_FILE} to precache-v-${NEW_VERSION}"
